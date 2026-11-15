package lv.sknarovs.sportscalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One row of an LFF national-team fixture list. */
record Fixture(
        String id,
        Category category,
        LocalDate date,
        LocalTime time,
        String competition,
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        Integer homePen,
        Integer awayPen,
        String stadium,
        String url) {

    Fixture {
        competition = orEmpty(competition);
        homeTeam = orEmpty(homeTeam);
        awayTeam = orEmpty(awayTeam);
        stadium = orEmpty(stadium);
        url = url == null || url.isBlank() ? null : url;
    }

    /** A match counts as played once the fixture list shows both scores. */
    boolean finished() {
        return homeScore != null && awayScore != null;
    }

    /** Kick-off in Europe/Riga, or null of the time is still unannounced. */
    LocalDateTime kickoff() {
        return time == null ? null : date.atTime(time);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
