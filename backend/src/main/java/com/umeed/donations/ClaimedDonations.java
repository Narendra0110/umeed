package com.umeed.donations;

import java.util.Set;
import java.util.UUID;

/**
 * Which donations a user has already claimed, in any claim status. The feed leaves these out,
 * because a person can claim a given item only once.
 *
 * Defined here and implemented by the claims module, so donations never depends on claims
 * (claims already depends on donations).
 */
public interface ClaimedDonations {

    Set<UUID> donationIdsClaimedBy(UUID userId);
}
