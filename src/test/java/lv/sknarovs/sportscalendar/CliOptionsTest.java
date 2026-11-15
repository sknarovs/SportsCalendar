package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CliOptionsTest {
    @Test
    void usesDefaults() throws Exception {
        CliOptions options = CliOptions.parse();

        assertEquals(Path.of("cal", "latvia.ics"), options.output());
        assertEquals(Path.of("cache", "matches.tsv"), options.cache());
        assertEquals(365, options.lookbackDays());
    }

    @Test
    void acceptsAllSpellings() throws Exception {
        CliOptions options = CliOptions.parse("-o", "a.ics", "--cache=b.tsv", "-l30", "--test");

        assertEquals(Path.of("a.ics"), options.output());
        assertEquals(Path.of("b.tsv"), options.cache());
        assertEquals(30, options.lookbackDays());
        assertEquals(true, options.test());
    }
}
