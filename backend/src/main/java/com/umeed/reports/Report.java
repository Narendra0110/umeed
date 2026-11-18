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

/** A user's report that a listing or a need breaks the rules. Exactly one of donationId and needId is set. */
@Entity
@Table(name = "reports")
public class Report {

    public enum Reason { SELLING_OR_ASKING_MONEY, PROHIBITED_ITEM, FAKE_OR_MISLEADING, OFFENSIVE, OTHER }

    public enum Status { OPEN, REVIEWED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "donation_id", updatable = false)
    private UUID donationId;

    @Column(name = "need_id", updatable = false)
    private UUID needId;

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

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    protected Report() {
        // Required by JPA
    }

    static Report aboutDonation(UUID donationId, UUID reporterId, Reason reason, String details) {
        Report report = create(reporterId, reason, details);
        report.donationId = donationId;
        return report;
    }

    static Report aboutNeed(UUID needId, UUID reporterId, Reason reason, String details) {
        Report report = create(reporterId, reason, details);
        report.needId = needId;
        return report;
    }

    private static Report create(UUID reporterId, Reason reason, String details) {
        Report report = new Report();
        report.reporterId = reporterId;
        report.reason = reason;
        report.details = details == null || details.isBlank() ? null : details.strip();
        return report;
    }

    /** An admin closes the report as REVIEWED (action taken or confirmed) or DISMISSED (nothing wrong). */
    void review(Status decision, UUID adminId, String note) {
        status = decision;
        reviewedBy = adminId;
        reviewedAt = Instant.now();
        reviewNote = note == null || note.isBlank() ? null : note.strip();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDonationId() { return donationId; }
    public UUID getNeedId() { return needId; }
    public UUID getReporterId() { return reporterId; }
    public Reason getReason() { return reason; }
    public String getDetails() { return details; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
}
