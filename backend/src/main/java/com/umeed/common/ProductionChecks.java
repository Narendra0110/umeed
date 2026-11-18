package com.umeed.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Production only (H02): stops the backend at startup when something it needs is missing, instead of running
 * half-broken (for example without notifications). The database settings fail even earlier, because
 * application-prod.properties gives them no default.
 */
@Component
@Profile("prod")
class ProductionChecks {

    private final String firebaseKeyFile;
    private final String photosDir;

    ProductionChecks(
            @Value("${umeed.notifications.credentials-file:}") String firebaseKeyFile,
            @Value("${umeed.storage.local.root:}") String photosDir) {
        this.firebaseKeyFile = firebaseKeyFile;
        this.photosDir = photosDir;
    }

    @EventListener(ApplicationStartedEvent.class)
    void check() {
        List<String> problems = problems(firebaseKeyFile, photosDir);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Production settings are not ready:\n - " + String.join("\n - ", problems));
        }
    }

    /** Everything that is wrong, in plain words. Empty when all is well. */
    static List<String> problems(String firebaseKeyFile, String photosDir) {
        List<String> problems = new ArrayList<>();
        if (firebaseKeyFile == null || firebaseKeyFile.isBlank()) {
            problems.add("UMEED_FIREBASE_CREDENTIALS is empty: set it to the Firebase key file");
        } else if (!Files.isReadable(Path.of(firebaseKeyFile))) {
            problems.add("The Firebase key file cannot be read: " + firebaseKeyFile);
        }
        if (photosDir == null || photosDir.isBlank()) {
            problems.add("The photos folder is not set (UMEED_PHOTOS_DIR)");
        } else if (!isWritableFolder(Path.of(photosDir))) {
            problems.add("The photos folder does not exist or cannot be written: " + photosDir);
        }
        return problems;
    }

    /** Tries a real write, since folder permissions on Linux can say one thing and allow another. */
    private static boolean isWritableFolder(Path folder) {
        if (!Files.isDirectory(folder)) {
            return false;
        }
        try {
            Path probe = Files.createTempFile(folder, ".write-check", ".tmp");
            Files.delete(probe);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
