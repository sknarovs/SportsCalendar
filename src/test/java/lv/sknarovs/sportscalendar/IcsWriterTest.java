package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcsWriterTest {
    private static final Instant STAMP = Instant.parse("2026-11-15T09:30:00Z");

    @Test
    void summarisesUpcomingMatch() {
        Event event = event(fixture(LocalTime.of(16, 0), null, null, "Nāciju līga"), "");

        assertEquals("Latvija vs Armēnija (Nāciju līga)", IcsWriter.summary(event));
    }

    @Test
    void summarisesFinishedMatch() {
        Event event = event(fixture(LocalTime.of(16, 0), 2, 1, "Nāciju līga"), "");

        assertEquals("Latvija 2-1 Armēnija (Nāciju līga)", IcsWriter.summary(event));
    }

    @Test
    void omitsEmptyCompetition() {
        Event event = event(fixture(LocalTime.of(16, 0), 2, 1, ""), "");

        assertEquals("Latvija 2-1 Armēnija", IcsWriter.summary(event));
    }

    @Test
    void showsPenaltyShootout() {
        Event event = event(penalties(fixture(LocalTime.of(16, 0), 2, 2, "PK")), "");

        assertEquals("Latvija 2-2 Armēnija (1-2 pēcsp.) (PK)", IcsWriter.summary(event));
    }

    @Test
    void descriptionListsGoalsByTeam() {
        Event event = new Event(fixture(LocalTime.of(16, 0), 2, 1, "Nāciju līga"), "Skonto Stadions",
                List.of(new Goal("12'", Goal.Side.HOME, "Renards Ūdris", false),
                        new Goal("44'", Goal.Side.HOME, "Miks Babris", false),
                        new Goal("90+1'", Goal.Side.AWAY, "Nihad Ismayilov", false)),
                STAMP);

        String description = IcsWriter.description(event);

        assertTrue(description.contains("Latvija 2-1 Armēnija"));
        assertTrue(description.contains("Vārti:"));
        assertTrue(description.contains("Latvija: 12' Renards Ūdris, 44' Miks Babris"));
        assertTrue(description.contains("Armēnija: 90+1' Nihad Ismayilov"));
        assertTrue(description.contains("Stadions: Skonto Stadions"));
        assertTrue(description.contains("Spēle: https://lff.lv/speles/test/"));
    }

    @Test
    void marksOwnGoals() {
        Event event = new Event(fixture(LocalTime.of(16, 0), 1, 0, ""), "",
                List.of(new Goal("5'", Goal.Side.HOME, "Jānis Bērziņš", true)), STAMP);

        assertTrue(IcsWriter.description(event).contains("5' Jānis Bērziņš (savos vārtos)"));
    }

    @Test
    void writesTimedAndAllDayEvents() {
        String ics = IcsWriter.write(List.of(
                event(fixture(LocalTime.of(16, 0), 2, 1, "Nāciju līga"), ""),
                new Event(new Fixture("222", Category.FUTSAL, LocalDate.of(2027, 3, 3), null, "PK kvalifikācija",
                        "Latvija", "Čehija", null, null, null, null, "", null), "", List.of(), STAMP)),
                "Test");

        assertTrue(ics.contains("DTSTART;TZID=Europe/Riga:20261115T160000\r\n"));
        assertTrue(ics.contains("DTEND;TZID=Europe/Riga:20261115T180000\r\n"));
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20270303\r\n"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20270304\r\n"));
        assertTrue(ics.contains("DTSTAMP:20261115T093000Z\r\n"));
        assertTrue(ics.contains("BEGIN:VTIMEZONE\r\n"));
    }

    @Test
    void usesFutsalDuration() {
        Fixture futsal = new Fixture("444", Category.FUTSAL, LocalDate.of(2026, 10, 20), LocalTime.of(19, 30), "",
                "Latvija", "Albānija", null, null, null, null, "", null);
        String ics = IcsWriter.write(List.of(new Event(futsal, "", List.of(), STAMP)), "Test");

        assertTrue(ics.contains("DTEND;TZID=Europe/Riga:20261020T211500\r\n"));
        assertTrue(ics.contains("CATEGORIES:Telpu futbols\r\n"));
    }

    @Test
    void sortsEventsChronologically() {
        Fixture later = new Fixture("2", Category.FOOTBALL, LocalDate.of(2027, 1, 1), LocalTime.of(16, 0), "Vēlāk",
                "Latvija", "X", null, null, null, null, "", null);
        Fixture earlier = new Fixture("1", Category.FOOTBALL, LocalDate.of(2026, 1, 1), LocalTime.of(16, 0), "Agrāk",
                "Latvija", "Y", null, null, null, null, "", null);

        String ics = IcsWriter.write(List.of(
                new Event(later, "", List.of(), STAMP),
                new Event(earlier, "", List.of(), STAMP)), "Test");

        assertTrue(ics.indexOf("SUMMARY:Latvija vs Y") < ics.indexOf("SUMMARY:Latvija vs X"));
    }

    @Test
    void escapesTextValues() {
        assertEquals("a\\,b\\;c\\\\d\\ne", IcsWriter.escape("a,b;c\\d\ne"));
    }

    @Test
    void foldsWithoutSplittingMultibyteCharacters() {
        String original = "SUMMARY:" + "Ā".repeat(120);
        String folded = IcsWriter.fold(original);
        String[] lines = folded.split("\r\n");

        for (String line : lines) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, () -> "too long: " + line);
        }
        assertTrue(lines.length > 1);
        assertTrue(folded.contains("\r\n "));
        assertEquals(original, folded.replace("\r\n ", ""));
    }

    @Test
    void usesCrlfLineEndings() {
        String ics = IcsWriter.write(List.of(), "Test");

        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertFalse(ics.contains("\n\n"));
        assertEquals(0, ics.replace("\r\n", "").chars().filter(c -> c == '\n').count());
    }

    @Test
    void timezoneMatchesJavaTime() {
        ZoneId riga = ZoneId.of(IcsWriter.TZID);

        assertEquals(3 * 3600, riga.getRules().getOffset(Instant.parse("2026-07-01T10:00:00Z")).getTotalSeconds());
        assertEquals(2 * 3600, riga.getRules().getOffset(Instant.parse("2026-01-01T10:00:00Z")).getTotalSeconds());
        String ics = IcsWriter.write(List.of(), "Test");
        assertTrue(ics.contains("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n"));
        assertTrue(ics.contains("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n"));
    }

    private static Event event(Fixture fixture, String venue) {
        return new Event(fixture, venue, List.of(), STAMP);
    }

    private static Fixture fixture(LocalTime time, Integer homeScore, Integer awayScore, String competition) {
        return new Fixture("333", Category.FOOTBALL, LocalDate.of(2026, 11, 15), time, competition,
                "Latvija", "Armēnija", homeScore, awayScore, null, null, "Skonto Stadions",
                "https://lff.lv/speles/test/");
    }

    private static Fixture penalties(Fixture fixture) {
        return new Fixture(fixture.id(), fixture.category(), fixture.date(), fixture.time(), fixture.competition(),
                fixture.homeTeam(), fixture.awayTeam(), fixture.homeScore(), fixture.awayScore(), 1, 2,
                fixture.stadium(), fixture.url());
    }
}
