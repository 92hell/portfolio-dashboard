package dev.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App settings, bound from application.yml / environment variables.
 *
 * @param profileLocation Spring resource location of the profile JSON (e.g. classpath:portfolio.json or file:./portfolio.json)
 * @param cvLocation      Spring resource location of the downloadable CV PDF (optional)
 * @param github          GitHub integration settings
 */
@ConfigurationProperties(prefix = "portfolio")
public record PortfolioProperties(String profileLocation, String cvLocation, Github github) {

    /**
     * @param apiUrl          GitHub API base, normally https://api.github.com
     * @param username        GitHub username whose public repos are shown; blank disables the integration
     * @param token           optional token - only needed for higher rate limits (60 req/h unauthenticated)
     * @param maxRepos        how many repos to show
     * @param includeForks    whether forked repos are listed
     * @param cacheMinutes    how long to cache GitHub responses
     */
    public record Github(String apiUrl, String username, String token, int maxRepos, boolean includeForks, int cacheMinutes) {
        public boolean enabled() {
            return username != null && !username.isBlank();
        }
    }
}
