package com.umeed.claims;

import java.time.Instant;
import java.util.UUID;

/**
 * A claim as shown to the donor and to the claimant. Never contains a phone number or an address.
 * donationStatus is the item's own status (AVAILABLE, RESERVED, COMPLETED or REMOVED), so the app can
 * tell an accepted claim that still needs a pickup from one that has been handed over.
 */
public record ClaimResponse(
        UUID id,
        UUID donationId,
        String donationTitle,
        String donationStatus,
        UUID claimantId,
        String claimantName,
        String message,
        String status,
        Instant createdAt) {

    static ClaimResponse from(Claim claim, String donationTitle, String donationStatus, String claimantName) {
        return new ClaimResponse(
                claim.getId(),
                claim.getDonationId(),
                donationTitle,
                donationStatus,
                claim.getClaimantId(),
                claimantName,
                claim.getMessage(),
                claim.getStatus().name(),
                claim.getCreatedAt());
    }
}
