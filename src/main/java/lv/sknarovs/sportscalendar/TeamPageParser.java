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

/** Parses the fixture list embedded in an LFF national-team page. */
final class TeamPageParser {
    private static final String BASE_URL = "https://lff.lv";
    private static final Pattern FIRST_INTEGER = Pattern.compile("\\d+");

    private TeamPageParser() {
    }

    static List<Fixture> parse(String html, Category category) {
        Document doc = Jsoup.parse(html);
        List<Fixture> fixtures = new ArrayList<>();
        for (Element row : doc.select("div.fixtures div.tr.match")) {
            Fixture fixture = parseRow(row, category);
            if (fixture != null) {
                fixtures.add(fixture);
            }
        }
        return fixtures;
    }

    private static Fixture parseRow(Element row, Category category) {
        String id = row.attr("data-id").strip();
        Element dateCell = row.selectFirst(".date");
        Elements clubs = row.select(".clubs .club");
        if (id.isEmpty() || dateCell == null || clubs.size() < 2) {
            return null;
        }

        LocalDate date;
        try {
            date = LffDates.date(text(dateCell.selectFirst("h5")), text(dateCell.selectFirst("h6")),
                    text(dateCell.selectFirst(".h8")));
        } catch (RuntimeException e) {
            return null;
        }
        LocalTime time = LffDates.time(text(dateCell.selectFirst(".h7")));

        Element first = clubs.get(0);
        Element second = clubs.get(1);
        String home = team(first);
        String away = team(second);
        if (home.isEmpty() || away.isEmpty()) {
            return null;
        }

        return new Fixture(
                id,
                category,
                date,
                time,
                text(row.selectFirst(".competition")),
                home,
                away,
                score(first, "res1"),
                score(second, "res2"),
                score(first, "res1pen"),
                score(second, "res2pen"),
                stadium(row),
                matchUrl(row));
    }

    /** The team name can be wrapped in extra markup, so take the whole text of the title span. */
    private static String team(Element club) {
        Element title = club.selectFirst(".title span");
        return title == null ? "" : title.text().strip();
    }

    private static Integer score(Element club, String cssClass) {
        Element element = club.selectFirst(".result ." + cssClass);
        if (element == null) {
            element = club.selectFirst(".result .res");
        }
        return firstInteger(element == null ? "" : element.text());
    }

    /** The fixture list writes "-" when there is no venue yet. */
    private static String stadium(Element row) {
        String text = text(row.selectFirst(".stadium"));
        return "-".equals(text) ? "" : text;
    }

    /** Future futsal fixtures have no link to a match page at all. */
    private static String matchUrl(Element row) {
        for (Element link : row.select("a[href]")) {
            String href = link.attr("href");
            if (href.startsWith("/speles/")) {
                return BASE_URL + href;
            }
        }
        return null;
    }

    private static Integer firstInteger(String text) {
        Matcher matcher = FIRST_INTEGER.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().strip();
    }
}
