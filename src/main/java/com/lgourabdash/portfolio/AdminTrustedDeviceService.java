package com.lgourabdash.portfolio;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminTrustedDeviceService {

    private final AdminTrustedDeviceRepository deviceRepository;
    private final AdminClientIpAllowlistService ipAllowlistService;

    public AdminTrustedDeviceService(
            AdminTrustedDeviceRepository deviceRepository,
            AdminClientIpAllowlistService ipAllowlistService) {
        this.deviceRepository = deviceRepository;
        this.ipAllowlistService = ipAllowlistService;
    }

    public List<AdminTrustedDevice> listForActor(Optional<String> firebaseActorEmail) {
        if (firebaseActorEmail.isPresent()) {
            return deviceRepository.findByFirebaseEmailOrderByCreatedAtDesc(firebaseActorEmail.get());
        }
        return deviceRepository.findAllByOrderByCreatedAtDesc();
    }

    private boolean canModify(Optional<String> firebaseActorEmail, AdminTrustedDevice device) {
        if (firebaseActorEmail.isEmpty()) {
            return true;
        }
        return firebaseActorEmail.get().equalsIgnoreCase(device.getFirebaseEmail());
    }

    @Transactional
    public AdminTrustedDevice registerFromFirebaseSession(
            HttpServletRequest request, String firebaseEmail, String deviceLabel) {
        String label = normalizeLabel(deviceLabel);
        AdminTrustedDevice d = new AdminTrustedDevice();
        d.setFirebaseEmail(normalizeEmail(firebaseEmail));
        d.setDeviceLabel(label);
        d.setClientIp(truncate(ipAllowlistService.resolveClientIp(request), 64));
        d.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        d.setSourceType(AdminTrustedDevice.SOURCE_FIREBASE);
        d.setSuspendedUntil(null);
        return deviceRepository.save(d);
    }

    @Transactional
    public AdminTrustedDevice registerFromIpForm(
            HttpServletRequest request, String deviceLabel, String clientIp, String ownerEmail) {
        if (!StringUtils.hasText(ownerEmail)) {
            throw new IllegalArgumentException("ownerEmail required for IP-based registration");
        }
        validateIp(clientIp);
        String label = normalizeLabel(deviceLabel);
        AdminTrustedDevice d = new AdminTrustedDevice();
        d.setFirebaseEmail(normalizeEmail(ownerEmail));
        d.setDeviceLabel(label);
        d.setClientIp(truncate(clientIp.trim(), 64));
        d.setUserAgent("(IP registry)");
        d.setSourceType(AdminTrustedDevice.SOURCE_IP);
        d.setSuspendedUntil(null);
        return deviceRepository.save(d);
    }

    @Transactional
    public Optional<AdminTrustedDevice> update(
            long id,
            Optional<String> firebaseActorEmail,
            String deviceLabel,
            String clientIp,
            String ownerEmail) {
        return deviceRepository
                .findById(id)
                .filter(d -> canModify(firebaseActorEmail, d))
                .map(
                        d -> {
                            if (StringUtils.hasText(deviceLabel)) {
                                d.setDeviceLabel(normalizeLabel(deviceLabel));
                            }
                            if (StringUtils.hasText(clientIp)) {
                                validateIp(clientIp);
                                d.setClientIp(truncate(clientIp.trim(), 64));
                            }
                            if (StringUtils.hasText(ownerEmail)) {
                                d.setFirebaseEmail(normalizeEmail(ownerEmail));
                            }
                            return deviceRepository.save(d);
                        });
    }

    @Transactional
    public boolean suspend(
            long id, Optional<String> firebaseActorEmail, Instant suspendedUntil) {
        if (suspendedUntil == null || !suspendedUntil.isAfter(Instant.now())) {
            throw new IllegalArgumentException("suspendedUntil must be in the future");
        }
        return deviceRepository
                .findById(id)
                .filter(d -> canModify(firebaseActorEmail, d))
                .map(
                        d -> {
                            d.setSuspendedUntil(suspendedUntil);
                            deviceRepository.save(d);
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public boolean resume(long id, Optional<String> firebaseActorEmail) {
        return deviceRepository
                .findById(id)
                .filter(d -> canModify(firebaseActorEmail, d))
                .map(
                        d -> {
                            d.setSuspendedUntil(null);
                            deviceRepository.save(d);
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public boolean deleteIfAllowed(long id, Optional<String> firebaseActorEmail) {
        return deviceRepository
                .findById(id)
                .filter(d -> canModify(firebaseActorEmail, d))
                .map(
                        d -> {
                            deviceRepository.delete(d);
                            return true;
                        })
                .orElse(false);
    }

    public static Instant suspendUntilPresetDays(int days) {
        if (days <= 0 || days > 366) {
            throw new IllegalArgumentException("days must be 1–366");
        }
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    private static String normalizeLabel(String deviceLabel) {
        String label = deviceLabel == null ? "" : deviceLabel.trim();
        if (label.isEmpty()) {
            label = "Unnamed device";
        }
        return label.length() > 200 ? label.substring(0, 200) : label;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            throw new IllegalArgumentException("IP address required");
        }
        String s = ip.trim();
        try {
            InetAddress.getByName(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address: " + s);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
