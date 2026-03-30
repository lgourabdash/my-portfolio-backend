package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * After {@link AdminClientIpFilter} verified a Firebase admin, mutating admin routes do not
     * require {@code X-Admin-Key}.
     */
    public boolean authorize(HttpServletRequest request, String headerValue) {
        if (request != null
                && request.getAttribute(AdminRequestAttributes.FIREBASE_EMAIL) != null) {
            return true;
        }
        return authorize(headerValue);
    }
}
