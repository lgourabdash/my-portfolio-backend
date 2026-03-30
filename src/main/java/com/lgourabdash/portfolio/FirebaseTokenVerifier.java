package com.lgourabdash.portfolio;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Lazy-init Firebase Admin from JSON or Base64 JSON (Render-friendly). */
@Component
public class FirebaseTokenVerifier {

    @Value("${portfolio.admin.firebase-service-account-json:}")
    private String serviceAccountJson;

    private final Object initLock = new Object();
    private volatile FirebaseAuth firebaseAuth;
    private volatile boolean initFailed;

    public boolean isConfigured() {
        return serviceAccountJson != null && !serviceAccountJson.isBlank();
    }

    private FirebaseAuth authOrNull() {
        if (initFailed) {
            return null;
        }
        if (firebaseAuth != null) {
            return firebaseAuth;
        }
        synchronized (initLock) {
            if (initFailed) {
                return null;
            }
            if (firebaseAuth != null) {
                return firebaseAuth;
            }
            if (!isConfigured()) {
                return null;
            }
            try {
                String raw = serviceAccountJson.trim();
                if (!raw.startsWith("{")) {
                    raw = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
                }
                GoogleCredentials credentials =
                        GoogleCredentials.fromStream(
                                new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
                FirebaseOptions options =
                        FirebaseOptions.builder().setCredentials(credentials).build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                firebaseAuth = FirebaseAuth.getInstance();
                return firebaseAuth;
            } catch (Exception e) {
                initFailed = true;
                return null;
            }
        }
    }

    /** Returns verified email, or empty if token invalid / Firebase not configured. */
    public Optional<String> verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        FirebaseAuth auth = authOrNull();
        if (auth == null) {
            return Optional.empty();
        }
        try {
            FirebaseToken decoded = auth.verifyIdToken(idToken);
            return Optional.ofNullable(decoded.getEmail());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
