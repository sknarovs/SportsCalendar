package lv.sknarovs.sportscalendar;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code --test} smoke test. It ships inside the binary and runs offline, so {@code run.sh} can prove that
 * parsing, caching and folding work before installing a fresh release.
 */
final class SelfTest {
    private static final String TEAM_PAGE = """
            <div class="fixtures">
              <div class="tr match even" data-id="111">
                <div><div class="date"><h4>svētdiena</h4><h5>15</h5><h6>nov</h6>
                  <div class="h7">16:00</div><div class="h8">2026</div></div></div>
                <div><div class="competition">2027. gada UEFA Nāciju līga - C2 grupa, 1. kārta</div>
                  <div class="clubs">
                    <div class="club"><div class="title"><span><a href="/izlases/nacionala-izlase/">Latvija</a></span></div>
                      <div class="result"><span class="res1">-</span></div></div>
                    <div class="club"><div class="title"><span>Armēnija</span></div>
                      <div class="result"><span class="res2">-</span></div></div>
                  </div>
                  <div class="stadium">Skonto Stadions</div></div>
                <div><a class="info" href="/speles/latvija-armenija-111/">&nbsp;</a></div>
              </div>
              <div class="tr match" data-id="222">
                <div><div class="date"><h4>trešdiena</h4><h5>3</h5><h6>mar</h6>
                  <div class="h7"></div><div class="h8">2027</div></div></div>
                <div><div class="competition">Pārbaudes spēle</div>
                  <div class="clubs">
                    <div class="club"><div class="title"><span>Latvija</span></div>
                      <div class="result"><span class="res1">-</span></div></div>
                    <div class="club"><div class="title"><span><span class="sizeMedium">Čehija</span></span></div>
                      <div class="result"><span class="res2">-</span></div></div>
                  </div>
                  <div class="stadium">-</div></div>
                <div><span></span></div>
              </div>
              <div class="tr match" data-id="333">
                <div><div class="date"><h4>svētdiena</h4><h5>8</h5><h6>nov</h6>
                  <div class="h7">18:00</div><div class="h8">2026</div></div></div>
                <div><div class="competition">Pārbaudes spēle</div>
                  <div class="clubs">
                    <div class="club"><div class="title"><span>Azerbaidžāna</span></div>
                      <div class="result"><span class="res1">1</span></div></div>
                    <div class="club"><div class="title"><span>Latvija</span></div>
                      <div class="result"><span class="res2">5</span></div></div>
                  </div>
                  <div class="stadium">Baku Sports Hall</div></div>
                <div><a href="/speles/azerbaidzana-latvija-333/">&nbsp;</a></div>
              </div>
            </div>
            """;

    private static final String MATCH_PAGE = """
            <div class="matchData"><ul>
              <li class="date"><b>08 nov</b> 2026 <b>18:00</b></li>
              <li class="venue"><span class="stadium">Baku Sports Hall, Baku</span></li>
              <li class="competition"><span>Pārbaudes spēle</span></li>
            </ul></div>
            <div class="resultContainer"><div class="line1">
              <div class="club1"><span>Azerbaidžāna</span><div class="result"><div class="res">1</div></div></div>
              <div class="club2"><span>Latvija</span><div class="result"><div class="res">5</div></div></div>
            </div></div>
            <div class="matchprogress"><div class="matchprogress reverse">
              <div class="tr event odd club2event goal"><div class="minute"><h4 class="">1’</h4></div>
                <div class="goal"><h3 class="big">Vāāāārti! &nbsp;0:1</h3>
                  <span>Vārtus guva <b><a href="/speletaji/renards-udris-30965/">Renards Ūdris</a></b></span></div></div>
              <div class="tr event club1event goal"><div class="minute"><h4 class="withstoppage">90<span class="stoppage">+1’</span></h4></div>
                <div class="goal"><h3 class="big">Vāāāārti! &nbsp;1:5</h3>
                  <span>Vārtus guva <b>Nihad Ismayilov</b></span></div></div>
            </div></div>
            """;

    private SelfTest() {
    }

    static boolean run(PrintStream out) {
        out.println("[*] Running self-test...");
        List<String> failures = new ArrayList<>();

        List<Fixture> fixtures = TeamPageParser.parse(TEAM_PAGE, Category.FOOTBALL);
        check(failures, "fixture count", 3, fixtures.size());
        if (fixtures.isEmpty()) {
            out.println("[!] fixture parse produced no rows");
            return false;
        }
        if (fixtures.size() == 3) {
            Fixture upcoming = fixtures.get(0);
            check(failures, "upcoming id", "111", upcoming.id());
            check(failures, "upcoming date", LocalDate.of(2026, 11, 15), upcoming.date());
            check(failures, "upcoming time", LocalTime.of(16, 0), upcoming.time());
            check(failures, "upcoming home", "Latvija", upcoming.homeTeam());
            check(failures, "upcoming away", "Armēnija", upcoming.awayTeam());
            check(failures, "upcoming score", null, upcoming.homeScore());
            check(failures, "upcoming venue", "Skonto Stadions", upcoming.stadium());
            check(failures, "upcoming url", "https://lff.lv/speles/latvija-armenija-111/", upcoming.url());

            Fixture unknownTime = fixtures.get(1);
            check(failures, "unknown time", null, unknownTime.time());
            check(failures, "missing venue", "", unknownTime.stadium());
            check(failures, "missing url", null, unknownTime.url());
            check(failures, "nested team name", "Čehija", unknownTime.awayTeam());

            Fixture finished = fixtures.get(2);
            check(failures, "finished score", "1-5", finished.homeScore() + "-" + finished.awayScore());
            check(failures, "finished flag", true, finished.finished());
        }

        MatchDetails details = MatchPageParser.parse(MATCH_PAGE);
        check(failures, "match score", "1-5", details.homeScore() + "-" + details.awayScore());
        check(failures, "match venue", "Baku Sports Hall, Baku", details.venue());
        check(failures, "goal count", 2, details.goals().size());
        if (details.goals().size() == 2) {
            check(failures, "first goal", "1'|AWAY|Renards Ūdris", describe(details.goals().get(0)));
            check(failures, "stoppage goal", "90+1'|HOME|Nihad Ismayilov", describe(details.goals().get(1)));
        }

        Fixture finished = fixtures.get(fixtures.size() - 1);
        Event finishedEvent = new Event(finished, "Baku Sports Hall", details.goals(), Instant.parse("2026-11-08T19:00:00Z"));
        check(failures, "summary", "Azerbaidžāna 1-5 Latvija (Pārbaudes spēle)", IcsWriter.summary(finishedEvent));

        String ics = IcsWriter.write(List.of(finishedEvent), "Test calendar");
        String unfolded = ics.replace("\r\n ", "");
        check(failures, "uid present", true, ics.contains("UID:lff-333@sportscalendar\r\n"));
        check(failures, "start present", true, ics.contains("DTSTART;TZID=Europe/Riga:20261108T180000\r\n"));
        check(failures, "end present", true, ics.contains("DTEND;TZID=Europe/Riga:20261108T200000\r\n"));
        check(failures, "goal line present", true, unfolded.contains("Azerbaidžāna: 90+1' Nihad Ismayilov"));
        check(failures, "crlf ending", true, ics.endsWith("END:VCALENDAR\r\n"));

        String folded = IcsWriter.fold("A".repeat(200));
        String[] lines = folded.split("\r\n");
        check(failures, "folded line count", 3, lines.length);
        check(failures, "folded continuation", true, lines[lines.length - 1].startsWith(" "));
        check(failures, "folded width", true,
                List.of(lines).stream().allMatch(line -> line.getBytes(StandardCharsets.UTF_8).length <= 75));

        check(failures, "month jūn", LocalDate.of(2026, 6, 9), LffDates.date("9", "jūn", "2026"));
        check(failures, "month okt", LocalDate.of(2025, 10, 11), LffDates.date("11", "okt", "2025"));
        check(failures, "time", LocalTime.of(21, 45), LffDates.time("21:45"));

        checkCache(failures);

        if (failures.isEmpty()) {
            out.println("[+] Self-test passed.");
            return true;
        }
        failures.forEach(failure -> out.println("[!] " + failure));
        return false;
    }

    private static void checkCache(List<String> failures) {
        Map<String, Cache.Entry> entries = new LinkedHashMap<>();
        entries.put("333", new Cache.Entry(1_700_000_000_000L, "abc",
                1, 5, null, null, "Baku, Hall; A\tB",
                List.of(new Goal("1'", Goal.Side.AWAY, "Renards Ūdris", false),
                        new Goal("90+1'", Goal.Side.HOME, "Name | weird; chars", true))));
        Path file = null;
        try {
            file = Files.createTempFile("sports-calendar-cache", ".tsv");
            Cache.save(file, entries);
            Map<String, Cache.Entry> loaded = Cache.load(file);
            check(failures, "cache round-trip", entries, loaded);
        } catch (Exception e) {
            failures.add("cache round-trip failed: " + e);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (Exception ignored) {
                    // A leftover temp file is harmless.
                }
            }
        }
    }

    private static String describe(Goal goal) {
        return goal.minute() + "|" + goal.side() + "|" + goal.scorer();
    }

    private static void check(List<String> failures, String name, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
