package com.umeed.users;

import java.util.Set;
import java.util.UUID;

/**
 * Who has blocked whom. A block works both ways, so these methods never ask who started it.
 * Implemented by the blocks module; declared here so the donations and claims modules can use it
 * without depending on that module.
 */
public interface BlockedUsers {

    /** Everyone this user has blocked, plus everyone who has blocked this user. */
    Set<UUID> blockedEitherWay(UUID userId);

    /** True when either of the two users has blocked the other. */
    boolean isBlockedEitherWay(UUID userId, UUID otherUserId);
}
