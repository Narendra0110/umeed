package com.umeed.pickup;

import java.util.UUID;

/**
 * One side confirmed the handover and the other side has not confirmed yet;
 * {@code otherPartyId} is the person asked to confirm too.
 */
public record HandoverConfirmedEvent(
        UUID donationId, String donationTitle, UUID confirmedById, String confirmedByName, UUID otherPartyId) {
}
