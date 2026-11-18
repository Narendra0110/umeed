package com.umeed.needs;

import java.util.List;
import java.util.UUID;

/**
 * The person who asked chose an offer. {@code declinedDonorIds} are the donors whose pending offers on the
 * same need were declined by this choice. Published inside the acceptance's transaction.
 */
public record NeedOfferAcceptedEvent(
        UUID needId,
        String needTitle,
        UUID requesterId,
        String requesterName,
        UUID donationId,
        String donationTitle,
        UUID donorId,
        List<UUID> declinedDonorIds) {
}
