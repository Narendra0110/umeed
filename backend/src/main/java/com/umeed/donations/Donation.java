package com.umeed.donations;

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

@Entity
@Table(name = "donations")
public class Donation {

    public enum ItemCondition { NEW, LIKE_NEW, GOOD, USABLE }

    public enum Status { AVAILABLE, RESERVED, COMPLETED, REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "donor_id", nullable = false, updatable = false)
    private UUID donorId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String area;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Donation() {
        // Required by JPA
    }

    static Donation create(
            UUID donorId,
            UUID categoryId,
            String title,
            String description,
            ItemCondition itemCondition,
            String city,
            String area,
            double latitude,
            double longitude,
            String pickupAddress) {
        Donation donation = new Donation();
        donation.donorId = donorId;
        donation.categoryId = categoryId;
        donation.title = title.strip();
        donation.description = description.strip();
        donation.itemCondition = itemCondition;
        donation.city = city.strip();
        donation.area = area.strip();
        donation.latitude = approximate(latitude);
        donation.longitude = approximate(longitude);
        donation.pickupAddress = pickupAddress.strip();
        return donation;
    }

    /** Rounds a coordinate to two decimals (about one kilometre) so the stored location cannot pinpoint a home. */
    private static double approximate(double coordinate) {
        return Math.round(coordinate * 100.0) / 100.0;
    }

    /** Takes the listing down. The row is kept so claims and reports that point at it stay valid. */
    void markRemoved() {
        this.status = Status.REMOVED;
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

    public UUID getId() { return id; }
    public UUID getDonorId() { return donorId; }
    public UUID getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ItemCondition getItemCondition() { return itemCondition; }
    public Status getStatus() { return status; }
    public String getCity() { return city; }
    public String getArea() { return area; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPickupAddress() { return pickupAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
