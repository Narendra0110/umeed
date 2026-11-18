package com.umeed.needs;

import java.util.UUID;

/** A donor offered an item for a need. Published inside the offer's transaction. */
public record NeedOfferMadeEvent(
        UUID needId, String needTitle, UUID requesterId, UUID donorId, String donorName, String donationTitle) {
}
