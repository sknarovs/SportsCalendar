package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheTest {
    @TempDir
    Path dir;

    @Test
    void roundTripsAllFields() throws Exception {
        Map<String, Cache.Entry> entries = new LinkedHashMap<>();
        entries.put("333", new Cache.Entry(1_700_000_000_000L, "abc",
                1, 5, 4, 3, "Baku, Hall; A\tB\nC",
                List.of(new Goal("1'", Goal.Side.AWAY, "Renards Ūdris", false),
                        new Goal("90+1'", Goal.Side.HOME, "Name | weird; chars \\ here", true))));

        Path file = dir.resolve("cache.tsv");
        Cache.save(file, entries);

        assertEquals(entries, Cache.load(file));
    }

    @Test
    void ignoresMalformedLines() throws Exception {
        Path file = dir.resolve("cache.tsv");
        Files.writeString(file, """
                # sports-calendar cache v1
                garbage
                333\t1700000000000\tabc\t1\t5\t\t\tVenue\t1'|AWAY|Scorer|0
                444\tnot-a-number\tabc\t1\t5\t\t\tVenue\t
                """, StandardCharsets.UTF_8);

        Map<String, Cache.Entry> loaded = Cache.load(file);

        assertEquals(1, loaded.size());
        assertTrue(loaded.containsKey("333"));
        assertEquals("Venue", loaded.get("333").venue());
        assertEquals(1, loaded.get("333").goals().size());
    }

    @Test
    void storesMissingNumbersAsEmptyCells() throws Exception {
        Map<String, Cache.Entry> entries = Map.of("1",
                new Cache.Entry(1L, "h", null, null, null, null, "", List.of()));
        Path file = dir.resolve("cache.tsv");
        Cache.save(file, entries);

        Cache.Entry loaded = Cache.load(file).get("1");
        assertNull(loaded.homeScore());
        assertNull(loaded.awayScore());
        assertEquals("", loaded.venue());
        assertTrue(loaded.goals().isEmpty());
    }

    @Test
    void escapesAndUnescapesTheDelimiters() {
        String original = "a\tb\nc\\d|e;f";

        assertEquals(original, Cache.unescape(Cache.escape(original)));
    }

    @Test
    void missingFileLoadsEmpty() throws Exception {
        assertTrue(Cache.load(dir.resolve("absent.tsv")).isEmpty());
    }
}
