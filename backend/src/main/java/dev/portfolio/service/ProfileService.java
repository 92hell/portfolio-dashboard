package dev.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.config.PortfolioProperties;
import dev.portfolio.model.Profile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final PortfolioProperties props;

    public ProfileService(ResourceLoader resourceLoader, ObjectMapper objectMapper, PortfolioProperties props) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /** Re-read on every call so edits to an external file:./portfolio.json show up without a restart. */
    public Profile getProfile() {
        Resource resource = resourceLoader.getResource(props.profileLocation());
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, Profile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read profile from " + props.profileLocation(), e);
        }
    }

    public Resource getCv() {
        if (props.cvLocation() == null || props.cvLocation().isBlank()) {
            return null;
        }
        Resource cv = resourceLoader.getResource(props.cvLocation());
        return cv.exists() ? cv : null;
    }
}
