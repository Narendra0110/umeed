package com.umeed.needs;

import java.util.UUID;

/** After choosing an offer: the item is reserved for the caller, so the app opens its pickup details. */
public record AcceptOfferResponse(UUID donationId, UUID claimId) {
}
