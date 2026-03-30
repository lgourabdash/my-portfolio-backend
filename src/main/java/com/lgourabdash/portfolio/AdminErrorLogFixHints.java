package com.lgourabdash.portfolio;

import org.springframework.util.StringUtils;

/** Heuristic “how to fix” text for the admin dashboard (not a substitute for real monitoring). */
public final class AdminErrorLogFixHints {

    private AdminErrorLogFixHints() {}

    public static String forFrontend(String message, String code, String path) {
        String m = safe(message).toLowerCase();
        String c = safe(code).toUpperCase();
        if ("CONFIG".equals(c) || m.contains("api url") || m.contains("react_app_api")) {
            return "Set REACT_APP_API_ORIGIN (e.g. http://localhost:8080) in the frontend .env file, restart npm start, and confirm the value matches your Spring Boot port.";
        }
        if ("NETWORK".equals(c) || m.contains("failed to fetch") || m.contains("network error")) {
            return "Start the Spring Boot API, check the URL in .env, and use http://localhost for dev so CORS allows the browser. Verify nothing blocks port 8080 (firewall / VPN).";
        }
        if (m.contains("401") || m.contains("unauthorized") || m.contains("invalid admin key")) {
            return "Set the same value for PORTFOLIO_ADMIN_KEY on the server and REACT_APP_PORTFOLIO_ADMIN_KEY in the frontend .env (used as header X-Admin-Key).";
        }
        if (m.contains("403") || m.contains("forbidden")) {
            return "Your IP may not be on portfolio.admin.allowed-client-ips. Add this machine’s IP in application.properties or env, or use 127.0.0.1 when testing locally.";
        }
        if (m.contains("cors")) {
            return "Allow your dev origin in WebConfig (allowedOriginPatterns) or run the React app from an allowed localhost URL.";
        }
        if (m.contains("json") && m.contains("invalid")) {
            return "Fix JSON syntax in the editor (trailing commas, quotes). Use a JSON validator or the Advanced tab to paste valid JSON.";
        }
        if (m.contains("firebase")) {
            return "Check Firebase config in the frontend, API keys, and that Authentication is enabled for Email/Password if you use it.";
        }
        if (StringUtils.hasText(path)) {
            return "Reproduce on page "
                    + path
                    + ". Open DevTools → Console for the stack trace; fix the component or API call that threw.";
        }
        return "Open the browser DevTools Console, note the file and line from the stack trace, and fix the throwing code or the failing fetch/await.";
    }

    public static String forBackend(String message, Integer status, String path) {
        String m = safe(message).toLowerCase();
        if (status != null && status == 401) {
            return "Client sent wrong or missing X-Admin-Key. Align PORTFOLIO_ADMIN_KEY with the admin panel .env.";
        }
        if (status != null && status == 403) {
            return "Forbidden — often IP allowlist. Check portfolio.admin.allowed-client-ips and proxy headers if behind nginx.";
        }
        if (status != null && status == 404) {
            return "No handler or resource for this path. Verify the URL matches a @GetMapping/@PutMapping and that path variables are correct.";
        }
        if (m.contains("could not open jpa") || m.contains("could not create connection")) {
            return "Database unreachable. Start MySQL, check spring.datasource.* in application.properties, and credentials.";
        }
        if (m.contains("sql") || m.contains("constraint") || m.contains("duplicate")) {
            return "SQL or constraint violation — inspect the failing query/entity, unique keys, and nullable columns.";
        }
        if (m.contains("json") || m.contains("parse") || m.contains("deserialize")) {
            return "Request or stored JSON is invalid. Validate payload shape against the DTO and section schema.";
        }
        if (m.contains("illegalargument") || m.contains("invalid")) {
            return "Validation failed in service layer — read the message, fix input or business rules, add guards for null/empty.";
        }
        if (m.contains("outofmemory") || m.contains("heap")) {
            return "Increase JVM heap or fix a leak (large uploads, unclosed streams). Review attachment sizes.";
        }
        if (StringUtils.hasText(path)) {
            return "Check server logs for full stack trace. Reproduce the request to "
                    + path
                    + " and fix the controller/service method indicated at the top of the stack.";
        }
        return "Read the stack trace in the Spring Boot console (root cause at Caused by). Fix the indicated class/method; add tests around the failing path.";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
