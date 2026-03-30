package com.lgourabdash.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>When {@code portfolio.admin.allowed-emails} is set: requires {@code Authorization: Bearer
 * &lt;Firebase ID token&gt;} and an email allowlist match (Gmail / any Firebase email sign-in).
 *
 * <p>When allowed emails are empty (local dev): legacy IP allowlist only.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class AdminClientIpFilter extends OncePerRequestFilter {

    private final AdminClientIpAllowlistService allowlistService;
    private final AdminAllowedEmailService allowedEmailService;
    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final ObjectMapper objectMapper;
    private final String deniedMessage;
    private final String firebaseDeniedMessage;

    public AdminClientIpFilter(
            AdminClientIpAllowlistService allowlistService,
            AdminAllowedEmailService allowedEmailService,
            FirebaseTokenVerifier firebaseTokenVerifier,
            ObjectMapper objectMapper,
            @Value(
                    "${portfolio.admin.access-denied-message:This admin area is only accessible from L. Gourab Dash's authorized laptop. Please go back.}")
                    String deniedMessage,
            @Value(
                    "${portfolio.admin.firebase-denied-message:Sign in with an authorized admin account (allowed email) and retry.}")
                    String firebaseDeniedMessage) {
        this.allowlistService = allowlistService;
        this.allowedEmailService = allowedEmailService;
        this.firebaseTokenVerifier = firebaseTokenVerifier;
        this.objectMapper = objectMapper;
        this.deniedMessage = deniedMessage;
        this.firebaseDeniedMessage = firebaseDeniedMessage;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !path.startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (allowedEmailService.isFirebaseEmailMode()) {
            if (!firebaseTokenVerifier.isConfigured()) {
                writeJson(
                        request,
                        response,
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        Map.of(
                                "allowed",
                                false,
                                "message",
                                "Server missing Firebase Admin credentials. Set portfolio.admin.firebase-service-account-json (or Base64)."));
                return;
            }
            String bearer = extractBearerToken(request.getHeader("Authorization"));
            if (bearer == null) {
                writeForbiddenFirebase(request, response);
                return;
            }
            var emailOpt = firebaseTokenVerifier.verifyIdToken(bearer);
            if (emailOpt.isEmpty() || !allowedEmailService.isAllowedEmail(emailOpt.get())) {
                writeForbiddenFirebase(request, response);
                return;
            }
            request.setAttribute(AdminRequestAttributes.FIREBASE_EMAIL, emailOpt.get());
            filterChain.doFilter(request, response);
            return;
        }
        if (!allowlistService.isAllowed(request)) {
            writeForbiddenIp(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String v = authorization.trim();
        if (!v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = v.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private void writeForbiddenIp(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        writeJson(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                Map.of("allowed", false, "message", deniedMessage));
    }

    private void writeForbiddenFirebase(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        writeJson(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                Map.of("allowed", false, "message", firebaseDeniedMessage));
    }

    private void writeJson(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            Map<String, Object> body)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mirrorCorsOrigin(request, response);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /** Let the browser read JSON on 403 for cross-origin access-check (no credentials). */
    private void mirrorCorsOrigin(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
    }
}
