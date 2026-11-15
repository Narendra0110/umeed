package com.umeed.donations;

import java.util.UUID;

/**
 * The few facts about a donation that other modules need. Contains nothing private.
 */
public record DonationSummary(UUID id, UUID donorId, String title, String status) {

    static DonationSummary from(Donation donation) {
        return new DonationSummary(
                donation.getId(), donation.getDonorId(), donation.getTitle(), donation.getStatus().name());
    }
}
