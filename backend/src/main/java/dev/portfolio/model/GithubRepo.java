package dev.portfolio.model;

import java.time.OffsetDateTime;
import java.util.List;

/** Trimmed-down view of a GitHub repository, as served to the frontend. */
public record GithubRepo(
        long id,
        String name,
        String description,
        String url,
        String homepage,
        String language,
        int stars,
        int forks,
        List<String> topics,
        OffsetDateTime pushedAt) {}
