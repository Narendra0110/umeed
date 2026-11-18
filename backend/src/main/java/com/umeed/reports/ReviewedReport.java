package com.umeed.reports;

import java.time.Instant;
import java.util.UUID;

/** A report after an admin handled it. For the admin module only. */
public record ReviewedReport(
        UUID id, UUID donationId, UUID needId, String status, String reviewNote, Instant reviewedAt) {

    static ReviewedReport from(Report report) {
        return new ReviewedReport(
                report.getId(),
                report.getDonationId(),
                report.getNeedId(),
                report.getStatus().name(),
                report.getReviewNote(),
                report.getReviewedAt());
    }
}
