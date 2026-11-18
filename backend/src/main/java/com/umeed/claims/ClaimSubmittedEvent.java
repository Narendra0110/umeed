package com.umeed.claims;

import java.util.UUID;

/** Someone claimed an item. Published inside the claim's transaction. */
public record ClaimSubmittedEvent(
        UUID donationId, String donationTitle, UUID donorId, UUID claimantId, String claimantName) {
}
