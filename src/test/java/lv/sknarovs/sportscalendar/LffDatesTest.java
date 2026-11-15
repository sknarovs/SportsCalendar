package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class LffDatesTest {
    @Test
    void readsLatvianMonthAbbreviations() {
        assertEquals(LocalDate.of(2026, 6, 9), LffDates.date("9", "jūn", "2026"));
        assertEquals(LocalDate.of(2025, 10, 11), LffDates.date("11", "okt", "2025"));
        assertEquals(LocalDate.of(2026, 1, 28), LffDates.date("28", "jan", "2026"));
        assertEquals(LocalDate.of(2026, 12, 3), LffDates.date("3", "dec", "2026"));
    }

    @Test
    void readsTimesAndIgnoresEmptyCells() {
        assertEquals(LocalTime.of(21, 45), LffDates.time("21:45"));
        assertEquals(LocalTime.of(9, 5), LffDates.time("09:05"));
        assertNull(LffDates.time(""));
        assertNull(LffDates.time(null));
    }

    @Test
    void stripsDiacritics() {
        assertEquals("jun", LffDates.ascii("jūn"));
        assertEquals("sestdiena", LffDates.ascii("sestdiena"));
    }
}
