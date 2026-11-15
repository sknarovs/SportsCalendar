package lv.sknarovs.sportscalendar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/** Parses an LFF match page: final score, venue and the goal timeline with scorers. */
final class MatchPageParser {
    private static final Pattern DATE = Pattern.compile(
            "(\\d{1,2})\\s+(\\p{L}+)\\s+(\\d{4})(?:\\s+(\\d{1,2}):(\\d{2}))?");
    private static final Pattern FIRST_INTEGER = Pattern.compile("\\d+");
    /** Some futsal pages render the goal time as a match clock, e.g. {@code 02:02}. */
    private static final Pattern MATCH_CLOCK = Pattern.compile("(\\d{1,3}):\\d{2}");

    private MatchPageParser() {
    }

    static MatchDetails parse(String html) {
        Document doc = Jsoup.parse(html);
        Integer home = score(doc.selectFirst(".resultContainer .club1 .result .res"));
        Integer away = score(doc.selectFirst(".resultContainer .club2 .result .res"));
        return new MatchDetails(home, away,
                text(doc.selectFirst(".matchData .venue .stadium")),
                goals(doc));
    }

    /** Kept separate so tests can check that the kick-off on a match page agrees with the fixture list. */
    static LocalDate date(Document doc) {
        Matcher matcher = DATE.matcher(text(doc.selectFirst(".matchData .date")));
        if (!matcher.find()) {
            return null;
        }
        return LffDates.date(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    static LocalTime time(Document doc) {
        Matcher matcher = DATE.matcher(text(doc.selectFirst(".matchData .date")));
        if (!matcher.find() || matcher.group(4) == null) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
    }

    private static List<Goal> goals(Document doc) {
        List<Goal> goals = new ArrayList<>();
        for (Element event : doc.select(".matchprogress .tr.event.goal")) {
            Goal.Side side = side(event);
            if (side == null) {
                continue;
            }
            String minute = normalizeMinute(text(event.selectFirst(".minute h4")));
            Element scorer = event.selectFirst(".goal span b");
            String span = text(event.selectFirst(".goal span"));
            boolean ownGoal = span.toLowerCase().contains("savos vārtos")
                    || span.toLowerCase().contains("savos vartos");
            goals.add(new Goal(minute, side, text(scorer), ownGoal));
        }
        return goals;
    }

    /** The timeline marks the scoring side with {@code club1event} (home) or {@code club2event} (away). */
    private static Goal.Side side(Element event) {
        String classes = " " + event.className() + " ";
        if (classes.contains(" club1event ")) {
            return Goal.Side.HOME;
        }
        if (classes.contains(" club2event ")) {
            return Goal.Side.AWAY;
        }
        return null;
    }

    /** Turns 90 + stoppage markup (90+1’ or 45+2') into the plain {@code 90+1'} form. */
    private static String normalizeMinute(String minute) {
        String text = minute.replace('’', '\'').replace('‘', '\'').strip();
        Matcher clock = MATCH_CLOCK.matcher(text);
        return clock.matches() ? Integer.parseInt(clock.group(1)) + "'" : text;
    }

    private static Integer score(Element element) {
        if (element == null) {
            return null;
        }
        Matcher matcher = FIRST_INTEGER.matcher(element.text());
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().strip();
    }

    static Elements goalEvents(String html) {
        return Jsoup.parse(html).select(".matchprogress .tr.event.goal");
    }
}
