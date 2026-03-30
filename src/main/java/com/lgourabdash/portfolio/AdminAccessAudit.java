package com.lgourabdash.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_access_audit")
public class AdminAccessAudit {

    public static final String EVENT_ACCESS_CHECK = "ACCESS_CHECK";
    public static final String EVENT_DEVICE_REGISTERED = "DEVICE_REGISTERED";
    public static final String EVENT_DEVICE_UPDATED = "DEVICE_UPDATED";
    public static final String EVENT_DEVICE_DELETED = "DEVICE_DELETED";
    public static final String EVENT_DEVICE_SUSPENDED = "DEVICE_SUSPENDED";
    public static final String EVENT_DEVICE_RESUMED = "DEVICE_RESUMED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 320)
    private String firebaseEmail;

    @Column(length = 64)
    private String clientIp;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(length = 500)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getFirebaseEmail() {
        return firebaseEmail;
    }

    public void setFirebaseEmail(String firebaseEmail) {
        this.firebaseEmail = firebaseEmail;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
