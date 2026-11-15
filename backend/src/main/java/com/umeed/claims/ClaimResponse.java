package com.umeed.claims;

import java.time.Instant;
import java.util.UUID;

/**
 * A claim as shown to the donor and to the claimant. Never contains a phone number or an address.
 */
public record ClaimResponse(
        UUID id,
        UUID donationId,
        String donationTitle,
        UUID claimantId,
        String claimantName,
        String message,
        String status,
        Instant createdAt) {

    static ClaimResponse from(Claim claim, String donationTitle, String claimantName) {
        return new ClaimResponse(
                claim.getId(),
                claim.getDonationId(),
                donationTitle,
                claim.getClaimantId(),
                claimantName,
                claim.getMessage(),
                claim.getStatus().name(),
                claim.getCreatedAt());
    }
}
