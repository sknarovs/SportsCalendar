package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class MatchPageParserTest {
    @Test
    void parsesFinishedFutsalMatch() {
        String html = Pages.load("match-futsal.html");
        MatchDetails details = MatchPageParser.parse(html);

        assertEquals(1, details.homeScore());
        assertEquals(5, details.awayScore());
        assertEquals("Baku Sports Hall, Baku", details.venue());
        assertEquals(6, details.goals().size());

        Goal first = details.goals().getFirst();
        assertEquals("1'", first.minute());
        assertEquals(Goal.Side.AWAY, first.side());
        assertEquals("Renards Ūdris", first.scorer());
        assertEquals(false, first.ownGoal());

        Goal last = details.goals().getLast();
        assertEquals("36'", last.minute());
        assertEquals(Goal.Side.HOME, last.side());
        assertEquals("Nihad Ismayilov", last.scorer());
    }

    @Test
    void parsesFootballMatchWithStoppageTime() {
        String html = Pages.load("match-football.html");
        MatchDetails details = MatchPageParser.parse(html);

        assertEquals(2, details.homeScore());
        assertEquals(1, details.awayScore());
        assertEquals("Podgorica City Stadium, Podgorica", details.venue());
        assertEquals(3, details.goals().size());

        assertEquals("70'", details.goals().get(0).minute());
        assertEquals(Goal.Side.HOME, details.goals().get(0).side());
        assertEquals("Nikola Krstović", details.goals().get(0).scorer());

        assertEquals("72'", details.goals().get(1).minute());
        assertEquals(Goal.Side.AWAY, details.goals().get(1).side());
        assertEquals("Deniss Meļņiks", details.goals().get(1).scorer());

        assertEquals("90+1'", details.goals().get(2).minute());
        assertEquals(Goal.Side.HOME, details.goals().get(2).side());
        assertEquals("Aleksa Maraš", details.goals().get(2).scorer());
    }

    @Test
    void parsesKickOffFromTheMatchHeader() {
        Document doc = Jsoup.parse(Pages.load("match-futsal.html"));

        assertEquals(LocalDate.of(2026, 11, 8), MatchPageParser.date(doc));
        assertEquals(LocalTime.of(18, 0), MatchPageParser.time(doc));
    }

    @Test
    void turnsAMatchClockIntoSoccerMinutes() {
        String html = """
                <div class="resultContainer"><div class="club1"><div class="result"><div class="res">0</div></div></div>
                <div class="club2"><div class="result"><div class="res">1</div></div></div></div>
                <div class="matchprogress"><div class="tr event club2event goal">
                <div class="minute"><h4 class="withseconds ">02:<span class="seconds">02</span></h4></div>
                <div class="goal"><span>Vārtus guva <b>Renards Ūdris</b></span></div>
                </div></div>
                """;

        MatchDetails details = MatchPageParser.parse(html);

        assertEquals(1, details.goals().size());
        assertEquals("2'", details.goals().getFirst().minute());
    }

    @Test
    void returnsEmptyDetailsForAPageWithoutAGoalTimeline() {
        MatchDetails details = MatchPageParser.parse("<html><body><h1>Nav</h1></body></html>");

        assertNull(details.homeScore());
        assertNull(details.awayScore());
        assertEquals("", details.venue());
        assertEquals(List.of(), details.goals());
    }
}
