package lv.sknarovs.sportscalendar;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the Latvian date and time spellings used on lff.lv. */
final class LffDates {
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})[:.](\\d{2})");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1),
            Map.entry("feb", 2),
            Map.entry("mar", 3),
            Map.entry("apr", 4),
            Map.entry("mai", 5),
            Map.entry("jun", 6),
            Map.entry("jul", 7),
            Map.entry("aug", 8),
            Map.entry("sep", 9),
            Map.entry("okt", 10),
            Map.entry("nov", 11),
            Map.entry("dec", 12));

    private LffDates() {
    }

    /** Builds a date from the separate day, month and year cells, e.g. {@code 15}, {@code nov}, {@code 2026}. */
    static LocalDate date(String day, String month, String year) {
        int d = Integer.parseInt(day.strip());
        int y = Integer.parseInt(year.strip());
        String key = ascii(month).toLowerCase(Locale.ROOT).strip();
        Integer m = MONTHS.get(key.length() > 3 ? key.substring(0, 3) : key);
        if (m == null) {
            throw new IllegalArgumentException("unknown month: " + month);
        }
        return LocalDate.of(y, m, d);
    }

    /** Returns null when the time is still unannounced. */
    static LocalTime time(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = TIME.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    /** Drops Latvian diacritics, so {@code jūn} matches {@code jun}. */
    static String ascii(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
