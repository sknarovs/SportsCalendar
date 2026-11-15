package lv.sknarovs.sportscalendar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Loads lff.lv pages. The site can be slow, so every page gets two time-limited attempts. */
final class LffClient implements PageSource {
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int ATTEMPTS = 2;

    private final Duration timeout;
    private final Duration attemptCap;
    private final Duration backoff;
    private final HttpClient client;

    LffClient() {
        this(Duration.ofSeconds(20), Duration.ofSeconds(60), Duration.ofSeconds(2));
    }

    /**
     * @param timeout    connect timeout and the time allowed until the response headers arrive
     * @param attemptCap upper bound for a whole attempt, so a stalled body cannot hang the run
     * @param backoff    pause before the second attempt
     */
    LffClient(Duration timeout, Duration attemptCap, Duration backoff) {
        this.timeout = timeout;
        this.attemptCap = attemptCap;
        this.backoff = backoff;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "lv,en;q=0.8")
                .timeout(timeout)
                .GET()
                .build();
        String error = "";
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            if (attempt > 1) {
                Thread.sleep(backoff);
            }
            try {
                return send(request, url);
            } catch (IOException | ExecutionException | TimeoutException e) {
                error = describe(e);
                System.err.println("[!] Attempt " + attempt + "/" + ATTEMPTS + " failed for " + url + ": " + error);
            }
        }
        throw new IOException("could not load " + url + " after " + ATTEMPTS + " attempts: " + error);
    }

    private String send(HttpRequest request, String url)
            throws IOException, ExecutionException, TimeoutException, InterruptedException {
        CompletableFuture<HttpResponse<String>> pending =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response;
        try {
            response = pending.get(attemptCap.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.cancel(true);
            throw e;
        }
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private String describe(Exception e) {
        if (e instanceof TimeoutException) {
            return "no complete response within " + attemptCap.toSeconds() + " s";
        }
        Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
