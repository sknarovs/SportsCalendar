package lv.sknarovs.sportscalendar;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** One update run: scrape both team pages, refresh the details that are missing, write the calendar and cache. */
final class CalendarUpdater {
    static final ZoneId RIGA = ZoneId.of("Europe/Riga");
    /** A fixture list is 14 rows per page; the lookback window never needs more than a couple. */
    private static final int MAX_PAGES = 10;
    /** Pause between match-page fetches; the first run of the year is the only busy one. */
    private static final long REQUEST_DELAY_MS = 250;
    /** Matches this recent are re-read even when the score matches, to catch late corrections. */
    private static final int RECENT_DAYS = 2;
    static final String CALENDAR_NAME = "Latvija – futbols un telpu futbols";

    private final PageSource source;
    private final Clock clock;
    private final Path icsFile;
    private final Path cacheFile;
    private final int lookbackDays;
    private final PrintStream out;
    private final PrintStream err;

    CalendarUpdater(PageSource source, Clock clock, Path icsFile, Path cacheFile, int lookbackDays,
            PrintStream out, PrintStream err) {
        this.source = source;
        this.clock = clock;
        this.icsFile = icsFile;
        this.cacheFile = cacheFile;
        this.lookbackDays = lookbackDays;
        this.out = out;
        this.err = err;
    }

    /** Returns the process exit code: 0 when the calendar was written, 1 otherwise. */
    int run() throws IOException, InterruptedException {
        LocalDate today = LocalDate.ofInstant(clock.instant(), RIGA);
        LocalDate cutoff = today.minusDays(lookbackDays);
        Instant now = clock.instant();
        out.println("Lookback: " + lookbackDays + " days (matches since " + cutoff + ").");

        Map<String, Cache.Entry> cache = Cache.load(cacheFile);
        out.println("Cached matches: " + cache.size() + ".");

        List<Fixture> fixtures = scrape(cutoff);
        List<Fixture> window = fixtures.stream().filter(fixture -> !fixture.date().isBefore(cutoff)).toList();
        out.println("Matches in window: " + window.size() + ".");

        Map<String, MatchDetails> details = resolveDetails(window, cache, today);
        List<Event> events = buildEvents(window, details, cache, now);
        if (events.isEmpty()) {
            err.println("[!] No matches found; keeping the existing calendar and cache.");
            return 1;
        }

        Cache.writeAtomically(cacheFile, Cache.saveToString(buildCache(events)));
        Cache.writeAtomically(icsFile, IcsWriter.write(events, CALENDAR_NAME));

        long finished = events.stream().filter(event -> event.fixture().finished()).count();
        out.println("Wrote " + icsFile + ": " + events.size() + " events (" + finished + " with results).");
        out.println("Wrote " + cacheFile + ".");
        return 0;
    }

    private List<Fixture> scrape(LocalDate cutoff) throws IOException, InterruptedException {
        Map<String, Fixture> unique = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            for (int page = 1; page <= MAX_PAGES; page++) {
                String url = category.fixturesUrl(page);
                out.println("Fetching " + url + " ...");
                List<Fixture> rows = TeamPageParser.parse(source.fetch(url), category);
                if (rows.isEmpty()) {
                    break;
                }
                for (Fixture fixture : rows) {
                    unique.putIfAbsent(fixture.id(), fixture);
                }
                LocalDate oldest = rows.stream().map(Fixture::date).min(LocalDate::compareTo).orElse(cutoff);
                if (oldest.isBefore(cutoff)) {
                    break;
                }
            }
        }
        return List.copyOf(unique.values());
    }

    private Map<String, MatchDetails> resolveDetails(List<Fixture> window, Map<String, Cache.Entry> cache,
            LocalDate today) throws IOException, InterruptedException {
        Map<String, MatchDetails> details = new LinkedHashMap<>();
        for (Fixture fixture : window) {
            if (!fixture.finished()) {
                continue;
            }
            Cache.Entry cached = cache.get(fixture.id());
            boolean recent = !fixture.date().isBefore(today.minusDays(RECENT_DAYS));
            if (cached != null && !recent && sameScore(cached, fixture)) {
                details.put(fixture.id(), cached.toDetails());
                continue;
            }
            if (fixture.url() == null) {
                if (cached != null) {
                    details.put(fixture.id(), cached.toDetails());
                }
                continue;
            }
            try {
                Thread.sleep(REQUEST_DELAY_MS);
                out.println("Fetching " + fixture.url() + " ...");
                details.put(fixture.id(), MatchPageParser.parse(source.fetch(fixture.url())));
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                err.println("[!] Could not read " + fixture.url() + ": " + e.getMessage());
                if (cached != null) {
                    details.put(fixture.id(), cached.toDetails());
                }
            }
        }
        return details;
    }

    private List<Event> buildEvents(List<Fixture> window, Map<String, MatchDetails> details,
            Map<String, Cache.Entry> cache, Instant now) {
        List<Event> events = new ArrayList<>();
        for (Fixture fixture : window) {
            events.add(stamp(fixture, details.get(fixture.id()), cache.get(fixture.id()), now));
        }
        return events;
    }

    /** Keeps the previous {@code DTSTAMP} while the event content is unchanged. */
    private Event stamp(Fixture fixture, MatchDetails details, Cache.Entry cached, Instant now) {
        String venue = fixture.stadium().isEmpty() && details != null ? details.venue() : fixture.stadium();
        List<Goal> goals = details == null ? List.of() : details.goals();
        Event probe = new Event(fixture, venue, goals, now);
        String hash = probe.contentHash();
        Instant stamp = cached != null && cached.hash().equals(hash) ? Instant.ofEpochMilli(cached.stampMillis()) : now;
        return probe.withStamp(stamp);
    }

    private Map<String, Cache.Entry> buildCache(List<Event> events) {
        Map<String, Cache.Entry> entries = new TreeMap<>();
        for (Event event : events) {
            Fixture fixture = event.fixture();
            entries.put(fixture.id(), new Cache.Entry(event.stamp().toEpochMilli(), event.contentHash(),
                    fixture.homeScore(), fixture.awayScore(), fixture.homePen(), fixture.awayPen(),
                    event.venue(), event.goals()));
        }
        return entries;
    }

    private static boolean sameScore(Cache.Entry cached, Fixture fixture) {
        return java.util.Objects.equals(cached.homeScore(), fixture.homeScore())
                && java.util.Objects.equals(cached.awayScore(), fixture.awayScore());
    }
}
