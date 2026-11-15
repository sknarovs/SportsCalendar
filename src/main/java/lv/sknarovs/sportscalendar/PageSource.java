package lv.sknarovs.sportscalendar;

import java.io.IOException;

/** Fetches one HTML page. Tests substitute a fixed map of URLs to saved pages. */
interface PageSource {
    String fetch(String url) throws IOException, InterruptedException;
}
