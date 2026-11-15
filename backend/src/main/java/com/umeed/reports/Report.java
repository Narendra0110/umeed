package com.umeed.reports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    public enum Reason { SELLING_OR_ASKING_MONEY, PROHIBITED_ITEM, FAKE_OR_MISLEADING, OFFENSIVE, OTHER }

    public enum Status { OPEN, REVIEWED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private Reason reason;

    @Column(length = 500, updatable = false)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Report() {
        // Required by JPA
    }

    static Report create(UUID donationId, UUID reporterId, Reason reason, String details) {
        Report report = new Report();
        report.donationId = donationId;
        report.reporterId = reporterId;
        report.reason = reason;
        report.details = details == null || details.isBlank() ? null : details.strip();
        return report;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDonationId() { return donationId; }
    public UUID getReporterId() { return reporterId; }
    public Reason getReason() { return reason; }
    public String getDetails() { return details; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
