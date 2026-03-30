package com.lgourabdash.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioSectionService {

    private static final java.util.regex.Pattern KEY_PATTERN =
            java.util.regex.Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    /** Short TTL avoids hammering MySQL when many components hit /api/public/sections. */
    private static final long SECTION_CACHE_TTL_MS = 8000;

    private final PortfolioSectionRepository repository;
    private final ObjectMapper objectMapper;

    private final Object sectionCacheLock = new Object();
    private Map<String, JsonNode> sectionCache;
    private long sectionCacheExpiresAtMs;

    public PortfolioSectionService(
            PortfolioSectionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Map<String, JsonNode> getAllAsMap() {
        synchronized (sectionCacheLock) {
            long now = System.currentTimeMillis();
            if (sectionCache != null && now < sectionCacheExpiresAtMs) {
                return new LinkedHashMap<>(sectionCache);
            }
            List<PortfolioSection> all = repository.findAll();
            Map<String, JsonNode> out = new LinkedHashMap<>();
            for (PortfolioSection row : all) {
                try {
                    out.put(row.getSectionKey(), objectMapper.readTree(row.getPayload()));
                } catch (Exception e) {
                    out.put(row.getSectionKey(), objectMapper.createObjectNode());
                }
            }
            sectionCache = out;
            sectionCacheExpiresAtMs = now + SECTION_CACHE_TTL_MS;
            return new LinkedHashMap<>(out);
        }
    }

    private void invalidateSectionCache() {
        synchronized (sectionCacheLock) {
            sectionCache = null;
        }
    }

    @Transactional
    public void upsertSection(String key, JsonNode payload) throws Exception {
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid section key");
        }
        String json = objectMapper.writeValueAsString(payload);
        PortfolioSection row =
                repository
                        .findBySectionKey(key)
                        .orElseGet(
                                () -> {
                                    PortfolioSection p = new PortfolioSection();
                                    p.setSectionKey(key);
                                    return p;
                                });
        row.setPayload(json);
        repository.save(row);
        invalidateSectionCache();
    }
}
