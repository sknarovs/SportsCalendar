package lv.sknarovs.sportscalendar;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/** Command-line options for the calendar generator. */
record CliOptions(Path output, Path cache, int lookbackDays, boolean test, boolean help) {
    static final String USAGE = "usage: sports-calendar [-h] [-o ICS] [-c CACHE] [-l DAYS] [-t]";
    static final String HELP = USAGE + "\n\n" + """
            Latvian national football and futsal team calendar generator

            options:
              -h, --help            show this help message and exit
              -o, --output ICS      Calendar file to write (default: cal/latvia.ics)
              -c, --cache CACHE     Match-details cache (default: cache/matches.tsv)
              -l, --lookback DAYS   Days of finished matches to keep (default: 365)
              -t, --test            Run the offline self-test and exit""";

    static final class UsageException extends Exception {
        UsageException(String message) {
            super(message);
        }
    }

    /** Accepts {@code --opt value}, {@code --opt=value}, {@code -o value} and {@code -ovalue}. */
    static CliOptions parse(String... args) throws UsageException {
        Path output = Path.of("cal", "latvia.ics");
        Path cache = Path.of("cache", "matches.tsv");
        int lookback = 365;
        boolean test = false;
        boolean help = false;
        Deque<String> tokens = tokens(args);
        while (!tokens.isEmpty()) {
            String option = tokens.removeFirst();
            switch (option) {
                case "-o", "--output" -> output = Path.of(value(tokens, "-o/--output"));
                case "-c", "--cache" -> cache = Path.of(value(tokens, "-c/--cache"));
                case "-l", "--lookback" -> lookback = integer(value(tokens, "-l/--lookback"), "-l/--lookback");
                case "-t", "--test" -> test = true;
                case "-h", "--help" -> help = true;
                default -> throw new UsageException("unrecognized arguments: " + option);
            }
        }
        if (lookback < 0) {
            throw new UsageException("argument -l/--lookback: must not be negative");
        }
        return new CliOptions(output, cache, lookback, test, help);
    }

    /** Splits {@code --opt=value} and {@code -ovalue} into separate option and value tokens. */
    private static Deque<String> tokens(String... args) {
        Deque<String> tokens = new ArrayDeque<>();
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (arg.startsWith("--") && equals > 0) {
                tokens.add(arg.substring(0, equals));
                tokens.add(arg.substring(equals + 1));
            } else if ((arg.startsWith("-o") || arg.startsWith("-c") || arg.startsWith("-l"))
                    && arg.length() > 2) {
                tokens.add(arg.substring(0, 2));
                tokens.add(arg.substring(2));
            } else {
                tokens.add(arg);
            }
        }
        return tokens;
    }

    private static String value(Deque<String> tokens, String option) throws UsageException {
        String next = tokens.peekFirst();
        if (next == null || (next.startsWith("-") && !next.matches("-\\d+"))) {
            throw new UsageException("argument " + option + ": expected one argument");
        }
        return tokens.removeFirst();
    }

    private static int integer(String value, String option) throws UsageException {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new UsageException("argument " + option + ": invalid int value: '" + value + "'");
        }
    }
}
