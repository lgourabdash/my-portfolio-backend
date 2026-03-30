package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAccessAuditRepository auditRepository;
    private final AdminClientIpAllowlistService ipAllowlistService;

    public AdminAuditService(
            AdminAccessAuditRepository auditRepository,
            AdminClientIpAllowlistService ipAllowlistService) {
        this.auditRepository = auditRepository;
        this.ipAllowlistService = ipAllowlistService;
    }

    @Transactional
    public void log(
            HttpServletRequest request,
            String firebaseEmail,
            String eventType,
            String detail) {
        AdminAccessAudit row = new AdminAccessAudit();
        row.setFirebaseEmail(firebaseEmail);
        row.setClientIp(truncate(ipAllowlistService.resolveClientIp(request), 64));
        row.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        row.setEventType(eventType);
        row.setDetail(truncate(detail, 500));
        auditRepository.save(row);
    }

    public java.util.List<AdminAccessAudit> recentForEmail(String email, int limit) {
        int n = Math.min(Math.max(limit, 1), 200);
        return auditRepository.findByFirebaseEmailOrderByCreatedAtDesc(
                email, PageRequest.of(0, n));
    }

    public java.util.List<AdminAccessAudit> recentAll(int limit) {
        int n = Math.min(Math.max(limit, 1), 200);
        return auditRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, n));
    }

    @Transactional
    public void logOptionalEmail(
            HttpServletRequest request,
            java.util.Optional<String> firebaseEmail,
            String eventType,
            String detail) {
        AdminAccessAudit row = new AdminAccessAudit();
        row.setFirebaseEmail(firebaseEmail.orElse(null));
        row.setClientIp(truncate(ipAllowlistService.resolveClientIp(request), 64));
        row.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        row.setEventType(eventType);
        row.setDetail(truncate(detail, 500));
        auditRepository.save(row);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public Optional<String> currentFirebaseEmail(HttpServletRequest request) {
        Object v = request.getAttribute(AdminRequestAttributes.FIREBASE_EMAIL);
        if (v instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }
}
