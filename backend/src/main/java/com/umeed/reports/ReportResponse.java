package com.umeed.reports;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirmation returned to the person who reported. Does not say who else reported the listing.
 */
public record ReportResponse(UUID id, UUID donationId, String reason, String status, Instant createdAt) {

    static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getDonationId(),
                report.getReason().name(),
                report.getStatus().name(),
                report.getCreatedAt());
    }
}
