package com.umeed.claims;

import java.util.List;
import java.util.UUID;

/**
 * The donor chose a receiver. {@code notChosenIds} are the people whose pending claims were rejected
 * by this choice. Published inside the acceptance's transaction.
 */
public record ClaimAcceptedEvent(
        UUID donationId, String donationTitle, UUID donorId, UUID receiverId, List<UUID> notChosenIds) {
}
