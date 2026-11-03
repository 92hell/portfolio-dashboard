package dev.portfolio.model;

import java.util.List;

/** Everything shown on the dashboard that is not pulled from GitLab. Edit portfolio.json to change it. */
public record Profile(
        String name,
        String title,
        String location,
        String summary,
        String avatarUrl,
        List<Link> links,
        List<Stat> highlights,
        List<SkillGroup> skills,
        List<Experience> experience,
        List<Education> education,
        List<FeaturedProject> featuredProjects) {

    public record Link(String label, String url) {}

    public record Stat(String label, String value) {}

    public record SkillGroup(String category, List<String> items) {}

    public record Experience(String company, String role, String start, String end, String location, List<String> bullets) {}

    public record Education(String school, String degree, String start, String end) {}

    public record FeaturedProject(String name, String description, String url, List<String> tags) {}
}
