package com.umeed.claims;

import com.umeed.donations.ClaimedDonations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Answers the feed's question "which items has this user claimed?". Kept apart from ClaimService,
 * which depends on DonationService, so the two services never need each other.
 */
@Component
class ClaimedDonationsLookup implements ClaimedDonations {

    private final ClaimRepository claimRepository;

    ClaimedDonationsLookup(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> donationIdsClaimedBy(UUID userId) {
        return claimRepository.findDonationIdsByClaimantId(userId);
    }
}
