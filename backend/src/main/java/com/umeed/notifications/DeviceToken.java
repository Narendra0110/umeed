package com.umeed.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One phone a user is logged in on. The token is Firebase's address for that phone. */
@Entity
@Table(name = "device_tokens")
class DeviceToken {

    enum Platform { ANDROID, IOS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {
        // Required by JPA
    }

    static DeviceToken of(UUID userId, String token, Platform platform) {
        DeviceToken device = new DeviceToken();
        device.userId = userId;
        device.token = token;
        device.platform = platform;
        return device;
    }

    /** Someone else logged in on this phone: notifications now go to them. */
    void assignTo(UUID userId, Platform platform) {
        this.userId = userId;
        this.platform = platform;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getToken() { return token; }
    Platform getPlatform() { return platform; }
}
