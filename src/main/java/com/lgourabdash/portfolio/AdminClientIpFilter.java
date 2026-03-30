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
 * Blocks /api/admin/** when the client IP is not on the allowlist. OPTIONS is
 * passed through for CORS preflight.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class AdminClientIpFilter extends OncePerRequestFilter {

    private final AdminClientIpAllowlistService allowlistService;
    private final ObjectMapper objectMapper;
    private final String deniedMessage;

    public AdminClientIpFilter(
            AdminClientIpAllowlistService allowlistService,
            ObjectMapper objectMapper,
            @Value(
                    "${portfolio.admin.access-denied-message:This admin area is only accessible from L. Gourab Dash's authorized laptop. Please go back.}")
                    String deniedMessage) {
        this.allowlistService = allowlistService;
        this.objectMapper = objectMapper;
        this.deniedMessage = deniedMessage;
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
        if (!allowlistService.isAllowed(request)) {
            writeForbidden(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mirrorCorsOrigin(request, response);
        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "allowed", false,
                                "message", deniedMessage));
        response.getWriter().write(body);
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
