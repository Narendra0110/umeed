package com.umeed.blocks;

import com.umeed.users.BlockedUsers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Answers "who is blocked" for the donations and claims modules. Kept apart from BlockService,
 * which uses those modules, so the beans never depend on each other in a circle.
 */
@Component
class BlockedUsersLookup implements BlockedUsers {

    private final UserBlockRepository userBlockRepository;

    BlockedUsersLookup(UserBlockRepository userBlockRepository) {
        this.userBlockRepository = userBlockRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> blockedEitherWay(UUID userId) {
        Set<UUID> ids = new HashSet<>(userBlockRepository.findBlockedIdsByBlockerId(userId));
        ids.addAll(userBlockRepository.findBlockerIdsByBlockedId(userId));
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(UUID userId, UUID otherUserId) {
        return userBlockRepository.existsEitherWay(userId, otherUserId);
    }
}
