package com.neong.vixie.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase configuration for the AI Companion service.
 * Initializes FirebaseApp for FCM push notifications.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FileInputStream serviceAccount =
                        new FileInputStream("firebase-service-account.json");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully for ai-companion");
            } catch (IOException e) {
                log.warn("Firebase service account not found — FCM push disabled. "
                        + "Place firebase-service-account.json in ai-companion root.");
            }
        } else {
            log.info("FirebaseApp already initialized");
        }
    }
}
