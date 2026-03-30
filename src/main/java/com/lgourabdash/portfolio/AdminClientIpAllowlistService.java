package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Restricts /api/admin/** to clients whose IP is listed in
 * {@code portfolio.admin.allowed-client-ips}. Add your laptop's LAN or public IP
 * when you deploy; keep 127.0.0.1 and ::1 for local development.
 */
@Service
public class AdminClientIpAllowlistService {

    private final Set<String> allowedNormalized;
    private final boolean trustProxyHeaders;

    public AdminClientIpAllowlistService(
            @Value("${portfolio.admin.allowed-client-ips:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String allowedCsv,
            @Value("${portfolio.admin.trust-proxy-headers:false}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
        this.allowedNormalized =
                Arrays.stream(allowedCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(this::normalizeIp)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isAllowed(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String n = normalizeIp(ip);
        return allowedNormalized.contains(n);
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String first = xff.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return stripZone(first);
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return stripZone(realIp.trim());
            }
        }
        String remote = request.getRemoteAddr();
        return stripZone(remote != null ? remote : "");
    }

    private String stripZone(String ip) {
        if (ip == null) {
            return "";
        }
        int pct = ip.indexOf('%');
        if (pct > 0 && ip.contains(":")) {
            return ip.substring(0, pct);
        }
        return ip;
    }

    /** Normalize for comparison (loopback, IPv4-mapped IPv6 → IPv4). */
    public String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        String s = stripZone(ip.trim());
        if (s.startsWith("/") && s.length() > 1) {
            s = s.substring(1);
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("::ffff:")) {
            return lower.substring("::ffff:".length());
        }
        if (lower.startsWith("0:0:0:0:0:ffff:")) {
            return lower.substring("0:0:0:0:0:ffff:".length());
        }
        try {
            InetAddress addr = InetAddress.getByName(s);
            if (addr.isLoopbackAddress()) {
                return addr instanceof Inet6Address ? "::1" : "127.0.0.1";
            }
            return addr.getHostAddress();
        } catch (Exception e) {
            return lower;
        }
    }
}
