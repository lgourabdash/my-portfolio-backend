package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminTrustedDeviceController {

    private final AdminTrustedDeviceService deviceService;
    private final AdminAuditService auditService;
    private final AdminAllowedEmailService allowedEmailService;

    public AdminTrustedDeviceController(
            AdminTrustedDeviceService deviceService,
            AdminAuditService auditService,
            AdminAllowedEmailService allowedEmailService) {
        this.deviceService = deviceService;
        this.auditService = auditService;
        this.allowedEmailService = allowedEmailService;
    }

    /** Firebase-authenticated admin, or IP allowlist when server is not in Firebase-email mode. */
    private boolean canAccessTrustedDevices(HttpServletRequest request) {
        if (auditService.currentFirebaseEmail(request).isPresent()) {
            return true;
        }
        return !allowedEmailService.isFirebaseEmailMode();
    }

    private ResponseEntity<?> forbiddenRegistry() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Not allowed to use device registry for this session."));
    }

    @GetMapping("/api/admin/trusted-devices")
    public ResponseEntity<?> list(HttpServletRequest request) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        Optional<String> actor = auditService.currentFirebaseEmail(request);
        List<Map<String, Object>> out =
                deviceService.listForActor(actor).stream()
                        .map(this::toJson)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    public record RegisterBody(String addMode, String deviceLabel, String clientIp, String ownerEmail) {}

    @PostMapping("/api/admin/trusted-devices")
    public ResponseEntity<?> register(
            HttpServletRequest request, @RequestBody(required = false) RegisterBody body) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        Optional<String> fb = auditService.currentFirebaseEmail(request);
        RegisterBody b = body != null ? body : new RegisterBody(null, null, null, null);
        String mode = b.addMode() == null ? "" : b.addMode().trim();
        boolean ipMode = "IP".equalsIgnoreCase(mode) || "IP_MANUAL".equalsIgnoreCase(mode);

        try {
            AdminTrustedDevice saved;
            if (fb.isPresent() && !ipMode) {
                saved =
                        deviceService.registerFromFirebaseSession(
                                request, fb.get(), b.deviceLabel());
                auditService.logOptionalEmail(
                        request,
                        fb,
                        AdminAccessAudit.EVENT_DEVICE_REGISTERED,
                        "deviceId="
                                + saved.getId()
                                + " label="
                                + saved.getDeviceLabel()
                                + " source=FIREBASE");
            } else {
                saved =
                        deviceService.registerFromIpForm(
                                request, b.deviceLabel(), b.clientIp(), b.ownerEmail());
                auditService.logOptionalEmail(
                        request,
                        fb,
                        AdminAccessAudit.EVENT_DEVICE_REGISTERED,
                        "deviceId="
                                + saved.getId()
                                + " label="
                                + saved.getDeviceLabel()
                                + " source=IP owner="
                                + saved.getFirebaseEmail());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(toJson(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    public record UpdateBody(String deviceLabel, String clientIp, String ownerEmail) {}

    @PatchMapping("/api/admin/trusted-devices/{id}")
    public ResponseEntity<?> update(
            HttpServletRequest request,
            @PathVariable long id,
            @RequestBody(required = false) UpdateBody body) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        UpdateBody b = body != null ? body : new UpdateBody(null, null, null);
        Optional<AdminTrustedDevice> updated =
                deviceService.update(
                        id,
                        auditService.currentFirebaseEmail(request),
                        b.deviceLabel(),
                        b.clientIp(),
                        b.ownerEmail());
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        auditService.logOptionalEmail(
                request,
                auditService.currentFirebaseEmail(request),
                AdminAccessAudit.EVENT_DEVICE_UPDATED,
                "deviceId=" + id);
        return ResponseEntity.ok(toJson(updated.get()));
    }

    public record SuspendBody(String preset) {}

    @PostMapping("/api/admin/trusted-devices/{id}/suspend")
    public ResponseEntity<?> suspend(
            HttpServletRequest request,
            @PathVariable long id,
            @RequestBody(required = false) SuspendBody body) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        String preset = body != null && body.preset() != null ? body.preset().trim() : "";
        Instant until;
        if ("7d".equalsIgnoreCase(preset) || "P7D".equalsIgnoreCase(preset)) {
            until = Instant.now().plus(7, ChronoUnit.DAYS);
        } else if ("30d".equalsIgnoreCase(preset)
                || "1m".equalsIgnoreCase(preset)
                || "P30D".equalsIgnoreCase(preset)) {
            until = Instant.now().plus(30, ChronoUnit.DAYS);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "preset must be \"7d\" or \"30d\""));
        }
        try {
            if (!deviceService.suspend(
                    id, auditService.currentFirebaseEmail(request), until)) {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        auditService.logOptionalEmail(
                request,
                auditService.currentFirebaseEmail(request),
                AdminAccessAudit.EVENT_DEVICE_SUSPENDED,
                "deviceId=" + id + " until=" + until);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/trusted-devices/{id}/resume")
    public ResponseEntity<?> resume(HttpServletRequest request, @PathVariable long id) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        if (!deviceService.resume(id, auditService.currentFirebaseEmail(request))) {
            return ResponseEntity.notFound().build();
        }
        auditService.logOptionalEmail(
                request,
                auditService.currentFirebaseEmail(request),
                AdminAccessAudit.EVENT_DEVICE_RESUMED,
                "deviceId=" + id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/trusted-devices/{id}")
    public ResponseEntity<?> delete(HttpServletRequest request, @PathVariable long id) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        if (!deviceService.deleteIfAllowed(id, auditService.currentFirebaseEmail(request))) {
            return ResponseEntity.notFound().build();
        }
        auditService.logOptionalEmail(
                request,
                auditService.currentFirebaseEmail(request),
                AdminAccessAudit.EVENT_DEVICE_DELETED,
                "deviceId=" + id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/access-audit")
    public ResponseEntity<?> audit(
            HttpServletRequest request,
            @RequestParam(defaultValue = "40") int limit) {
        if (!canAccessTrustedDevices(request)) {
            return forbiddenRegistry();
        }
        List<AdminAccessAudit> rows;
        Optional<String> email = auditService.currentFirebaseEmail(request);
        if (email.isPresent()) {
            rows = auditService.recentForEmail(email.get(), limit);
        } else {
            rows = auditService.recentAll(limit);
        }
        List<Map<String, Object>> out =
                rows.stream().map(this::auditToJson).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> toJson(AdminTrustedDevice d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("firebaseEmail", d.getFirebaseEmail());
        m.put("ownerEmail", d.getFirebaseEmail());
        m.put("deviceLabel", StringUtils.hasText(d.getDeviceLabel()) ? d.getDeviceLabel() : "");
        m.put("clientIp", d.getClientIp() != null ? d.getClientIp() : "");
        m.put("userAgent", d.getUserAgent() != null ? d.getUserAgent() : "");
        m.put(
                "sourceType",
                d.getSourceType() != null ? d.getSourceType() : AdminTrustedDevice.SOURCE_FIREBASE);
        m.put("createdAt", d.getCreatedAt().toString());
        m.put(
                "updatedAt",
                d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : d.getCreatedAt().toString());
        Instant su = d.getSuspendedUntil();
        m.put("suspendedUntil", su != null ? su.toString() : null);
        m.put("suspended", d.isSuspendedNow());
        return m;
    }

    private Map<String, Object> auditToJson(AdminAccessAudit a) {
        return Map.of(
                "id", a.getId(),
                "firebaseEmail", a.getFirebaseEmail() != null ? a.getFirebaseEmail() : "",
                "clientIp", a.getClientIp() != null ? a.getClientIp() : "",
                "userAgent", a.getUserAgent() != null ? a.getUserAgent() : "",
                "eventType", a.getEventType(),
                "detail", a.getDetail() != null ? a.getDetail() : "",
                "createdAt", a.getCreatedAt().toString());
    }
}
