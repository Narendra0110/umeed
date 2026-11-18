package com.umeed.pickup;

import com.umeed.claims.ClaimService;
import com.umeed.donations.DonationPickupDetails;
import com.umeed.donations.DonationService;
import com.umeed.users.UserContact;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class PickupService {

    private final DonationService donationService;
    private final ClaimService claimService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    PickupService(
            DonationService donationService,
            ClaimService claimService,
            UserService userService,
            ApplicationEventPublisher eventPublisher) {
        this.donationService = donationService;
        this.claimService = claimService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * The private pickup details: address and both phone numbers.
     * Only for the donor and the receiver whose claim was accepted.
     */
    @Transactional(readOnly = true)
    public PickupResponse getDetails(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        DonationPickupDetails donation = donationService.getPickupDetails(donationId);
        UUID receiverId = requireParty(caller.id(), donation);
        return toResponse(donation, receiverId);
    }

    /**
     * The caller confirms the handover. When both sides have confirmed, the donation is completed.
     */
    @Transactional
    public PickupResponse confirm(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        DonationPickupDetails donation = donationService.lockForPickup(donationId);
        UUID receiverId = requireParty(caller.id(), donation);
        boolean byDonor = donation.donorId().equals(caller.id());
        boolean alreadyConfirmed = byDonor ? donation.donorConfirmed() : donation.receiverConfirmed();
        DonationPickupDetails updated = donationService.confirmHandover(donationId, byDonor);
        boolean otherConfirmed = byDonor ? updated.receiverConfirmed() : updated.donorConfirmed();
        // Ask the other side to confirm only on a first confirmation; the second one completes the item.
        if (!alreadyConfirmed && !otherConfirmed) {
            eventPublisher.publishEvent(new HandoverConfirmedEvent(
                    donationId,
                    updated.title(),
                    caller.id(),
                    caller.displayName(),
                    byDonor ? receiverId : donation.donorId()));
        }
        return toResponse(updated, receiverId);
    }

    /**
     * The donor or the accepted receiver cancels the reservation. The item returns to the feed
     * and the claims that were rejected by the acceptance are open again.
     */
    @Transactional
    public void cancel(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        DonationPickupDetails donation = donationService.lockForPickup(donationId);
        UUID receiverId = requireParty(caller.id(), donation);
        boolean byReceiver = !donation.donorId().equals(caller.id());
        donationService.reopen(donationId);
        claimService.cancelAccepted(donationId, byReceiver);
        eventPublisher.publishEvent(new PickupCancelledEvent(
                donationId, donation.title(), caller.id(), byReceiver ? donation.donorId() : receiverId));
    }

    /**
     * Refuses everyone except the donor and the accepted receiver. Returns the receiver's user id.
     */
    private UUID requireParty(UUID callerId, DonationPickupDetails donation) {
        Optional<UUID> receiverId = claimService.acceptedClaimantId(donation.id());
        boolean isDonor = donation.donorId().equals(callerId);
        boolean isReceiver = receiverId.isPresent() && receiverId.get().equals(callerId);
        if (!isDonor && !isReceiver) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the donor and the accepted receiver can do this");
        }
        return receiverId.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT, "No claim has been accepted for this donation yet"));
    }

    private PickupResponse toResponse(DonationPickupDetails donation, UUID receiverId) {
        UserContact donor = userService.contactOf(donation.donorId());
        UserContact receiver = userService.contactOf(receiverId);
        return new PickupResponse(
                donation.id(),
                donation.title(),
                donation.status(),
                donation.pickupAddress(),
                new PickupResponse.Party(donor.name(), donor.phoneNumber()),
                new PickupResponse.Party(receiver.name(), receiver.phoneNumber()),
                donation.donorConfirmed(),
                donation.receiverConfirmed());
    }
}
