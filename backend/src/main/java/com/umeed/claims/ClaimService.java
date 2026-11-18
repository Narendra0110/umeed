package com.umeed.claims;

import com.umeed.common.PageResponse;
import com.umeed.donations.DonationRemovedEvent;
import com.umeed.donations.DonationService;
import com.umeed.donations.DonationSummary;
import com.umeed.users.BlockedUsers;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ClaimService {

    private static final String UNKNOWN_NAME = "Umeed user";

    private final ClaimRepository claimRepository;
    private final UserService userService;
    private final DonationService donationService;
    private final BlockedUsers blockedUsers;
    private final ApplicationEventPublisher eventPublisher;

    ClaimService(
            ClaimRepository claimRepository,
            UserService userService,
            DonationService donationService,
            BlockedUsers blockedUsers,
            ApplicationEventPublisher eventPublisher) {
        this.claimRepository = claimRepository;
        this.userService = userService;
        this.donationService = donationService;
        this.blockedUsers = blockedUsers;
        this.eventPublisher = eventPublisher;
    }

    /**
     * A receiver asks for an item. Any number of people may claim the same item until the donor accepts one.
     */
    @Transactional
    public ClaimResponse submit(String firebaseUid, UUID donationId, String message) {
        UserResponse claimant = userService.requireActiveUser(firebaseUid);
        if (!claimant.profileComplete()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Complete your profile before claiming a donation");
        }
        DonationSummary donation = donationService.lockClaimable(donationId);
        if (donation.donorId().equals(claimant.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot claim your own donation");
        }
        if (blockedUsers.isBlockedEitherWay(claimant.id(), donation.donorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot claim this item");
        }
        if (claimRepository.existsByDonationIdAndClaimantId(donationId, claimant.id())) {
            throw alreadyClaimed();
        }
        Claim claim;
        try {
            claim = claimRepository.saveAndFlush(Claim.create(donationId, claimant.id(), message));
        } catch (DataIntegrityViolationException e) {
            throw alreadyClaimed();
        }
        eventPublisher.publishEvent(new ClaimSubmittedEvent(
                donationId, donation.title(), donation.donorId(), claimant.id(), claimant.displayName()));
        return ClaimResponse.from(claim, donation.title(), donation.status(), claimant.displayName());
    }

    /**
     * All claims on a donation, oldest first. Only the donor may see them.
     */
    @Transactional(readOnly = true)
    public List<ClaimResponse> listForDonation(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        DonationSummary donation = donationService.getSummary(donationId);
        if (!donation.donorId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor can see the claims");
        }
        List<Claim> claims = claimRepository.findByDonationIdOrderByCreatedAtAscIdAsc(donationId);
        Set<UUID> claimantIds = new HashSet<>();
        for (Claim claim : claims) {
            claimantIds.add(claim.getClaimantId());
        }
        Map<UUID, String> names = userService.displayNamesByIds(claimantIds);
        return claims.stream()
                .map(claim -> ClaimResponse.from(
                        claim,
                        donation.title(),
                        donation.status(),
                        names.getOrDefault(claim.getClaimantId(), UNKNOWN_NAME)))
                .toList();
    }

    /**
     * The donor picks a receiver. In one transaction: the donation becomes reserved,
     * this claim becomes accepted and every other pending claim on the donation is rejected.
     */
    @Transactional
    public ClaimResponse accept(String firebaseUid, UUID claimId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        UUID donationId = claimRepository.findById(claimId)
                .map(Claim::getDonationId)
                .orElseThrow(ClaimService::claimNotFound);

        // Locks the donation first; every change to its claims happens while this lock is held.
        DonationSummary donation = donationService.reserve(donationId, caller.id());

        Instant now = Instant.now();
        if (claimRepository.changeStatus(claimId, Claim.Status.PENDING, Claim.Status.ACCEPTED, now) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This claim is no longer pending");
        }
        List<UUID> notChosen = claimRepository.findClaimantIdsByDonationIdAndStatus(donationId, Claim.Status.PENDING);
        claimRepository.changeStatusForDonation(donationId, Claim.Status.PENDING, Claim.Status.REJECTED, now);

        Claim accepted = reload(claimId);
        eventPublisher.publishEvent(new ClaimAcceptedEvent(
                donationId, donation.title(), donation.donorId(), accepted.getClaimantId(), List.copyOf(notChosen)));
        return toResponse(accepted, donation);
    }

    /**
     * A claimant cancels their own pending claim. In the MVP this is final for that item.
     */
    @Transactional
    public ClaimResponse withdraw(String firebaseUid, UUID claimId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Claim claim = claimRepository.findById(claimId).orElseThrow(ClaimService::claimNotFound);
        if (!claim.getClaimantId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the claimant can withdraw this claim");
        }
        UUID donationId = claim.getDonationId();
        if (claimRepository.changeStatus(claimId, Claim.Status.PENDING, Claim.Status.WITHDRAWN, Instant.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a pending claim can be withdrawn");
        }
        DonationSummary donation = donationService.summariesByIds(List.of(donationId)).get(donationId);
        return toResponse(reload(claimId), donation);
    }

    /**
     * The caller's own claims, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listMine(String firebaseUid, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<Claim> page = claimRepository.findByClaimantIdOrderByCreatedAtDesc(caller.id(), pageable);
        Set<UUID> donationIds = new HashSet<>();
        for (Claim claim : page.getContent()) {
            donationIds.add(claim.getDonationId());
        }
        Map<UUID, DonationSummary> donations = donationService.summariesByIds(donationIds);
        String name = caller.displayName() != null ? caller.displayName() : UNKNOWN_NAME;
        return PageResponse.of(page.map(claim -> {
            DonationSummary donation = donations.get(claim.getDonationId());
            return ClaimResponse.from(claim, titleOf(donation), statusOf(donation), name);
        }));
    }

    // ----- For the pickup module -----

    /**
     * The user whose claim on this donation is accepted, if any.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> acceptedClaimantId(UUID donationId) {
        return claimRepository.findFirstByDonationIdAndStatus(donationId, Claim.Status.ACCEPTED)
                .map(Claim::getClaimantId);
    }

    /**
     * A reservation is cancelled. The claims that were rejected by that acceptance become pending again,
     * so the donor can pick someone else. The cancelled claim is closed: withdrawn if the receiver cancelled,
     * rejected if the donor did. A claim closed this way is not reopened by a later cancellation.
     */
    @Transactional
    public void cancelAccepted(UUID donationId, boolean byReceiver) {
        Claim accepted = claimRepository.findFirstByDonationIdAndStatus(donationId, Claim.Status.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "No claim has been accepted for this donation"));
        UUID acceptedId = accepted.getId();
        // accept() stamps the accepted claim and the claims it rejects with the same moment.
        Instant acceptedAt = accepted.getUpdatedAt();
        Instant now = Instant.now();

        claimRepository.changeStatusForDonationChangedAt(
                donationId, Claim.Status.REJECTED, acceptedAt, Claim.Status.PENDING, now);
        claimRepository.changeStatus(
                acceptedId,
                Claim.Status.ACCEPTED,
                byReceiver ? Claim.Status.WITHDRAWN : Claim.Status.REJECTED,
                now);
    }

    /**
     * When a donor takes a listing down, its open claims are closed in the same transaction.
     */
    @EventListener
    @Transactional
    public void onDonationRemoved(DonationRemovedEvent event) {
        claimRepository.changeStatusForDonation(
                event.donationId(), Claim.Status.PENDING, Claim.Status.REJECTED, Instant.now());
    }

    // ----- For the blocks module -----

    /**
     * True when the two users have a pickup in progress together: a reserved item of one of them
     * whose accepted claim belongs to the other.
     */
    @Transactional(readOnly = true)
    public boolean sharePickup(UUID userId, UUID otherUserId) {
        return hasAcceptedClaimOn(otherUserId, donationService.reservedIdsByDonor(userId))
                || hasAcceptedClaimOn(userId, donationService.reservedIdsByDonor(otherUserId));
    }

    /**
     * After a block, the open claims between the two users are closed: the blocked person's claims on the
     * blocker's items are rejected, and the blocker's own claims on the blocked person's items are withdrawn.
     */
    @Transactional
    public void closeClaimsAfterBlock(UUID blockerId, UUID blockedId) {
        Instant now = Instant.now();
        List<UUID> blockerItems = donationService.idsByDonor(blockerId);
        if (!blockerItems.isEmpty()) {
            claimRepository.changeStatusForClaimantOn(
                    blockedId, blockerItems, Claim.Status.PENDING, Claim.Status.REJECTED, now);
        }
        List<UUID> blockedItems = donationService.idsByDonor(blockedId);
        if (!blockedItems.isEmpty()) {
            claimRepository.changeStatusForClaimantOn(
                    blockerId, blockedItems, Claim.Status.PENDING, Claim.Status.WITHDRAWN, now);
        }
    }

    // ----- For account deletion -----

    /**
     * Account deletion, claimant side. Withdraws every pending claim first, so none can be accepted
     * afterwards, then refuses if the user is the receiver of an item that is still reserved.
     * Finally erases the messages the user wrote.
     */
    @Transactional
    public void eraseClaimant(UUID claimantId) {
        claimRepository.changeStatusForClaimant(
                claimantId, Claim.Status.PENDING, Claim.Status.WITHDRAWN, Instant.now());
        List<UUID> acceptedOn = claimRepository.findDonationIdsByClaimantIdAndStatus(claimantId, Claim.Status.ACCEPTED);
        for (DonationSummary donation : donationService.summariesByIds(acceptedOn).values()) {
            if ("RESERVED".equals(donation.status())) {
                throw DonationService.pickupInProgress();
            }
        }
        claimRepository.clearMessages(claimantId);
    }

    private boolean hasAcceptedClaimOn(UUID claimantId, List<UUID> donationIds) {
        return !donationIds.isEmpty()
                && claimRepository.existsByClaimantIdAndStatusAndDonationIdIn(
                        claimantId, Claim.Status.ACCEPTED, donationIds);
    }

    private Claim reload(UUID claimId) {
        return claimRepository.findById(claimId).orElseThrow(ClaimService::claimNotFound);
    }

    private ClaimResponse toResponse(Claim claim, DonationSummary donation) {
        String name = userService.displayNamesByIds(List.of(claim.getClaimantId()))
                .getOrDefault(claim.getClaimantId(), UNKNOWN_NAME);
        return ClaimResponse.from(claim, titleOf(donation), statusOf(donation), name);
    }

    // A claim's donation always exists (rows are never deleted); these only guard against a missing lookup.
    private static String titleOf(DonationSummary donation) {
        return donation != null ? donation.title() : "";
    }

    private static String statusOf(DonationSummary donation) {
        return donation != null ? donation.status() : null;
    }

    private static ResponseStatusException claimNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found");
    }

    private static ResponseStatusException alreadyClaimed() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "You have already claimed this donation");
    }
}
