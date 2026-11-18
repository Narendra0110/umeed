package com.umeed.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One message in the conversation of an accepted claim. */
@Entity
@Table(name = "chat_messages")
class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claim_id", nullable = false, updatable = false)
    private UUID claimId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(nullable = false, length = 1000, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected ChatMessage() {
        // Required by JPA
    }

    static ChatMessage of(UUID claimId, UUID senderId, String body) {
        ChatMessage message = new ChatMessage();
        message.claimId = claimId;
        message.senderId = senderId;
        message.body = body.strip();
        return message;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    UUID getId() { return id; }
    UUID getClaimId() { return claimId; }
    UUID getSenderId() { return senderId; }
    String getBody() { return body; }
    Instant getCreatedAt() { return createdAt; }
    Instant getReadAt() { return readAt; }
}
