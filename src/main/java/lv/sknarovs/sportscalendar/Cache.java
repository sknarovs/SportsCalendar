package lv.sknarovs.sportscalendar;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persistent match details. The file is a tab-separated table committed next to the calendar, so a run only
 * fetches match pages for games whose score changed, plus games played in the last couple of days.
 */
final class Cache {
    private static final String HEADER = "# sports-calendar cache v1";
    private static final int COLUMNS = 9;

    record Entry(
            long stampMillis,
            String hash,
            Integer homeScore,
            Integer awayScore,
            Integer homePen,
            Integer awayPen,
            String venue,
            List<Goal> goals) {

        MatchDetails toDetails() {
            return new MatchDetails(homeScore, awayScore, venue, goals);
        }
    }

    private Cache() {
    }

    static Map<String, Entry> load(Path file) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return entries;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            Parsed parsed = parseLine(line);
            if (parsed != null) {
                entries.put(parsed.id(), parsed.entry());
            }
        }
        return entries;
    }

    static void save(Path file, Map<String, Entry> entries) throws IOException {
        writeAtomically(file, saveToString(entries));
    }

    static String saveToString(Map<String, Entry> entries) {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (Map.Entry<String, Entry> e : new TreeMap<>(entries).entrySet()) {
            Entry entry = e.getValue();
            out.append(e.getKey()).append('\t')
                    .append(entry.stampMillis()).append('\t')
                    .append(entry.hash()).append('\t')
                    .append(value(entry.homeScore())).append('\t')
                    .append(value(entry.awayScore())).append('\t')
                    .append(value(entry.homePen())).append('\t')
                    .append(value(entry.awayPen())).append('\t')
                    .append(escape(entry.venue())).append('\t')
                    .append(encodeGoals(entry.goals())).append('\n');
        }
        return out.toString();
    }

    private record Parsed(String id, Entry entry) {
    }

    private static Parsed parseLine(String line) {
        if (line.isBlank() || line.startsWith("#")) {
            return null;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length != COLUMNS) {
            return null;
        }
        try {
            String id = parts[0].strip();
            long stamp = Long.parseLong(parts[1]);
            String hash = parts[2];
            return new Parsed(id, new Entry(stamp, hash, integer(parts[3]), integer(parts[4]),
                    integer(parts[5]), integer(parts[6]), unescape(parts[7]), decodeGoals(parts[8])));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Integer integer(String value) {
        return value.isEmpty() || "-".equals(value) ? null : Integer.valueOf(value);
    }

    private static String value(Integer number) {
        return number == null ? "" : number.toString();
    }

    private static String encodeGoals(List<Goal> goals) {
        List<String> encoded = new ArrayList<>();
        for (Goal goal : goals) {
            encoded.add(escape(goal.minute()) + "|" + goal.side() + "|" + escape(goal.scorer()) + "|"
                    + (goal.ownGoal() ? "1" : "0"));
        }
        return String.join(";", encoded);
    }

    private static List<Goal> decodeGoals(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        List<Goal> goals = new ArrayList<>();
        for (String encoded : text.split(";", -1)) {
            String[] parts = encoded.split("\\|", -1);
            if (parts.length != 4) {
                continue;
            }
            goals.add(new Goal(unescape(parts[0]), Goal.Side.valueOf(parts[1]), unescape(parts[2]),
                    "1".equals(parts[3])));
        }
        return goals;
    }

    /** Escapes the characters that would break the tab-separated layout or the goal encoding. */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '|' -> out.append("\\p");
                case ';' -> out.append("\\s");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String unescape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 == text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case '\\' -> out.append('\\');
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 'p' -> out.append('|');
                case 's' -> out.append(';');
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    static void writeAtomically(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
