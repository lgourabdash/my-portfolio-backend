package com.lgourabdash.portfolio;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * When {@code portfolio.admin.allowed-emails} is non-empty, admin APIs require a valid Firebase ID
 * token whose email is in this set (see {@link AdminClientIpFilter}). When empty, legacy IP
 * allowlist applies.
 */
@Service
public class AdminAllowedEmailService {

    private final Set<String> allowedLowercase;

    public AdminAllowedEmailService(
            @Value("${portfolio.admin.allowed-emails:}") String allowedCsv) {
        if (allowedCsv == null || allowedCsv.isBlank()) {
            allowedLowercase = Collections.emptySet();
        } else {
            allowedLowercase =
                    Arrays.stream(allowedCsv.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.toLowerCase(Locale.ROOT))
                            .collect(Collectors.toUnmodifiableSet());
        }
    }

    public boolean isFirebaseEmailMode() {
        return !allowedLowercase.isEmpty();
    }

    public boolean isAllowedEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return allowedLowercase.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
