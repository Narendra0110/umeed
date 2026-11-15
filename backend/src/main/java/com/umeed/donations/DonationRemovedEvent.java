package com.umeed.donations;

import java.util.UUID;

/**
 * Published inside the same transaction when a donor takes a listing down,
 * so other modules can react (for example by closing open claims).
 */
public record DonationRemovedEvent(UUID donationId) {
}
