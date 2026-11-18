package com.umeed.donations;

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

/** A user marked an item "Not interested": it no longer appears on their Home. */
@Entity
@Table(name = "donation_skips")
@IdClass(DonationSkip.Key.class)
class DonationSkip {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DonationSkip() {
        // Required by JPA
    }

    static DonationSkip of(UUID userId, UUID donationId) {
        DonationSkip skip = new DonationSkip();
        skip.userId = userId;
        skip.donationId = donationId;
        return skip;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** The primary key: the pair of user and item. */
    static class Key implements Serializable {

        private UUID userId;
        private UUID donationId;

        protected Key() {
            // Required by JPA
        }

        Key(UUID userId, UUID donationId) {
            this.userId = userId;
            this.donationId = donationId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && Objects.equals(userId, key.userId)
                    && Objects.equals(donationId, key.donationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, donationId);
        }
    }
}
