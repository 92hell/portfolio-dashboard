package dev.portfolio.model;

import java.util.List;

/** Everything shown on the dashboard that is not pulled from GitHub. Edit portfolio.json to change it. */
public record Profile(
        String name,
        String title,
        String location,
        String summary,
        String avatarUrl,
        List<Link> links,
        List<Highlight> highlights,
        List<Win> wins,
        List<SkillGroup> skills,
        List<Experience> experience,
        List<Education> education,
        List<FeaturedProject> featuredProjects) {

    public record Link(String label, String url) {}

    /** A short, qualitative "what I do" card: an emoji, a title and one sentence. */
    public record Highlight(String icon, String title, String text) {}

    /**
     * A verified result told as a story. {@code before} is optional (e.g. for a delivery, not a speed-up).
     * Only put numbers here that you have measured.
     */
    public record Win(String title, String before, String after, String detail) {}

    public record SkillGroup(String category, List<String> items) {}

    public record Experience(String company, String role, String start, String end, String location, List<String> bullets) {}

    public record Education(String school, String degree, String start, String end) {}

    public record FeaturedProject(String name, String description, String url, List<String> tags) {}
}
