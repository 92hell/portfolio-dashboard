package dev.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the wording rules from the verified KPI notes (3 Nov 2026), so later edits cannot quietly
 * drop a qualifier or reintroduce a claim that should not be made.
 */
class PortfolioClaimsTest {

    /** Each claim as one unit: a whole card (win, highlight, project) or a single bullet / summary. */
    private static List<String> claims() throws Exception {
        JsonNode root;
        try (InputStream in = PortfolioClaimsTest.class.getResourceAsStream("/portfolio.json")) {
            root = new ObjectMapper().readTree(in);
        }
        List<String> claims = new ArrayList<>();
        claims.add(root.get("summary").asText());
        for (String cards : List.of("wins", "highlights", "featuredProjects")) {
            root.get(cards).forEach(card -> claims.add(card.toString()));
        }
        root.get("experience").forEach(job -> job.get("bullets").forEach(b -> claims.add(b.asText())));
        return claims;
    }

    @Test
    void theSlowQueryResultIsAlwaysQualifiedAsStagingTests() throws Exception {
        assertThat(claims()).filteredOn(c -> c.contains("49%"))
                .isNotEmpty()
                .allSatisfy(c -> assertThat(c).contains("in staging tests"));
    }

    @Test
    void claimsMarkedDoNotClaimAreAbsent() throws Exception {
        assertThat(String.join("\n", claims()))
                .doesNotContain("35-45 s", "35–45 s", "31 h", "lines of code", "100k");
    }
}
