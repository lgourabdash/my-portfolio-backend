package com.lgourabdash.portfolio;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "portfolio_attachment")
public class PortfolioAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    /**
     * CERTIFICATE — Experience; EDUCATION_DOC — Education; LETTER — gallery;<br>
     * RESUME / PROFILE_PHOTO — versioned assets; only one {@code isActive} per kind per user.
     */
    @Column(nullable = false, length = 32)
    private String kind;

    @Column(nullable = false, length = 255)
    private String title;

    /**
     * For certificates: use the same text as the internship "company" field for auto-linking.
     */
    @Column(name = "related_key", length = 512)
    private String relatedKey;

    /**
     * Hibernate 6 + MySQL: map as BLOB explicitly. {@code @Lob} alone can mis-map and cause
     * errors when materializing bytes for download (HTTP 500 in the SPA viewer).
     */
    @JdbcTypeCode(SqlTypes.BLOB)
    @Column(name = "data", columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = Boolean.FALSE;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(Long userProfileId) {
        this.userProfileId = userProfileId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRelatedKey() {
        return relatedKey;
    }

    public void setRelatedKey(String relatedKey) {
        this.relatedKey = relatedKey;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
