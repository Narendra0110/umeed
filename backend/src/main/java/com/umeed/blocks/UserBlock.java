package com.umeed.blocks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One user has blocked another. */
@Entity
@Table(name = "user_blocks")
@IdClass(UserBlock.Key.class)
class UserBlock {

    @Id
    @Column(name = "blocker_id", nullable = false, updatable = false)
    private UUID blockerId;

    @Id
    @Column(name = "blocked_id", nullable = false, updatable = false)
    private UUID blockedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserBlock() {
        // Required by JPA
    }

    static UserBlock of(UUID blockerId, UUID blockedId) {
        UserBlock block = new UserBlock();
        block.blockerId = blockerId;
        block.blockedId = blockedId;
        return block;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    UUID getBlockerId() { return blockerId; }
    UUID getBlockedId() { return blockedId; }
    Instant getCreatedAt() { return createdAt; }

    /** The primary key: the pair of users. */
    static class Key implements Serializable {

        private UUID blockerId;
        private UUID blockedId;

        protected Key() {
            // Required by JPA
        }

        Key(UUID blockerId, UUID blockedId) {
            this.blockerId = blockerId;
            this.blockedId = blockedId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && Objects.equals(blockerId, key.blockerId)
                    && Objects.equals(blockedId, key.blockedId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockerId, blockedId);
        }
    }
}
