package com.lgourabdash.portfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminKeyService {

    @Value("${portfolio.admin.api-key:}")
    private String apiKey;

    /**
     * When no key is configured, PUT is allowed (local dev only). Set
     * PORTFOLIO_ADMIN_KEY / portfolio.admin.api-key in production.
     */
    public boolean authorize(String headerValue) {
        if (apiKey == null || apiKey.isBlank()) {
            return true;
        }
        return apiKey.equals(headerValue);
    }
}
