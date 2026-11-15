package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamPageParserTest {
    @Test
    void parsesUpcomingFootballFixture() {
        List<Fixture> fixtures = TeamPageParser.parse(Pages.load("team-football.html"), Category.FOOTBALL);

        assertEquals(14, fixtures.size());
        Fixture first = fixtures.getFirst();
        assertEquals("23697290", first.id());
        assertEquals(Category.FOOTBALL, first.category());
        assertEquals(LocalDate.of(2026, 11, 15), first.date());
        assertEquals(LocalTime.of(16, 0), first.time());
        assertEquals("2027. gada UEFA Nāciju līga - C2 grupa, 1. kārta", first.competition());
        assertEquals("Latvija", first.homeTeam());
        assertEquals("Armēnija", first.awayTeam());
        assertNull(first.homeScore());
        assertNull(first.awayScore());
        assertEquals("Skonto Stadions", first.stadium());
        assertEquals("https://lff.lv/speles/latvija-armenija-23697290/", first.url());
        assertFalse(first.finished());
    }

    @Test
    void parsesFinishedFootballFixture() {
        Fixture fixture = byId(TeamPageParser.parse(Pages.load("team-football.html"), Category.FOOTBALL),
                "23697280");

        assertTrue(fixture.finished());
        assertEquals(2, fixture.homeScore());
        assertEquals(1, fixture.awayScore());
        assertEquals("Melnkalne", fixture.homeTeam());
        assertEquals("Latvija", fixture.awayTeam());
        assertEquals("Podgorica City Stadium", fixture.stadium());
        assertEquals("https://lff.lv/speles/melnkalne-latvija-23697280/", fixture.url());
    }

    @Test
    void keepsTheOldestFixtureOnThePage() {
        Fixture oldest = byId(TeamPageParser.parse(Pages.load("team-football.html"), Category.FOOTBALL),
                "20494756");

        assertEquals(LocalDate.of(2025, 10, 11), oldest.date());
        assertEquals("Latvija", oldest.homeTeam());
        assertEquals("Andora", oldest.awayTeam());
        assertEquals("Daugavas stadions", oldest.stadium());
    }

    @Test
    void parsesFutsalFixtureWithUnknownKickOff() {
        List<Fixture> fixtures = TeamPageParser.parse(Pages.load("team-futsal.html"), Category.FUTSAL);

        assertEquals(14, fixtures.size());
        Fixture first = fixtures.getFirst();
        assertEquals("25367890", first.id());
        assertEquals(Category.FUTSAL, first.category());
        assertEquals(LocalDate.of(2027, 3, 3), first.date());
        assertNull(first.time());
        assertEquals("Latvija", first.homeTeam());
        assertEquals("Čehija", first.awayTeam());
        assertEquals("", first.stadium());
        assertNull(first.url());
    }

    @Test
    void parsesFinishedFutsalFixture() {
        Fixture fixture = byId(TeamPageParser.parse(Pages.load("team-futsal.html"), Category.FUTSAL), "25367858");

        assertTrue(fixture.finished());
        assertEquals(1, fixture.homeScore());
        assertEquals(5, fixture.awayScore());
        assertEquals("Azerbaidžāna", fixture.homeTeam());
        assertEquals("Latvija", fixture.awayTeam());
        assertEquals("Baku Sports Hall", fixture.stadium());
        assertEquals("https://lff.lv/speles/azerbaidzana-latvija-25367858/", fixture.url());
    }

    @Test
    void buildsPaginatedFixtureUrls() {
        assertEquals("https://lff.lv/izlases/nacionala-izlase/?pg=2&tab=fixtures",
                Category.FOOTBALL.fixturesUrl(2));
        assertEquals("https://lff.lv/izlases/nacionala-izlase/telpu-futbola-izlase/?pg=1&tab=fixtures",
                Category.FUTSAL.fixturesUrl(1));
    }

    private static Fixture byId(List<Fixture> fixtures, String id) {
        return fixtures.stream()
                .filter(fixture -> fixture.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no fixture " + id));
    }
}
