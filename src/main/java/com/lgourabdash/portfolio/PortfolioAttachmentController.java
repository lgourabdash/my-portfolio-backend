package com.lgourabdash.portfolio;

import static com.lgourabdash.portfolio.PortfolioAttachmentService.KIND_CERTIFICATE;
import static com.lgourabdash.portfolio.PortfolioAttachmentService.KIND_EDUCATION_DOC;
import static com.lgourabdash.portfolio.PortfolioAttachmentService.KIND_LETTER;
import static com.lgourabdash.portfolio.PortfolioAttachmentService.KIND_PROFILE_PHOTO;
import static com.lgourabdash.portfolio.PortfolioAttachmentService.KIND_RESUME;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class PortfolioAttachmentController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAttachmentController.class);

    private final PortfolioAttachmentService service;
    private final UserProfileService userProfileService;

    public PortfolioAttachmentController(
            PortfolioAttachmentService service, UserProfileService userProfileService) {
        this.service = service;
        this.userProfileService = userProfileService;
    }

    private static MediaType mediaTypeForProfilePhoto(
            String contentType, String fileName) {
        MediaType mt = MediaType.IMAGE_JPEG;
        try {
            if (contentType != null && !contentType.isBlank()) {
                String simple = contentType.split(";")[0].trim();
                mt = MediaType.parseMediaType(simple);
                return mt;
            }
        } catch (Exception ignored) {
            mt = MediaType.IMAGE_JPEG;
        }
        if (fileName == null || fileName.isBlank()) {
            return mt;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @PostMapping("/{userId}/photo")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            userProfileService.uploadProfilePhoto(userId, file);
            return ResponseEntity.ok("Profile photo updated");
        } catch (Exception e) {
            log.error("Profile photo upload failed: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body("Error uploading photo: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long userId) {
        Optional<PortfolioAttachmentService.AttachmentDownload> fromAtt =
                service.fetchActiveAssetDownload(userId, KIND_PROFILE_PHOTO);
        if (fromAtt.isPresent()) {
            PortfolioAttachmentService.AttachmentDownload d = fromAtt.get();
            byte[] data = d.bytes();
            if (data.length == 0) {
                return ResponseEntity.notFound().build();
            }
            MediaType mt = mediaTypeForProfilePhoto(d.contentType(), d.fileName());
            return ResponseEntity.ok()
                    .contentType(mt)
                    .header("X-Content-Type-Options", "nosniff")
                    .body(data);
        }
        byte[] data = userProfileService.getProfilePhoto(userId);
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(userProfileService.getProfilePhotoMediaType(userId))
                .header("X-Content-Type-Options", "nosniff")
                .body(data);
    }

    @DeleteMapping("/{userId}/photo")
    public ResponseEntity<?> deletePhoto(@PathVariable Long userId) {
        try {
            userProfileService.clearProfilePhoto(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Profile photo delete failed: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/resume")
    public ResponseEntity<?> uploadResume(
            @PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            userProfileService.uploadResume(userId, file);
            return ResponseEntity.ok("Resume updated successfully");
        } catch (Exception e) {
            log.error("Resume upload failed: userId={}", userId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/resume")
    public ResponseEntity<byte[]> getResume(@PathVariable Long userId) {
        Optional<PortfolioAttachmentService.AttachmentDownload> fromAtt =
                service.fetchActiveAssetDownload(userId, KIND_RESUME);
        if (fromAtt.isPresent()) {
            PortfolioAttachmentService.AttachmentDownload d = fromAtt.get();
            byte[] data = d.bytes();
            if (data.length == 0) {
                return ResponseEntity.notFound().build();
            }
            String filename =
                    d.fileName() != null && !d.fileName().isBlank()
                            ? d.fileName()
                            : "resume.pdf";
            MediaType mt = MediaType.APPLICATION_PDF;
            try {
                if (d.contentType() != null && !d.contentType().isBlank()) {
                    mt = MediaType.parseMediaType(d.contentType().split(";")[0].trim());
                }
            } catch (Exception ignored) {
                mt = MediaType.APPLICATION_PDF;
            }
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(mt)
                    .body(data);
        }
        byte[] data = userProfileService.getResume(userId);
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        String filename = userProfileService.getResumeFilename(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @DeleteMapping("/{userId}/resume")
    public ResponseEntity<?> deleteResume(@PathVariable Long userId) {
        try {
            userProfileService.clearResume(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Resume delete failed: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/attachments")
    public ResponseEntity<List<PortfolioAttachmentDto>> list(
            @PathVariable Long userId,
            @RequestParam(required = false) String kind) {
        return ResponseEntity.ok(service.list(userId, kind));
    }

    @GetMapping("/{userId}/attachments/{attachmentId}/file")
    public ResponseEntity<byte[]> download(
            @PathVariable Long userId,
            @PathVariable Long attachmentId) {
        try {
            PortfolioAttachmentService.AttachmentDownload file = service.fetchForHttpDownload(attachmentId, userId);
            byte[] data = file.bytes();
            if (data.length == 0) {
                return ResponseEntity.notFound().build();
            }
            MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
            try {
                String ct = file.contentType();
                if (ct != null && !ct.isBlank()) {
                    String simple = ct.split(";")[0].trim();
                    mt = MediaType.parseMediaType(simple);
                }
            } catch (Exception ignored) {
                mt = MediaType.APPLICATION_OCTET_STREAM;
            }
            String fn = file.fileName() != null ? file.fileName() : "download";
            ResponseEntity.BodyBuilder bb = ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fn + "\"")
                    .contentType(mt)
                    .header("X-Content-Type-Options", "nosniff");
            String kind = file.kind();
            if (KIND_CERTIFICATE.equals(kind)
                    || KIND_EDUCATION_DOC.equals(kind)
                    || KIND_LETTER.equals(kind)
                    || KIND_RESUME.equals(kind)
                    || KIND_PROFILE_PHOTO.equals(kind)) {
                bb = bb.header(HttpHeaders.CACHE_CONTROL, "no-store, private, max-age=0")
                        .header(HttpHeaders.PRAGMA, "no-cache");
            }
            return bb.body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Attachment download failed: userId={} attachmentId={}", userId, attachmentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{userId}/attachments")
    public ResponseEntity<?> upload(
            @PathVariable Long userId,
            @RequestParam String kind,
            @RequestParam String title,
            @RequestParam(required = false) String relatedKey,
            @RequestParam("file") MultipartFile file) {
        try {
            service.save(userId, kind, title, relatedKey, file);
            return ResponseEntity.ok("Uploaded");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable Long userId, @PathVariable Long attachmentId) {
        return doDeleteAttachment(userId, attachmentId);
    }

    /**
     * POST fallback when DELETE is blocked by a proxy or static hosting (avoids “no static resource”
     * style routing issues).
     */
    @PostMapping("/{userId}/attachments/{attachmentId}/delete")
    public ResponseEntity<?> deleteAttachmentPost(
            @PathVariable Long userId, @PathVariable Long attachmentId) {
        return doDeleteAttachment(userId, attachmentId);
    }

    private ResponseEntity<?> doDeleteAttachment(Long userId, Long attachmentId) {
        try {
            service.delete(userId, attachmentId);
            return ResponseEntity.ok("Deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Attachment delete failed: userId={} id={}", userId, attachmentId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/attachments/{attachmentId}/set-active")
    public ResponseEntity<?> setActiveAttachment(
            @PathVariable Long userId, @PathVariable Long attachmentId) {
        try {
            service.setActiveAsset(userId, attachmentId);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Set active attachment failed: userId={} id={}", userId, attachmentId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping(
            value = "/{userId}/attachments/{attachmentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAttachment(
            @PathVariable Long userId,
            @PathVariable Long attachmentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String relatedKey,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            service.update(userId, attachmentId, title, relatedKey, file);
            return ResponseEntity.ok("Updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Attachment update failed: userId={} id={}", userId, attachmentId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
