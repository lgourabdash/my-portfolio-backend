package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin entry points: {@link AdminClientIpFilter} enforces Firebase email or IP allowlist for all
 * {@code /api/admin/**} routes except CORS preflight.
 */
@RestController
public class AdminAccessController {

    private final AdminErrorLogService errorLogService;
    private final AdminKeyService adminKeyService;
    private final AdminAuditService auditService;

    public AdminAccessController(
            AdminErrorLogService errorLogService,
            AdminKeyService adminKeyService,
            AdminAuditService auditService) {
        this.errorLogService = errorLogService;
        this.adminKeyService = adminKeyService;
        this.auditService = auditService;
    }

    @GetMapping("/api/admin/access-check")
    public ResponseEntity<Map<String, Object>> accessCheck(HttpServletRequest request) {
        Object emailAttr = request.getAttribute(AdminRequestAttributes.FIREBASE_EMAIL);
        if (emailAttr instanceof String email && !email.isBlank()) {
            auditService.log(
                    request, email, AdminAccessAudit.EVENT_ACCESS_CHECK, "access-check");
            return ResponseEntity.ok(
                    Map.of(
                            "allowed",
                            true,
                            "auth",
                            "firebase",
                            "email",
                            email,
                            "deviceNote",
                            "Signed in as " + email));
        }
        auditService.logOptionalEmail(
                request,
                Optional.empty(),
                AdminAccessAudit.EVENT_ACCESS_CHECK,
                "access-check-ip");
        return ResponseEntity.ok(
                Map.of(
                        "allowed",
                        true,
                        "auth",
                        "ip",
                        "deviceNote",
                        "IP allowlist (legacy dev / LAN)"));
    }

    @GetMapping("/api/admin/error-logs")
    public ResponseEntity<?> listErrorLogs(
            HttpServletRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!adminKeyService.authorize(request, adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
        }
        return ResponseEntity.ok(errorLogService.listNewestFirst());
    }

    @PostMapping("/api/admin/error-logs")
    public ResponseEntity<?> reportErrorLog(
            HttpServletRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody(required = false) AdminErrorLogReportRequest body) {
        if (!adminKeyService.authorize(request, adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
        }
        if (body == null || !StringUtils.hasText(body.getMessage())) {
            return ResponseEntity.badRequest().body("message required");
        }
        String src = body.getSource() == null ? "FRONTEND" : body.getSource().trim().toUpperCase();
        if (!"FRONTEND".equals(src)) {
            return ResponseEntity.badRequest().body("source must be FRONTEND");
        }
        errorLogService.logFrontendReport(
                body.getMessage(),
                body.getDetail(),
                body.getPath(),
                body.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/admin/error-logs")
    public ResponseEntity<?> clearErrorLogs(
            HttpServletRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!adminKeyService.authorize(request, adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
        }
        errorLogService.clear();
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/error-logs/{id}/close")
    public ResponseEntity<?> closeErrorLogPatch(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        return closeErrorLogInternal(request, id, adminKey);
    }

    /**
     * Same as PATCH close — some proxies/firewalls block PATCH; the admin UI tries POST as a
     * fallback.
     */
    @PostMapping("/api/admin/error-logs/{id}/close")
    public ResponseEntity<?> closeErrorLogPost(
            HttpServletRequest request,
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        return closeErrorLogInternal(request, id, adminKey);
    }

    private ResponseEntity<?> closeErrorLogInternal(
            HttpServletRequest request, String id, String adminKey) {
        if (!adminKeyService.authorize(request, adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
        }
        if (!errorLogService.closeById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
