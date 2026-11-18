package com.umeed.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    /**
     * Real Firebase sending when a key file is configured, otherwise log-only. A missing or broken key
     * never stops the backend from starting; it only means notifications are not delivered.
     */
    @Bean
    PushSender pushSender(@Value("${umeed.notifications.credentials-file:}") String keyFile) {
        if (keyFile == null || keyFile.isBlank()) {
            log.info("No Firebase key configured: notifications are written to the log only");
            return new LoggingPushSender();
        }
        try {
            return new FcmPushSender(Path.of(keyFile));
        } catch (IOException | RuntimeException e) {
            log.error("The Firebase key could not be read: notifications are written to the log only", e);
            return new LoggingPushSender();
        }
    }
}
