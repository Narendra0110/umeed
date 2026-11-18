package com.umeed.blocks;

import com.umeed.claims.ClaimService;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserService userService;
    private final ClaimService claimService;

    BlockService(UserBlockRepository userBlockRepository, UserService userService, ClaimService claimService) {
        this.userBlockRepository = userBlockRepository;
        this.userService = userService;
        this.claimService = claimService;
    }

    /**
     * The caller blocks another user. From now on neither sees or claims the other's items, and the
     * open claims between them are closed. Refused while they share a pickup. Blocking twice changes nothing.
     */
    @Transactional
    public void block(String firebaseUid, UUID userId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        if (caller.id().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself");
        }
        if (!userService.isActiveUser(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(caller.id(), userId)) {
            return;
        }
        if (claimService.sharePickup(caller.id(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Cancel your pickup with this person before blocking them");
        }
        try {
            userBlockRepository.saveAndFlush(UserBlock.of(caller.id(), userId));
        } catch (DataIntegrityViolationException e) {
            // The same block was saved by another request at the same moment.
            return;
        }
        claimService.closeClaimsAfterBlock(caller.id(), userId);
    }

    /**
     * Removes the caller's block. Items become visible again; claims that were closed stay closed.
     */
    @Transactional
    public void unblock(String firebaseUid, UUID userId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        userBlockRepository.deletePair(caller.id(), userId);
    }

    /**
     * The people the caller has blocked, most recent first.
     */
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> listMine(String firebaseUid) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        List<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(caller.id());
        Map<UUID, String> names = userService.displayNamesByIds(
                blocks.stream().map(UserBlock::getBlockedId).toList());
        return blocks.stream()
                .map(block -> new BlockedUserResponse(
                        block.getBlockedId(),
                        names.getOrDefault(block.getBlockedId(), "Umeed user"),
                        block.getCreatedAt()))
                .toList();
    }

    /**
     * For account deletion: removes every block this user made or received.
     */
    @Transactional
    public void removeAllInvolving(UUID userId) {
        userBlockRepository.deleteAllInvolving(userId);
    }
}
