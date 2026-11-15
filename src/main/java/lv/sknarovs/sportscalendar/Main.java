package lv.sknarovs.sportscalendar;

import java.io.PrintStream;
import java.time.Clock;

/** Fetches the LFF national-team fixtures and writes one iCalendar file. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, new LffClient(), Clock.systemUTC(), System.out, System.err));
    }

    /** Returns the process exit code: 0 on success, 1 on a failed run, 2 on bad arguments. */
    static int run(String[] args, PageSource source, Clock clock, PrintStream out, PrintStream err) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (CliOptions.UsageException e) {
            err.println(e.getMessage());
            err.println(CliOptions.USAGE);
            return 2;
        }
        if (options.help()) {
            out.println(CliOptions.HELP);
            return 0;
        }
        if (options.test()) {
            return SelfTest.run(out) ? 0 : 1;
        }
        try {
            return new CalendarUpdater(source, clock, options.output(), options.cache(), options.lookbackDays(),
                    out, err).run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("Interrupted.");
            return 1;
        } catch (Exception e) {
            err.println("Error: " + e);
            return 1;
        }
    }
}
