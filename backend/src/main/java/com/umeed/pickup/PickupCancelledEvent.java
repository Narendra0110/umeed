package com.umeed.pickup;

import java.util.UUID;

/** A reservation was cancelled by one side; {@code otherPartyId} is the person to tell. */
public record PickupCancelledEvent(UUID donationId, String donationTitle, UUID cancelledById, UUID otherPartyId) {
}
