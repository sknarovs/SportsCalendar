package lv.sknarovs.sportscalendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads saved lff.lv pages from {@code src/test/resources/lff}. */
final class Pages {
    private Pages() {
    }

    static String load(String name) {
        try (InputStream in = Pages.class.getResourceAsStream("/lff/" + name)) {
            if (in == null) {
                throw new IllegalStateException("missing test page: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
