package com.umeed.donations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a donation. Never contains the pickup address.
 */
public record DonationResponse(
        UUID id,
        String title,
        String description,
        String itemCondition,
        String status,
        UUID categoryId,
        String city,
        String area,
        double latitude,
        double longitude,
        UUID donorId,
        String donorName,
        List<String> imageUrls,
        Instant createdAt) {

    static DonationResponse from(Donation donation, String donorName, List<String> imageUrls) {
        return new DonationResponse(
                donation.getId(),
                donation.getTitle(),
                donation.getDescription(),
                donation.getItemCondition().name(),
                donation.getStatus().name(),
                donation.getCategoryId(),
                donation.getCity(),
                donation.getArea(),
                donation.getLatitude(),
                donation.getLongitude(),
                donation.getDonorId(),
                donorName,
                List.copyOf(imageUrls),
                donation.getCreatedAt());
    }
}
