package com.umeed.claims;

import java.util.List;
import java.util.UUID;

/**
 * A receiver was chosen. {@code notChosenIds} are the people whose pending claims were rejected by this
 * choice. {@code notifyReceiver} is false when the receiver made the choice themselves (accepting an offer
 * for their need), so they are not told about their own action. Published inside the acceptance's transaction.
 */
public record ClaimAcceptedEvent(
        UUID donationId,
        String donationTitle,
        UUID donorId,
        UUID receiverId,
        List<UUID> notChosenIds,
        boolean notifyReceiver) {
}
