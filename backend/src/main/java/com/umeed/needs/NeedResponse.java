package com.umeed.needs;

import java.time.Instant;
import java.util.UUID;

/**
 * A need as shown to anyone. {@code pendingOfferCount} is filled in only for the person who asked;
 * for everyone else it is null.
 */
public record NeedResponse(
        UUID id,
        String title,
        String description,
        String status,
        UUID categoryId,
        String city,
        String area,
        double latitude,
        double longitude,
        UUID requesterId,
        String requesterName,
        Long pendingOfferCount,
        Instant createdAt) {

    static NeedResponse from(NeedRequest need, String requesterName, Long pendingOfferCount) {
        return new NeedResponse(
                need.getId(),
                need.getTitle(),
                need.getDescription(),
                need.getStatus().name(),
                need.getCategoryId(),
                need.getCity(),
                need.getArea(),
                need.getLatitude(),
                need.getLongitude(),
                need.getRequesterId(),
                requesterName,
                pendingOfferCount,
                need.getCreatedAt());
    }
}
