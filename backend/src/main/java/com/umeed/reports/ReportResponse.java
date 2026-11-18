package com.umeed.reports;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirmation returned to the person who reported. Does not say who else reported the listing or need.
 * Exactly one of donationId and needId is set.
 */
public record ReportResponse(UUID id, UUID donationId, UUID needId, String reason, String status, Instant createdAt) {

    static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getDonationId(),
                report.getNeedId(),
                report.getReason().name(),
                report.getStatus().name(),
                report.getCreatedAt());
    }
}
