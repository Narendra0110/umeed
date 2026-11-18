package com.umeed.needs;

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

/** A donor offers one of their available items for a need. */
@Entity
@Table(name = "need_offers")
class NeedOffer {

    enum Status { PENDING, ACCEPTED, DECLINED, WITHDRAWN }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "need_id", nullable = false, updatable = false)
    private UUID needId;

    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "donor_id", nullable = false, updatable = false)
    private UUID donorId;

    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NeedOffer() {
        // Required by JPA
    }

    static NeedOffer create(UUID needId, UUID donationId, UUID donorId, String message) {
        NeedOffer offer = new NeedOffer();
        offer.needId = needId;
        offer.donationId = donationId;
        offer.donorId = donorId;
        offer.message = message == null || message.isBlank() ? null : message.strip();
        return offer;
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
    UUID getNeedId() { return needId; }
    UUID getDonationId() { return donationId; }
    UUID getDonorId() { return donorId; }
    String getMessage() { return message; }
    Status getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
}
