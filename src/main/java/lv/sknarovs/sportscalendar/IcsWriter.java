package lv.sknarovs.sportscalendar;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Renders events as iCalendar (RFC 5545) text. */
final class IcsWriter {
    static final String TZID = "Europe/Riga";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAX_LINE_OCTETS = 75;
    private static final Comparator<Event> ORDER = Comparator
            .comparing((Event e) -> e.fixture().date())
            .thenComparing(e -> e.fixture().time() == null ? java.time.LocalTime.MIN : e.fixture().time())
            .thenComparing(e -> e.fixture().id());

    /** Europe/Riga under EU daylight-saving rules; IcsWriterTest checks it against java.time. */
    private static final List<String> VTIMEZONE = List.of(
            "BEGIN:VTIMEZONE",
            "TZID:" + TZID,
            "BEGIN:DAYLIGHT",
            "TZOFFSETFROM:+0200",
            "TZOFFSETTO:+0300",
            "TZNAME:EEST",
            "DTSTART:19700329T030000",
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU",
            "END:DAYLIGHT",
            "BEGIN:STANDARD",
            "TZOFFSETFROM:+0300",
            "TZOFFSETTO:+0200",
            "TZNAME:EET",
            "DTSTART:19701025T040000",
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU",
            "END:STANDARD",
            "END:VTIMEZONE");

    private IcsWriter() {
    }

    static String write(List<Event> events, String calendarName) {
        List<String> lines = new ArrayList<>();
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:" + escape("-//SportsCalendar//Latvia national teams//LV"));
        lines.add("CALSCALE:GREGORIAN");
        lines.add("X-WR-CALNAME:" + escape(calendarName));
        lines.add("X-WR-TIMEZONE:" + TZID);
        lines.add("REFRESH-INTERVAL;VALUE=DURATION:PT12H");
        lines.add("X-PUBLISHED-TTL:PT12H");
        lines.addAll(VTIMEZONE);
        events.stream().sorted(ORDER).forEach(event -> addEvent(lines, event));
        lines.add("END:VCALENDAR");

        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(fold(line)).append("\r\n");
        }
        return out.toString();
    }

    private static void addEvent(List<String> lines, Event event) {
        Fixture fixture = event.fixture();
        lines.add("BEGIN:VEVENT");
        lines.add("UID:" + escape("lff-" + fixture.id() + "@sportscalendar"));
        lines.add("DTSTAMP:" + UTC.format(event.stamp()));
        if (fixture.time() != null) {
            LocalDateTime start = fixture.kickoff();
            lines.add("DTSTART;TZID=" + TZID + ":" + DATE_TIME.format(start));
            lines.add("DTEND;TZID=" + TZID + ":" + DATE_TIME.format(start.plus(fixture.category().duration())));
        } else {
            lines.add("DTSTART;VALUE=DATE:" + DATE.format(fixture.date()));
            lines.add("DTEND;VALUE=DATE:" + DATE.format(fixture.date().plusDays(1)));
        }
        lines.add("SUMMARY:" + escape(summary(event)));
        lines.add("DESCRIPTION:" + escape(description(event)));
        if (!event.venue().isEmpty()) {
            lines.add("LOCATION:" + escape(event.venue()));
        }
        if (fixture.url() != null) {
            lines.add("URL:" + fixture.url());
        }
        lines.add("CATEGORIES:" + escape(fixture.category().label()));
        lines.add("STATUS:CONFIRMED");
        lines.add("END:VEVENT");
    }

    /** {@code Latvija 2-1 Armēnija (competition)} or {@code Latvija vs Armēnija (competition)}. */
    static String summary(Event event) {
        Fixture fixture = event.fixture();
        String title;
        if (fixture.finished()) {
            title = fixture.homeTeam() + " " + fixture.homeScore() + "-" + fixture.awayScore() + " "
                    + fixture.awayTeam();
            if (fixture.homePen() != null && fixture.awayPen() != null) {
                title += " (" + fixture.homePen() + "-" + fixture.awayPen() + " pēcsp.)";
            }
        } else {
            title = fixture.homeTeam() + " vs " + fixture.awayTeam();
        }
        return fixture.competition().isEmpty() ? title : title + " (" + fixture.competition() + ")";
    }

    static String description(Event event) {
        Fixture fixture = event.fixture();
        List<String> parts = new ArrayList<>();
        parts.add(fixture.finished()
                ? fixture.homeTeam() + " " + fixture.homeScore() + "-" + fixture.awayScore() + " "
                        + fixture.awayTeam()
                : fixture.homeTeam() + " vs " + fixture.awayTeam());
        if (fixture.homePen() != null && fixture.awayPen() != null) {
            parts.add("Pēcspēles sitieni: " + fixture.homePen() + "-" + fixture.awayPen());
        }
        String home = goalLine(event, Goal.Side.HOME, fixture.homeTeam());
        String away = goalLine(event, Goal.Side.AWAY, fixture.awayTeam());
        if (!home.isEmpty() || !away.isEmpty()) {
            parts.add("");
            parts.add("Vārti:");
            if (!home.isEmpty()) {
                parts.add(home);
            }
            if (!away.isEmpty()) {
                parts.add(away);
            }
        }
        if (!event.venue().isEmpty()) {
            parts.add("");
            parts.add("Stadions: " + event.venue());
        }
        if (fixture.url() != null) {
            parts.add("");
            parts.add("Spēle: " + fixture.url());
        }
        return String.join("\n", parts);
    }

    private static String goalLine(Event event, Goal.Side side, String team) {
        List<String> goals = event.goals().stream()
                .filter(goal -> goal.side() == side)
                .map(IcsWriter::goalText)
                .toList();
        return goals.isEmpty() ? "" : team + ": " + String.join(", ", goals);
    }

    private static String goalText(Goal goal) {
        String text = goal.scorer().isEmpty() ? goal.minute() : goal.minute() + " " + goal.scorer();
        return goal.ownGoal() ? text + " (savos vārtos)" : text;
    }

    /** Escapes an iCalendar TEXT value. */
    static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    /** Folds a content line so each physical line holds at most 75 octets, never splitting a character. */
    static String fold(String line) {
        StringBuilder out = new StringBuilder();
        int octets = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_LINE_OCTETS) {
                out.append("\r\n ");
                // The continuation space is part of the physical line as well.
                octets = 1;
            }
            out.appendCodePoint(codePoint);
            octets += size;
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }
}
