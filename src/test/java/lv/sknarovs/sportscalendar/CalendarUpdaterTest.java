package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CalendarUpdaterTest {
    private static final Instant T1 = Instant.parse("2026-11-15T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-16T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-17T09:00:00Z");
    private static final String BASE = "https://lff.lv";
    private static final String FOOTBALL_PAGE =
            "https://lff.lv/izlases/nacionala-izlase/?pg=1&tab=fixtures";
    private static final String FUTSAL_PAGE =
            "https://lff.lv/izlases/nacionala-izlase/telpu-futbola-izlase/?pg=1&tab=fixtures";
    private static final String FOOTBALL_MATCH = BASE + "/speles/azerbaidzana-latvija-333/";
    private static final String FUTSAL_MATCH = BASE + "/speles/albanija-latvija-444/";

    @TempDir
    Path dir;

    @Test
    void writesCalendarAndCache() throws Exception {
        FakeSource source = new FakeSource(pages());
        Path ics = dir.resolve("cal/latvia.ics");
        Path cache = dir.resolve("cache/matches.tsv");

        int code = run(source, clock(T1), ics, cache);

        assertEquals(0, code);
        String calendar = Files.readString(ics);
        assertTrue(calendar.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(calendar.contains("X-WR-CALNAME:Latvija – futbols un telpu futbols\r\n"));
        assertTrue(calendar.contains("SUMMARY:Latvija vs Armēnija (Nāciju līga)\r\n"));
        assertTrue(calendar.contains("SUMMARY:Azerbaidžāna 1-5 Latvija (Pārbaudes spēle)\r\n"));
        assertTrue(calendar.contains("SUMMARY:Albānija 3-2 Latvija (PK kvalifikācija)\r\n"));
        assertTrue(calendar.contains("UID:lff-333@sportscalendar\r\n"));
        assertTrue(calendar.contains("DTSTART;TZID=Europe/Riga:20261108T180000\r\n"));
        assertTrue(calendar.contains("DTSTART;VALUE=DATE:20270303\r\n"));
        assertTrue(calendar.contains("DTEND;VALUE=DATE:20270304\r\n"));
        assertEquals(4, count(calendar, "BEGIN:VEVENT"));

        Map<String, Cache.Entry> entries = Cache.load(cache);
        assertEquals(4, entries.size());
        assertEquals(2, entries.get("333").goals().size());
        assertEquals(1, entries.get("444").goals().size());

        assertEquals(4, source.requested.size());
        assertFalse(source.requested.stream().anyMatch(url -> url.contains("pg=2")));
        assertFalse(source.requested.stream().anyMatch(url -> url.contains("old-")));
    }

    @Test
    void secondRunReusesCacheAndKeepsFileStable() throws Exception {
        FakeSource source = new FakeSource(pages());
        Path ics = dir.resolve("cal/latvia.ics");
        Path cache = dir.resolve("cache/matches.tsv");

        run(source, clock(T1), ics, cache);
        String first = Files.readString(ics);

        source.requested.clear();
        run(source, clock(T2), ics, cache);
        String second = Files.readString(ics);

        assertEquals(first, second);
        assertEquals(List.of(FOOTBALL_PAGE, FUTSAL_PAGE), source.requested);
    }

    @Test
    void restampsOnlyTheEventThatChanged() throws Exception {
        Map<String, String> pages = pages();
        FakeSource source = new FakeSource(pages);
        Path ics = dir.resolve("cal/latvia.ics");
        Path cache = dir.resolve("cache/matches.tsv");

        run(source, clock(T1), ics, cache);

        pages.put(FOOTBALL_PAGE, TestHtml.teamPage(
                TestHtml.row("111", "15", "nov", "2026", "16:00", "Nāciju līga", "Latvija", "Armēnija", "-", "-",
                        "Skonto Stadions", "/speles/latvija-armenija-111/"),
                TestHtml.row("333", "8", "nov", "2026", "18:00", "Pārbaudes spēle", "Azerbaidžāna", "Latvija", "2",
                        "5", "Baku Sports Hall", "/speles/azerbaidzana-latvija-333/"),
                oldRow("999")));
        pages.put(FOOTBALL_MATCH, TestHtml.matchPage("08 nov 2026 18:00", "Baku Sports Hall", "Pārbaudes spēle",
                "Azerbaidžāna", "Latvija", "2", "5", "1’|AWAY|Renards Ūdris", "90+1'|HOME|Nihad Ismayilov"));

        run(source, clock(T3), ics, cache);
        String calendar = Files.readString(ics);

        assertTrue(calendar.contains("SUMMARY:Azerbaidžāna 2-5 Latvija (Pārbaudes spēle)\r\n"));
        assertEquals("20261117T090000Z", dtstamp(calendar, "lff-333@sportscalendar"));
        assertEquals("20261115T090000Z", dtstamp(calendar, "lff-444@sportscalendar"));
        assertEquals("20261115T090000Z", dtstamp(calendar, "lff-111@sportscalendar"));
    }

    @Test
    void keepsTheExistingCalendarWhenNothingIsFound() throws Exception {
        Map<String, String> pages = new HashMap<>();
        pages.put(FOOTBALL_PAGE, "<div class=\"fixtures\"></div>");
        pages.put(FUTSAL_PAGE, "<div class=\"fixtures\"></div>");
        Path ics = dir.resolve("cal/latvia.ics");
        Path cache = dir.resolve("cache/matches.tsv");
        Files.createDirectories(ics.getParent());
        Files.writeString(ics, "OLD");

        int code = run(new FakeSource(pages), clock(T1), ics, cache);

        assertEquals(1, code);
        assertEquals("OLD", Files.readString(ics));
        assertFalse(Files.exists(cache));
    }

    private int run(FakeSource source, Clock clock, Path ics, Path cache) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        return new CalendarUpdater(source, clock, ics, cache, 365, stream, stream).run();
    }

    private static Map<String, String> pages() {
        Map<String, String> pages = new HashMap<>();
        pages.put(FOOTBALL_PAGE, TestHtml.teamPage(
                TestHtml.row("111", "15", "nov", "2026", "16:00", "Nāciju līga", "Latvija", "Armēnija", "-", "-",
                        "Skonto Stadions", "/speles/latvija-armenija-111/"),
                TestHtml.row("333", "8", "nov", "2026", "18:00", "Pārbaudes spēle", "Azerbaidžāna", "Latvija", "1",
                        "5", "Baku Sports Hall", "/speles/azerbaidzana-latvija-333/"),
                oldRow("999")));
        pages.put(FOOTBALL_MATCH, TestHtml.matchPage("08 nov 2026 18:00", "Baku Sports Hall", "Pārbaudes spēle",
                "Azerbaidžāna", "Latvija", "1", "5", "1’|AWAY|Renards Ūdris", "90+1'|HOME|Nihad Ismayilov"));
        pages.put(FUTSAL_PAGE, TestHtml.teamPage(
                TestHtml.row("555", "3", "mar", "2027", "", "PK kvalifikācija", "Latvija", "Čehija", "-", "-",
                        "-", null),
                TestHtml.row("444", "20", "okt", "2026", "19:30", "PK kvalifikācija", "Albānija", "Latvija", "3", "2",
                        "Hall", "/speles/albanija-latvija-444/"),
                oldRow("998")));
        pages.put(FUTSAL_MATCH, TestHtml.matchPage("20 okt 2026 19:30", "Hall", "PK kvalifikācija",
                "Albānija", "Latvija", "3", "2", "22'|AWAY|Miks Babris"));
        return pages;
    }

    /** A fixture outside the lookback window, which also stops pagination. */
    private static String oldRow(String id) {
        return TestHtml.row(id, "1", "jan", "2024", "12:00", "Vecs", "Latvija", "X", "0", "0", "",
                "/speles/old-" + id + "/");
    }

    private static Clock clock(Instant instant) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return instant;
            }
        };
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private static String dtstamp(String calendar, String uid) {
        int uidAt = calendar.indexOf("UID:" + uid);
        int eventAt = calendar.lastIndexOf("BEGIN:VEVENT", uidAt);
        int stampAt = calendar.indexOf("DTSTAMP:", eventAt);
        return calendar.substring(stampAt + "DTSTAMP:".length(), calendar.indexOf("\r\n", stampAt));
    }

    private static final class FakeSource implements PageSource {
        private final Map<String, String> pages;
        private final List<String> requested = new ArrayList<>();

        private FakeSource(Map<String, String> pages) {
            this.pages = pages;
        }

        @Override
        public String fetch(String url) throws IOException {
            requested.add(url);
            String page = pages.get(url);
            if (page == null) {
                throw new IOException("no test page for " + url);
            }
            return page;
        }
    }
}
