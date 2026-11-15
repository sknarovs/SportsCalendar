package lv.sknarovs.sportscalendar;

/** Builds the small slices of lff.lv markup that the updater tests need. */
final class TestHtml {
    private TestHtml() {
    }

    static String teamPage(String... rows) {
        return "<div class=\"fixtures\">" + String.join("\n", rows) + "</div>";
    }

    static String row(String id, String day, String month, String year, String time, String competition,
            String home, String away, String homeScore, String awayScore, String stadium, String url) {
        String timeCell = time.isEmpty() ? "" : "<div class=\"h7\">" + time + "</div>";
        String link = url == null ? "<span></span>" : "<a href=\"" + url + "\">&nbsp;</a>";
        return """
                <div class="tr match" data-id="%s"><div><div class="date"><h4>diena</h4><h5>%s</h5><h6>%s</h6>%s\
                <div class="h8">%s</div></div></div><div><div class="competition">%s</div><div class="clubs">\
                <div class="club"><div class="title"><span>%s</span></div><div class="result">\
                <span class="res1">%s</span></div></div><div class="club"><div class="title"><span>%s</span></div>\
                <div class="result"><span class="res2">%s</span></div></div></div><div class="stadium">%s</div>\
                </div><div>%s</div></div>
                """.formatted(id, day, month, timeCell, year, competition, home, homeScore, away, awayScore, stadium,
                link);
    }

    /** Goals are given as {@code minute|HOME or AWAY|scorer}. */
    static String matchPage(String date, String venue, String competition, String home, String away,
            String homeScore, String awayScore, String... goals) {
        StringBuilder timeline = new StringBuilder();
        for (String goal : goals) {
            timeline.append(goal(goal));
        }
        return """
                <div class="matchData"><ul><li class="date">%s</li><li class="venue"><span class="stadium">%s</span></li>\
                <li class="competition"><span>%s</span></li></ul></div>
                <div class="resultContainer"><div class="line1"><div class="club1"><span>%s</span>\
                <div class="result"><div class="res">%s</div></div></div><div class="club2"><span>%s</span>\
                <div class="result"><div class="res">%s</div></div></div></div></div>
                <div class="matchprogress">%s</div>
                """.formatted(date, venue, competition, home, homeScore, away, awayScore, timeline);
    }

    private static String goal(String encoded) {
        String[] parts = encoded.split("\\|", 3);
        String side = "HOME".equals(parts[1]) ? "club1event" : "club2event";
        return "<div class=\"tr event " + side + " goal\"><div class=\"minute\"><h4>" + parts[0] + "</h4></div>"
                + "<div class=\"goal\"><h3 class=\"big\">&nbsp;</h3><span>Vārtus guva <b>" + parts[2]
                + "</b></span></div></div>";
    }
}
