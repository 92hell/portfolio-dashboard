package dev.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.portfolio.config.PortfolioProperties;
import dev.portfolio.model.GithubRepo;
import dev.portfolio.service.GithubService;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * GitHub outages and rate limits must not slow the page down or turn into "no repositories":
 * failures are remembered for a short while, slow responses time out, and an earlier good list
 * keeps being served. Runs against a real local HTTP server so the timeouts are exercised.
 */
@DisplayName("GitHub fetching when GitHub misbehaves")
class GithubServiceTest {

    private static final String ONE_REPO = """
            [{"id":1,"name":"demo","html_url":"https://github.com/someone/demo","stargazers_count":2,
              "forks_count":0,"archived":false,"fork":false,"pushed_at":"2026-01-01T00:00:00Z"}]""";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    /** What the fake GitHub answers next: a status code, a body, and an optional delay. */
    private final AtomicReference<Answer> answer = new AtomicReference<>(new Answer(200, ONE_REPO, 0));

    record Answer(int status, String body, long delayMillis) {}

    @BeforeEach
    void startFakeGithub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            Answer a = answer.get();
            try {
                Thread.sleep(a.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = a.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(a.status(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeGithub() {
        server.stop(0);
    }

    private GithubService service(int cacheMinutes, Duration retryAfter, Duration readTimeout) {
        var github = new PortfolioProperties.Github("http://127.0.0.1:" + server.getAddress().getPort(),
                "someone", "", 9, false, cacheMinutes, retryAfter, Duration.ofSeconds(2), readTimeout);
        return new GithubService(new PortfolioProperties("", "", github), RestClient.builder());
    }

    @Test
    @DisplayName("a failure is remembered, so page views do not all wait on GitHub again")
    void failureIsCachedForRetryAfter() {
        answer.set(new Answer(503, "{}", 0));
        GithubService github = service(30, Duration.ofMinutes(1), Duration.ofSeconds(2));

        assertThat(github.getRepos()).isEmpty();
        assertThat(github.unavailable()).isTrue();
        github.getRepos();
        github.getRepos();

        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("after the retry pause GitHub is asked again, and recovery clears the error")
    void retriesAfterThePause() {
        answer.set(new Answer(503, "{}", 0));
        GithubService github = service(30, Duration.ZERO, Duration.ofSeconds(2));
        github.getRepos();

        answer.set(new Answer(200, ONE_REPO, 0));
        List<GithubRepo> repos = github.getRepos();

        assertThat(calls).hasValue(2);
        assertThat(repos).extracting(GithubRepo::name).containsExactly("demo");
        assertThat(github.unavailable()).isFalse();
    }

    @Test
    @DisplayName("when a refresh fails, the last good list is still served and not reported as unavailable")
    void staleListSurvivesAFailedRefresh() {
        GithubService github = service(0, Duration.ofMinutes(1), Duration.ofSeconds(2));
        github.getRepos();

        answer.set(new Answer(403, "{\"message\":\"API rate limit exceeded\"}", 0));
        List<GithubRepo> repos = github.getRepos();

        assertThat(repos).extracting(GithubRepo::name).containsExactly("demo");
        assertThat(github.unavailable()).isFalse();
    }

    @Test
    @DisplayName("a slow GitHub times out instead of holding up the page")
    void slowGithubTimesOut() {
        answer.set(new Answer(200, ONE_REPO, 3_000));
        GithubService github = service(30, Duration.ofMinutes(1), Duration.ofMillis(300));

        long start = System.nanoTime();
        List<GithubRepo> repos = github.getRepos();
        long tookMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(repos).isEmpty();
        assertThat(github.unavailable()).isTrue();
        assertThat(tookMillis).isLessThan(2_000);
    }
}
