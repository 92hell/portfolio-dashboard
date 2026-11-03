package dev.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "portfolio.github.username=")
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void servesProfile() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.skills").isArray())
                // Highlights are short qualitative statements (title + sentence), not numeric claims.
                .andExpect(jsonPath("$.highlights[0].title").isNotEmpty())
                .andExpect(jsonPath("$.highlights[0].text").isNotEmpty())
                .andExpect(jsonPath("$.highlights[0].value").doesNotExist());
    }

    @Test
    void reposDisabledWithoutUsername() throws Exception {
        mvc.perform(get("/api/repos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.repos").isEmpty());
    }

    @Test
    void spaRoutesForwardButAssetsDoNot() throws Exception {
        mvc.perform(get("/experience")).andExpect(status().isOk());
        mvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void servesCvPdf() throws Exception {
        mvc.perform(get("/api/cv"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType("application/pdf"));
    }
}
