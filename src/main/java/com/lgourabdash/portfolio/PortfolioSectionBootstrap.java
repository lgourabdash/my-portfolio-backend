package com.lgourabdash.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PortfolioSectionBootstrap implements CommandLineRunner {

    private final PortfolioSectionRepository repository;
    private final ObjectMapper objectMapper;

    public PortfolioSectionBootstrap(
            PortfolioSectionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/default-portfolio-sections.json");
        if (!resource.exists()) {
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            if (!root.isObject()) {
                return;
            }
            root.fields()
                    .forEachRemaining(
                            e -> {
                                String key = e.getKey();
                                if (repository.findBySectionKey(key).isEmpty()) {
                                    try {
                                        PortfolioSection row = new PortfolioSection();
                                        row.setSectionKey(key);
                                        row.setPayload(objectMapper.writeValueAsString(e.getValue()));
                                        repository.save(row);
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
        }
    }
}
