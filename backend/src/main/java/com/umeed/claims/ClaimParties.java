package com.umeed.claims;

import java.util.UUID;

/**
 * Who is involved in a claim and where it stands. For the chat module, which decides from this who may
 * read or write a conversation. Never contains a phone number or an address.
 */
public record ClaimParties(
        UUID claimId,
        UUID donationId,
        String donationTitle,
        String donationStatus,
        UUID donorId,
        UUID claimantId,
        String claimStatus) {

    public boolean involves(UUID userId) {
        return donorId.equals(userId) || claimantId.equals(userId);
    }

    public UUID otherParty(UUID userId) {
        return donorId.equals(userId) ? claimantId : donorId;
    }

    /** The donor accepted this claim and the handover has not been completed or cancelled. */
    public boolean pickupInProgress() {
        return "ACCEPTED".equals(claimStatus) && "RESERVED".equals(donationStatus);
    }
}
