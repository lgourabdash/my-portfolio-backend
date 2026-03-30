package com.lgourabdash.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_trusted_device")
public class AdminTrustedDevice {

    public static final String SOURCE_FIREBASE = "FIREBASE";
    public static final String SOURCE_IP = "IP_MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner/admin email (Firebase user or manually set for IP-based rows). */
    @Column(nullable = false, length = 320)
    private String firebaseEmail;

    @Column(nullable = false, length = 200)
    private String deviceLabel;

    @Column(length = 64)
    private String clientIp;

    @Column(length = 512)
    private String userAgent;

    /** FIREBASE = captured from session; IP_MANUAL = entered in IP registry UI. */
    @Column(name = "source_type", length = 32)
    private String sourceType;

    /** When non-null and in the future, device is treated as suspended. */
    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = SOURCE_FIREBASE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isSuspendedNow() {
        return suspendedUntil != null && suspendedUntil.isAfter(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getFirebaseEmail() {
        return firebaseEmail;
    }

    public void setFirebaseEmail(String firebaseEmail) {
        this.firebaseEmail = firebaseEmail;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(Instant suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
