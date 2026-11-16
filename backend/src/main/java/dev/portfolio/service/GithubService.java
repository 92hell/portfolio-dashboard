package dev.portfolio.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.portfolio.config.PortfolioProperties;
import dev.portfolio.model.GithubRepo;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches the user's public GitHub repositories, with a simple in-memory cache. A failed fetch is
 * remembered too (for {@code retry-after}), so an outage or rate limit does not make every page view
 * wait on GitHub again; the last good list, if any, keeps being served meanwhile.
 */
@Service
public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);

    private final PortfolioProperties.Github config;
    private final RestClient client;

    private volatile List<GithubRepo> cached = List.of();
    private volatile Instant nextFetchAt = Instant.EPOCH;
    private volatile boolean lastFetchFailed = false;

    public GithubService(PortfolioProperties props, RestClient.Builder builder) {
        this.config = props.github();
        HttpClient http = HttpClient.newBuilder().connectTimeout(config.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(config.readTimeout());
        RestClient.Builder b = builder.baseUrl(config.apiUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (config.token() != null && !config.token().isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token());
        }
        this.client = b.build();
    }

    public boolean enabled() {
        return config.enabled();
    }

    public String profileUrl() {
        return enabled() ? "https://github.com/" + config.username() : "";
    }

    public synchronized List<GithubRepo> getRepos() {
        if (!enabled()) {
            return List.of();
        }
        if (Instant.now().isBefore(nextFetchAt)) {
            return cached;
        }
        try {
            List<ApiRepo> raw = client.get()
                    .uri(u -> u.path("/users/{user}/repos")
                            .queryParam("type", "owner")
                            .queryParam("sort", "pushed")
                            .queryParam("per_page", 100)
                            .build(config.username()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            cached = (raw == null ? List.<ApiRepo>of() : raw).stream()
                    .filter(r -> !r.archived())
                    .filter(r -> config.includeForks() || !r.fork())
                    .map(ApiRepo::toModel)
                    .sorted(Comparator.comparingInt(GithubRepo::stars).reversed()
                            .thenComparing(GithubRepo::pushedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(config.maxRepos())
                    .toList();
            lastFetchFailed = false;
            nextFetchAt = Instant.now().plusSeconds(config.cacheMinutes() * 60L);
        } catch (RestClientException e) {
            // Serve stale data (or nothing) rather than breaking the whole page.
            log.warn("GitHub fetch failed for user '{}': {}", config.username(), e.getMessage());
            lastFetchFailed = true;
            nextFetchAt = Instant.now().plus(config.retryAfter());
        }
        return cached;
    }

    /**
     * True when the latest attempt to reach GitHub failed and no earlier list is available, so the
     * page can say "couldn't load" (with a link to GitHub) instead of "no repositories".
     */
    public boolean unavailable() {
        return enabled() && lastFetchFailed && cached.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiRepo(
            long id,
            String name,
            String description,
            @JsonProperty("html_url") String htmlUrl,
            String homepage,
            String language,
            @JsonProperty("stargazers_count") int stargazersCount,
            @JsonProperty("forks_count") int forksCount,
            List<String> topics,
            boolean archived,
            boolean fork,
            @JsonProperty("pushed_at") OffsetDateTime pushedAt) {

        GithubRepo toModel() {
            return new GithubRepo(id, name, description, htmlUrl,
                    homepage == null || homepage.isBlank() ? null : homepage,
                    language, stargazersCount, forksCount,
                    topics == null ? List.of() : topics, pushedAt);
        }
    }
}
