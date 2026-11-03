package dev.portfolio.controller;

import dev.portfolio.model.Profile;
import dev.portfolio.service.GithubService;
import dev.portfolio.service.ProfileService;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PortfolioController {

    private final ProfileService profileService;
    private final GithubService githubService;

    public PortfolioController(ProfileService profileService, GithubService githubService) {
        this.profileService = profileService;
        this.githubService = githubService;
    }

    @GetMapping("/profile")
    public Profile profile() {
        return profileService.getProfile();
    }

    @GetMapping("/repos")
    public Map<String, Object> repos() {
        return Map.of(
                "enabled", githubService.enabled(),
                "profileUrl", githubService.profileUrl(),
                "repos", githubService.getRepos(),
                "hasCv", profileService.getCv() != null);
    }

    @GetMapping("/cv")
    public ResponseEntity<Resource> cv() {
        Resource cv = profileService.getCv();
        if (cv == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("CV.pdf").build().toString())
                .body(cv);
    }
}
