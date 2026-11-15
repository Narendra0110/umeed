package com.umeed.donations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donation_images")
public class DonationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "storage_key", nullable = false, updatable = false, length = 300)
    private String storageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DonationImage() {
        // Required by JPA
    }

    static DonationImage of(UUID donationId, String storageKey, int sortOrder) {
        DonationImage image = new DonationImage();
        image.donationId = donationId;
        image.storageKey = storageKey;
        image.sortOrder = sortOrder;
        return image;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDonationId() { return donationId; }
    public String getStorageKey() { return storageKey; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
