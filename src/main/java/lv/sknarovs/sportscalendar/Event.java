package lv.sknarovs.sportscalendar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/** A single calendar entry: the fixture plus whatever the match page added. */
record Event(Fixture fixture, String venue, List<Goal> goals, Instant stamp) {

    Event {
        venue = venue == null ? "" : venue.strip();
        goals = goals == null ? List.of() : List.copyOf(goals);
        stamp = stamp == null ? Instant.EPOCH : stamp;
    }

    Event withStamp(Instant newStamp) {
        return new Event(fixture, venue, goals, newStamp);
    }

    /**
     * Everything that affects the rendered event. The updater hashes this so {@code DTSTAMP} only moves when
     * the event really changes, which keeps daily commits limited to actual news.
     */
    String signature() {
        return String.join("\u0001",
                fixture.id(),
                fixture.category().name(),
                fixture.date().toString(),
                String.valueOf(fixture.time()),
                fixture.competition(),
                fixture.homeTeam(),
                fixture.awayTeam(),
                String.valueOf(fixture.homeScore()),
                String.valueOf(fixture.awayScore()),
                String.valueOf(fixture.homePen()),
                String.valueOf(fixture.awayPen()),
                venue,
                String.valueOf(fixture.url()),
                goals.stream()
                        .map(g -> g.minute() + "\u0002" + g.side() + "\u0002" + g.scorer() + "\u0002" + g.ownGoal())
                        .reduce((a, b) -> a + "\u0003" + b)
                        .orElse(""));
    }

    /** Hex SHA-256 of {@link #signature()}, stored in the cache to detect real changes. */
    String contentHash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(signature().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
