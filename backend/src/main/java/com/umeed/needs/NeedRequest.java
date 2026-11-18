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

/** Something a person needs. Its location is approximate; it has no address. */
@Entity
@Table(name = "need_requests")
class NeedRequest {

    enum Status { OPEN, FULFILLED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String area;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NeedRequest() {
        // Required by JPA
    }

    static NeedRequest create(UUID requesterId, CreateNeedRequest request) {
        NeedRequest need = new NeedRequest();
        need.requesterId = requesterId;
        need.categoryId = request.categoryId();
        need.title = request.title().strip();
        need.description = request.description().strip();
        need.city = request.city().strip();
        need.area = request.area().strip();
        need.latitude = approximate(request.latitude());
        need.longitude = approximate(request.longitude());
        return need;
    }

    /** Rounds a coordinate to two decimals (about one kilometre) so the stored location cannot pinpoint a home. */
    private static double approximate(double coordinate) {
        return Math.round(coordinate * 100.0) / 100.0;
    }

    void markFulfilled() {
        status = Status.FULFILLED;
    }

    void close() {
        status = Status.CLOSED;
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
    UUID getRequesterId() { return requesterId; }
    UUID getCategoryId() { return categoryId; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    Status getStatus() { return status; }
    String getCity() { return city; }
    String getArea() { return area; }
    double getLatitude() { return latitude; }
    double getLongitude() { return longitude; }
    Instant getCreatedAt() { return createdAt; }
}
