package lv.sknarovs.sportscalendar;

/** One entry of the goal timeline on an LFF match page. */
record Goal(String minute, Side side, String scorer, boolean ownGoal) {

    enum Side {
        HOME,
        AWAY
    }

    Goal {
        minute = minute == null ? "" : minute.strip();
        scorer = scorer == null ? "" : scorer.strip();
    }
}
