package lv.sknarovs.sportscalendar;

import java.time.Duration;

/** The two LFF national teams this calendar follows. */
enum Category {
    FOOTBALL("Futbols", "https://lff.lv/izlases/nacionala-izlase/", Duration.ofHours(2)),
    FUTSAL("Telpu futbols", "https://lff.lv/izlases/nacionala-izlase/telpu-futbola-izlase/",
            Duration.ofMinutes(105));

    private final String label;
    private final String baseUrl;
    private final Duration duration;

    Category(String label, String baseUrl, Duration duration) {
        this.label = label;
        this.baseUrl = baseUrl;
        this.duration = duration;
    }

    String label() {
        return label;
    }

    Duration duration() {
        return duration;
    }

    /** The fixture list is one reverse-chronological list, paginated 14 rows at a time. */
    String fixturesUrl(int page) {
        return baseUrl + "?pg=" + page + "&tab=fixtures";
    }
}
