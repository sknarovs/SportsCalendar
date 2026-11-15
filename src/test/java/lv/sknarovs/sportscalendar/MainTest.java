package lv.sknarovs.sportscalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void selfTestPasses() {
        Result result = run("--test");

        assertEquals(0, result.code());
        assertTrue(result.out().contains("[+] Self-test passed."));
    }

    @Test
    void printsHelp() {
        Result result = run("--help");

        assertEquals(0, result.code());
        assertTrue(result.out().contains(CliOptions.USAGE));
        assertTrue(result.out().contains("--lookback"));
    }

    @Test
    void rejectsUnknownOption() {
        Result result = run("--bogus");

        assertEquals(2, result.code());
        assertTrue(result.err().contains("unrecognized arguments: --bogus"));
    }

    @Test
    void rejectsMissingValue() {
        Result result = run("-l");

        assertEquals(2, result.code());
        assertTrue(result.err().contains("expected one argument"));
    }

    @Test
    void rejectsNegativeLookback() {
        Result result = run("-l", "-5");

        assertEquals(2, result.code());
        assertTrue(result.err().contains("must not be negative"));
    }

    private static Result run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PageSource source = url -> {
            throw new AssertionError("these options must not fetch: " + url);
        };
        int code = Main.run(args, source, Clock.systemUTC(),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private record Result(int code, String out, String err) {
    }
}
