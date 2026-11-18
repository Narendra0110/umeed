package com.umeed.account;

import com.umeed.blocks.BlockService;
import com.umeed.claims.ClaimService;
import com.umeed.donations.DonationService;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting an account. Lives in its own module because it touches users, donations, claims and blocks.
 */
@Service
public class AccountService {

    private final UserService userService;
    private final DonationService donationService;
    private final ClaimService claimService;
    private final BlockService blockService;

    AccountService(
            UserService userService,
            DonationService donationService,
            ClaimService claimService,
            BlockService blockService) {
        this.userService = userService;
        this.donationService = donationService;
        this.claimService = claimService;
        this.blockService = blockService;
    }

    /**
     * Deletes the caller's account in one transaction. Refused while the caller has a pickup in progress,
     * as donor or as receiver. Otherwise: pending claims are withdrawn and claim messages erased, available
     * listings are removed with their photos, every pickup address is erased, blocks are removed, and the
     * user's Firebase id, phone number and name are cleared. Reports the user filed are kept for moderation.
     */
    @Transactional
    public void deleteAccount(String firebaseUid) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        claimService.eraseClaimant(caller.id());
        donationService.eraseDonor(caller.id());
        blockService.removeAllInvolving(caller.id());
        userService.markDeleted(caller.id());
    }
}
