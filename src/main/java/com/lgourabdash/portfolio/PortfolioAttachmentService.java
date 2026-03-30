package com.lgourabdash.portfolio;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PortfolioAttachmentService {

    public static final String KIND_CERTIFICATE = "CERTIFICATE";
    public static final String KIND_EDUCATION_DOC = "EDUCATION_DOC";
    /** Gallery assets for the public Certificates section (images/PDFs). */
    public static final String KIND_LETTER = "LETTER";
    public static final String KIND_RESUME = "RESUME";
    public static final String KIND_PROFILE_PHOTO = "PROFILE_PHOTO";

    /** Bytes + metadata loaded inside one transaction (LOB must not be read after session closes). */
    public record AttachmentDownload(byte[] bytes, String contentType, String fileName, String kind) {}

    private static final long LIST_CACHE_TTL_MS = 8000;

    private final PortfolioAttachmentRepository repository;

    private static final class CachedList {
        final List<PortfolioAttachmentDto> dtos;
        final long expiresAtMs;

        CachedList(List<PortfolioAttachmentDto> dtos, long expiresAtMs) {
            this.dtos = dtos;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private final ConcurrentHashMap<String, CachedList> listCache = new ConcurrentHashMap<>();

    public PortfolioAttachmentService(PortfolioAttachmentRepository repository) {
        this.repository = repository;
    }

    public List<PortfolioAttachmentDto> list(Long userProfileId, String kind) {
        String cacheKey =
                userProfileId
                        + ":"
                        + (kind == null || kind.isBlank() ? "*" : kind.trim().toUpperCase());
        long now = System.currentTimeMillis();
        CachedList hit = listCache.get(cacheKey);
        if (hit != null && now < hit.expiresAtMs) {
            return List.copyOf(hit.dtos);
        }
        List<PortfolioAttachment> rows = (kind == null || kind.isBlank())
                ? repository.findByUserProfileIdOrderByIdDesc(userProfileId)
                : repository.findByUserProfileIdAndKindOrderByIdDesc(
                        userProfileId, kind.trim().toUpperCase());
        List<PortfolioAttachmentDto> dtos = rows.stream().map(this::toDto).toList();
        listCache.put(cacheKey, new CachedList(dtos, now + LIST_CACHE_TTL_MS));
        return dtos;
    }

    private void invalidateListCachesForUser(Long userProfileId) {
        String prefix = userProfileId + ":";
        listCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public PortfolioAttachment save(
            Long userProfileId,
            String kind,
            String title,
            String relatedKey,
            MultipartFile file
    ) throws IOException {
        String k = kind == null ? "" : kind.trim().toUpperCase();
        if (!KIND_CERTIFICATE.equals(k)
                && !KIND_EDUCATION_DOC.equals(k)
                && !KIND_LETTER.equals(k)
                && !KIND_RESUME.equals(k)
                && !KIND_PROFILE_PHOTO.equals(k)) {
            throw new IllegalArgumentException(
                    "kind must be CERTIFICATE, EDUCATION_DOC, LETTER, RESUME, or PROFILE_PHOTO");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (KIND_RESUME.equals(k)) {
            String fn = file.getOriginalFilename();
            if (fn == null || !fn.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new IllegalArgumentException("Only PDF files are allowed for RESUME");
            }
        }
        PortfolioAttachment a = new PortfolioAttachment();
        a.setUserProfileId(userProfileId);
        a.setKind(k);
        a.setTitle(title.trim());
        a.setRelatedKey(relatedKey == null ? null : relatedKey.trim());
        a.setData(file.getBytes());
        a.setFileName(file.getOriginalFilename());
        String ct = file.getContentType();
        a.setContentType(ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (KIND_RESUME.equals(k) || KIND_PROFILE_PHOTO.equals(k)) {
            long n = repository.countByUserProfileIdAndKind(userProfileId, k);
            a.setIsActive(n == 0);
        } else {
            a.setIsActive(Boolean.FALSE);
        }
        PortfolioAttachment saved = repository.save(a);
        invalidateListCachesForUser(userProfileId);
        return saved;
    }

    public PortfolioAttachment getOwned(Long attachmentId, Long userProfileId) {
        PortfolioAttachment a = repository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (!a.getUserProfileId().equals(userProfileId)) {
            throw new IllegalArgumentException("Attachment not found");
        }
        return a;
    }

    /**
     * Active RESUME or PROFILE_PHOTO for public /resume and /photo endpoints.
     */
    @Transactional(readOnly = true)
    public Optional<AttachmentDownload> fetchActiveAssetDownload(Long userProfileId, String kind) {
        String kk = kind == null ? "" : kind.trim().toUpperCase();
        if (!KIND_RESUME.equals(kk) && !KIND_PROFILE_PHOTO.equals(kk)) {
            return Optional.empty();
        }
        Optional<PortfolioAttachment> opt =
                repository.findFirstByUserProfileIdAndKindAndIsActiveTrueOrderByIdDesc(
                        userProfileId, kk);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        PortfolioAttachment a = opt.get();
        byte[] raw = a.getData();
        byte[] bytes =
                raw != null && raw.length > 0 ? Arrays.copyOf(raw, raw.length) : new byte[0];
        return Optional.of(
                new AttachmentDownload(bytes, a.getContentType(), a.getFileName(), a.getKind()));
    }

    /**
     * Loads file bytes while the persistence context is open. Required for {@code @Lob} / LONGBLOB
     * fields so the SPA fetch does not trigger LazyInitializationException (HTTP 500).
     */
    @Transactional(readOnly = true)
    public AttachmentDownload fetchForHttpDownload(Long attachmentId, Long userProfileId) {
        PortfolioAttachment a =
                repository
                        .findByIdAndUserProfileId(attachmentId, userProfileId)
                        .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        byte[] raw = a.getData();
        /* Copy so the response does not hold a reference to persistence-managed state. */
        byte[] bytes =
                raw != null && raw.length > 0 ? Arrays.copyOf(raw, raw.length) : new byte[0];
        return new AttachmentDownload(bytes, a.getContentType(), a.getFileName(), a.getKind());
    }

    @Transactional
    public void delete(Long userProfileId, Long attachmentId) {
        PortfolioAttachment a = getOwned(attachmentId, userProfileId);
        String kind = a.getKind();
        boolean wasActive = Boolean.TRUE.equals(a.getIsActive());
        repository.delete(a);
        invalidateListCachesForUser(userProfileId);
        if (wasActive && (KIND_RESUME.equals(kind) || KIND_PROFILE_PHOTO.equals(kind))) {
            promoteNewestToActiveIfNone(userProfileId, kind);
        }
    }

    private void promoteNewestToActiveIfNone(Long userProfileId, String kind) {
        if (repository.existsByUserProfileIdAndKindAndIsActiveTrue(userProfileId, kind)) {
            return;
        }
        List<PortfolioAttachment> rest =
                repository.findByUserProfileIdAndKindOrderByIdDesc(userProfileId, kind);
        if (rest.isEmpty()) {
            return;
        }
        PortfolioAttachment pick = rest.get(0);
        pick.setIsActive(Boolean.TRUE);
        repository.save(pick);
        invalidateListCachesForUser(userProfileId);
    }

    @Transactional
    public void setActiveAsset(Long userProfileId, Long attachmentId) {
        PortfolioAttachment target = getOwned(attachmentId, userProfileId);
        String k = target.getKind();
        if (!KIND_RESUME.equals(k) && !KIND_PROFILE_PHOTO.equals(k)) {
            throw new IllegalArgumentException("Only RESUME or PROFILE_PHOTO can be set active");
        }
        List<PortfolioAttachment> all = repository.findByUserProfileIdAndKind(userProfileId, k);
        boolean found = false;
        for (PortfolioAttachment x : all) {
            boolean active = x.getId().equals(attachmentId);
            if (active) {
                found = true;
            }
            x.setIsActive(active);
        }
        if (!found) {
            throw new IllegalArgumentException("Attachment not found");
        }
        repository.saveAll(all);
        invalidateListCachesForUser(userProfileId);
    }

    /**
     * Replace file and/or metadata. Omit or leave {@code file} empty to keep existing bytes.
     */
    @Transactional
    public void update(
            Long userProfileId,
            Long attachmentId,
            String title,
            String relatedKey,
            MultipartFile file)
            throws IOException {
        PortfolioAttachment a = getOwned(attachmentId, userProfileId);
        if (title != null && !title.isBlank()) {
            a.setTitle(title.trim());
        }
        if (relatedKey != null) {
            String rk = relatedKey.trim();
            a.setRelatedKey(rk.isEmpty() ? null : rk);
        }
        if (file != null && !file.isEmpty()) {
            a.setData(file.getBytes());
            a.setFileName(file.getOriginalFilename());
            String ct = file.getContentType();
            a.setContentType(ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
        repository.save(a);
        invalidateListCachesForUser(userProfileId);
    }

    private PortfolioAttachmentDto toDto(PortfolioAttachment a) {
        return new PortfolioAttachmentDto(
                a.getId(),
                a.getKind(),
                a.getTitle(),
                a.getRelatedKey(),
                a.getFileName(),
                a.getContentType(),
                a.getCreatedAt(),
                Boolean.TRUE.equals(a.getIsActive()));
    }
}
