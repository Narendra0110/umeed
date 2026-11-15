package com.umeed.donations;

import java.util.UUID;

/**
 * PRIVATE details of a donation, including the pickup address.
 * Only the pickup module may use this, and only to answer the donor or the accepted receiver.
 */
public record DonationPickupDetails(
        UUID id,
        UUID donorId,
        String title,
        String status,
        String pickupAddress,
        boolean donorConfirmed,
        boolean receiverConfirmed) {

    static DonationPickupDetails from(Donation donation) {
        return new DonationPickupDetails(
                donation.getId(),
                donation.getDonorId(),
                donation.getTitle(),
                donation.getStatus().name(),
                donation.getPickupAddress(),
                donation.getDonorConfirmedAt() != null,
                donation.getReceiverConfirmedAt() != null);
    }
}
