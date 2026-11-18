package com.umeed.needs;

import java.time.Instant;
import java.util.UUID;

/**
 * An offer as shown to the person who asked and to the donor. {@code donationStatus} tells whether the
 * offered item is still available. Never contains a phone number or an address.
 */
public record OfferResponse(
        UUID id,
        UUID needId,
        String needTitle,
        UUID donationId,
        String donationTitle,
        String donationStatus,
        String donationImageUrl,
        UUID donorId,
        String donorName,
        String message,
        String status,
        Instant createdAt) {
}
