package lv.sknarovs.sportscalendar;

import java.util.List;

/** What a match page adds to a fixture row: venue and the goal timeline. */
record MatchDetails(Integer homeScore, Integer awayScore, String venue, List<Goal> goals) {

    MatchDetails {
        venue = venue == null ? "" : venue.strip();
        goals = goals == null ? List.of() : List.copyOf(goals);
    }

    static MatchDetails empty() {
        return new MatchDetails(null, null, "", List.of());
    }
}
