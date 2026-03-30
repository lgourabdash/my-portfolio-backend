package com.lgourabdash.portfolio;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile uploadProfilePhoto(Long id, MultipartFile file) throws IOException {
        UserProfile profile =
                repository
                        .findById(id)
                        .orElseGet(
                                () -> {
                                    UserProfile u = new UserProfile();
                                    u.setId(id);
                                    return u;
                                });

        profile.setProfilePhoto(file.getBytes());
        profile.setProfilePhotoName(file.getOriginalFilename());

        return repository.save(profile);
    }

    public UserProfile uploadResume(Long id, MultipartFile file) throws IOException {
        if (!file.getOriginalFilename().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed!");
        }

        UserProfile profile =
                repository
                        .findById(id)
                        .orElseGet(
                                () -> {
                                    UserProfile u = new UserProfile();
                                    u.setId(id);
                                    return u;
                                });

        profile.setResume(file.getBytes());
        profile.setResumeName(file.getOriginalFilename());

        return repository.save(profile);
    }

    /** Clears stored photo if a profile row exists (no-op if missing). */
    public void clearProfilePhoto(Long id) {
        repository
                .findById(id)
                .ifPresent(
                        p -> {
                            p.setProfilePhoto(null);
                            p.setProfilePhotoName(null);
                            repository.save(p);
                        });
    }

    /** Clears stored resume if a profile row exists (no-op if missing). */
    public void clearResume(Long id) {
        repository
                .findById(id)
                .ifPresent(
                        p -> {
                            p.setResume(null);
                            p.setResumeName(null);
                            repository.save(p);
                        });
    }

    public byte[] getProfilePhoto(Long id) {
        return repository.findById(id)
                .map(UserProfile::getProfilePhoto)
                .orElse(null);
    }

    public byte[] getResume(Long id) {
        return repository.findById(id)
                .map(UserProfile::getResume)
                .orElse(null);
    }

    public String getResumeFilename(Long id) {
        return repository.findById(id)
                .map(UserProfile::getResumeName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("resume.pdf");
    }

    /** Content-Type for GET /photo from stored filename; defaults to image/jpeg. */
    public MediaType getProfilePhotoMediaType(Long id) {
        return repository
                .findById(id)
                .map(UserProfile::getProfilePhotoName)
                .map(UserProfileService::mediaTypeFromFilename)
                .orElse(MediaType.IMAGE_JPEG);
    }

    private static MediaType mediaTypeFromFilename(String name) {
        if (name == null || name.isBlank()) {
            return MediaType.IMAGE_JPEG;
        }
        String lower = name.toLowerCase(Locale.ROOT);
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
}
