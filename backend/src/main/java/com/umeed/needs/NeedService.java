package com.umeed.needs;

import com.umeed.categories.CategoryService;
import com.umeed.claims.ClaimService;
import com.umeed.common.PageResponse;
import com.umeed.donations.DonationRemovedEvent;
import com.umeed.donations.DonationResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Need requests and offers. A person asks for something; donors offer one of their available items;
 * the person who asked chooses one offer, which reserves that item for them like an accepted claim.
 */
@Service
public class NeedService {

    private static final String UNKNOWN_NAME = "Umeed user";
    /** The all-zero UUID. Never a user id; used only to keep "not in" lists non-empty. */
    private static final UUID NO_USER = new UUID(0L, 0L);

    private final NeedRequestRepository needRepository;
    private final NeedOfferRepository offerRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final DonationService donationService;
    private final ClaimService claimService;
    private final BlockedUsers blockedUsers;
    private final ApplicationEventPublisher eventPublisher;

    NeedService(
            NeedRequestRepository needRepository,
            NeedOfferRepository offerRepository,
            UserService userService,
            CategoryService categoryService,
            DonationService donationService,
            ClaimService claimService,
            BlockedUsers blockedUsers,
            ApplicationEventPublisher eventPublisher) {
        this.needRepository = needRepository;
        this.offerRepository = offerRepository;
        this.userService = userService;
        this.categoryService = categoryService;
        this.donationService = donationService;
        this.claimService = claimService;
        this.blockedUsers = blockedUsers;
        this.eventPublisher = eventPublisher;
    }

    // ----- Needs -----

    @Transactional
    public NeedResponse create(String firebaseUid, CreateNeedRequest request) {
        UserResponse caller = requireCompleteProfile(firebaseUid, "Complete your profile before asking for something");
        if (!categoryService.isActiveCategory(request.categoryId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category");
        }
        NeedRequest need = needRepository.saveAndFlush(NeedRequest.create(caller.id(), request));
        return NeedResponse.from(need, caller.displayName(), 0L);
    }

    /** Open needs the caller can help with, newest first. Their own and blocked people's are left out. */
    @Transactional(readOnly = true)
    public PageResponse<NeedResponse> listOpen(String firebaseUid, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<NeedRequest> page = needRepository.findOpenNewestFirst(caller.id(), hiddenFor(caller.id()), pageable);
        return PageResponse.of(toResponses(page, caller.id()));
    }

    /** The same list, nearest first. The caller's location is not stored. */
    @Transactional(readOnly = true)
    public PageResponse<NeedResponse> listOpenNearest(
            String firebaseUid, double latitude, double longitude, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<NeedRequest> page = needRepository.findOpenNearestFirst(
                caller.id(), hiddenFor(caller.id()), latitude, longitude, pageable);
        return PageResponse.of(toResponses(page, caller.id()));
    }

    /** The caller's own needs in every status, newest first, with the number of offers waiting. */
    @Transactional(readOnly = true)
    public PageResponse<NeedResponse> listMine(String firebaseUid, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<NeedRequest> page = needRepository.findByRequesterIdOrderByCreatedAtDesc(caller.id(), pageable);
        return PageResponse.of(toResponses(page, caller.id()));
    }

    @Transactional(readOnly = true)
    public NeedResponse getOne(String firebaseUid, UUID needId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        NeedRequest need = needRepository.findById(needId).orElseThrow(NeedService::needNotFound);
        boolean mine = need.getRequesterId().equals(caller.id());
        if (!mine && blockedUsers.isBlockedEitherWay(caller.id(), need.getRequesterId())) {
            throw needNotFound();
        }
        Long pending = mine ? offerRepository.pendingCounts(List.of(needId)).getOrDefault(needId, 0L) : null;
        return NeedResponse.from(need, nameOf(need.getRequesterId()), pending);
    }

    /** The person who asked closes the need. Offers still waiting are declined. */
    @Transactional
    public NeedResponse close(String firebaseUid, UUID needId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        NeedRequest need = needRepository.findForUpdateById(needId).orElseThrow(NeedService::needNotFound);
        requireRequester(need, caller.id());
        if (need.getStatus() != NeedRequest.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This need is no longer open");
        }
        need.close();
        needRepository.flush();
        offerRepository.changeStatusForNeed(
                needId, NeedOffer.Status.PENDING, NeedOffer.Status.DECLINED, Instant.now());
        return NeedResponse.from(need, caller.displayName(), 0L);
    }

    // ----- Offers -----

    /**
     * A donor offers one of their own available items for an open need.
     */
    @Transactional
    public OfferResponse offer(String firebaseUid, UUID needId, UUID donationId, String message) {
        UserResponse caller = requireCompleteProfile(firebaseUid, "Complete your profile before offering an item");
        NeedRequest need = needRepository.findById(needId).orElseThrow(NeedService::needNotFound);
        if (need.getRequesterId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot offer an item for your own need");
        }
        if (blockedUsers.isBlockedEitherWay(caller.id(), need.getRequesterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot offer an item for this need");
        }
        if (need.getStatus() != NeedRequest.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This need is no longer open");
        }
        DonationSummary donation = donationService.getSummary(donationId);
        if (!donation.donorId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only offer your own items");
        }
        if (!"AVAILABLE".equals(donation.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This item is no longer available");
        }
        if (offerRepository.existsByNeedIdAndDonationId(needId, donationId)) {
            throw alreadyOffered();
        }
        NeedOffer offer;
        try {
            offer = offerRepository.saveAndFlush(NeedOffer.create(needId, donationId, caller.id(), message));
        } catch (DataIntegrityViolationException e) {
            throw alreadyOffered();
        }
        eventPublisher.publishEvent(new NeedOfferMadeEvent(
                needId, need.getTitle(), need.getRequesterId(), caller.id(), caller.displayName(), donation.title()));
        return toOfferResponses(List.of(offer), Map.of(needId, need)).get(0);
    }

    /** The person who asked sees every offer on their need, oldest first. */
    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(String firebaseUid, UUID needId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        NeedRequest need = needRepository.findById(needId).orElseThrow(NeedService::needNotFound);
        requireRequester(need, caller.id());
        return toOfferResponses(offerRepository.findByNeedIdOrderByCreatedAtAscIdAsc(needId), Map.of(needId, need));
    }

    /** The caller's own offers, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<OfferResponse> listMyOffers(String firebaseUid, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<NeedOffer> page = offerRepository.findByDonorIdOrderByCreatedAtDesc(caller.id(), pageable);
        Set<UUID> needIds = new HashSet<>();
        page.getContent().forEach(offer -> needIds.add(offer.getNeedId()));
        Map<UUID, NeedRequest> needs = new HashMap<>();
        needRepository.findAllById(needIds).forEach(need -> needs.put(need.getId(), need));
        List<OfferResponse> responses = toOfferResponses(page.getContent(), needs);
        return new PageResponse<>(
                responses, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    /**
     * The person who asked chooses an offer. In one transaction: the item is reserved for them through an
     * accepted claim, this offer is accepted, the other waiting offers are declined, and the need is fulfilled.
     */
    @Transactional
    public AcceptOfferResponse accept(String firebaseUid, UUID offerId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        NeedOffer offer = offerRepository.findById(offerId).orElseThrow(NeedService::offerNotFound);
        NeedRequest need = needRepository.findForUpdateById(offer.getNeedId()).orElseThrow(NeedService::needNotFound);
        requireRequester(need, caller.id());
        if (need.getStatus() != NeedRequest.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This need is no longer open");
        }
        if (offer.getStatus() != NeedOffer.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is no longer waiting");
        }
        UUID needId = need.getId();
        String needTitle = need.getTitle();
        UUID donationId = offer.getDonationId();
        UUID donorId = offer.getDonorId();

        // Refuses (409) before anything is written if the item is no longer available.
        UUID claimId = claimService.acceptForNeed(donationId, donorId, caller.id());

        // The claim steps run bulk updates that detach loaded entities, so the need (still locked) is reloaded.
        NeedRequest locked = needRepository.findById(needId).orElseThrow(NeedService::needNotFound);
        locked.markFulfilled();
        needRepository.flush();

        Instant now = Instant.now();
        offerRepository.changeStatus(offerId, NeedOffer.Status.PENDING, NeedOffer.Status.ACCEPTED, now);
        List<UUID> declined = offerRepository.findDonorIdsByNeedIdAndStatus(needId, NeedOffer.Status.PENDING);
        offerRepository.changeStatusForNeed(needId, NeedOffer.Status.PENDING, NeedOffer.Status.DECLINED, now);

        String donationTitle = donationService.getSummary(donationId).title();
        eventPublisher.publishEvent(new NeedOfferAcceptedEvent(
                needId, needTitle, caller.id(), caller.displayName(),
                donationId, donationTitle, donorId, List.copyOf(declined)));
        return new AcceptOfferResponse(donationId, claimId);
    }

    /** The donor takes back an offer that is still waiting. */
    @Transactional
    public void withdraw(String firebaseUid, UUID offerId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        NeedOffer offer = offerRepository.findById(offerId).orElseThrow(NeedService::offerNotFound);
        if (!offer.getDonorId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor can withdraw this offer");
        }
        if (offerRepository.changeStatus(offerId, NeedOffer.Status.PENDING, NeedOffer.Status.WITHDRAWN, Instant.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a waiting offer can be withdrawn");
        }
    }

    // ----- For the blocks and account modules -----

    /**
     * After a block, waiting offers between the two are closed: the blocked person's offers on the blocker's
     * needs are declined, and the blocker's own offers on the blocked person's needs are withdrawn.
     */
    @Transactional
    public void closeOffersAfterBlock(UUID blockerId, UUID blockedId) {
        Instant now = Instant.now();
        offerRepository.changeStatusForDonorOnNeedsOf(
                blockedId, blockerId, NeedOffer.Status.PENDING, NeedOffer.Status.DECLINED, now);
        offerRepository.changeStatusForDonorOnNeedsOf(
                blockerId, blockedId, NeedOffer.Status.PENDING, NeedOffer.Status.WITHDRAWN, now);
    }

    /** Account deletion: open needs are closed (their waiting offers declined) and own offers withdrawn. */
    @Transactional
    public void eraseUser(UUID userId) {
        Instant now = Instant.now();
        offerRepository.declineAllPendingOnNeedsOf(userId, now);
        needRepository.closeAllOpenOf(userId, now);
        offerRepository.changeStatusForDonor(userId, NeedOffer.Status.PENDING, NeedOffer.Status.WITHDRAWN, now);
    }

    // ----- For the reports and admin modules -----

    /** Basic facts about a need, in any status. */
    @Transactional(readOnly = true)
    public NeedSummary getSummary(UUID needId) {
        return needRepository.findById(needId).map(NeedSummary::from).orElseThrow(NeedService::needNotFound);
    }

    /** One need in any status, with its waiting offers counted. The caller must already have checked it is an admin. */
    @Transactional(readOnly = true)
    public NeedResponse getForAdmin(UUID needId) {
        NeedRequest need = needRepository.findById(needId).orElseThrow(NeedService::needNotFound);
        Long pending = offerRepository.pendingCounts(List.of(needId)).getOrDefault(needId, 0L);
        return NeedResponse.from(need, nameOf(need.getRequesterId()), pending);
    }

    /**
     * An admin closes an open need, as if the person who asked had closed it: waiting offers are declined.
     * The caller must already have checked it is an admin.
     */
    @Transactional
    public NeedSummary closeByAdmin(UUID needId) {
        NeedRequest need = needRepository.findForUpdateById(needId).orElseThrow(NeedService::needNotFound);
        if (need.getStatus() != NeedRequest.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This need is no longer open");
        }
        need.close();
        needRepository.flush();
        NeedSummary summary = NeedSummary.from(need);
        offerRepository.changeStatusForNeed(
                needId, NeedOffer.Status.PENDING, NeedOffer.Status.DECLINED, Instant.now());
        return summary;
    }

    /** When a donor takes a listing down, its waiting offers are withdrawn in the same transaction. */
    @EventListener
    @Transactional
    public void onDonationRemoved(DonationRemovedEvent event) {
        offerRepository.withdrawPendingForDonation(event.donationId(), Instant.now());
    }

    // ----- Helpers -----

    private UserResponse requireCompleteProfile(String firebaseUid, String message) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        if (!caller.profileComplete()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return caller;
    }

    private static void requireRequester(NeedRequest need, UUID callerId) {
        if (!need.getRequesterId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the person who asked can do this");
        }
    }

    /** People whose needs the caller must not see: blocked either way, or suspended by an admin. */
    private Set<UUID> hiddenFor(UUID userId) {
        Set<UUID> hidden = new HashSet<>(blockedUsers.blockedEitherWay(userId));
        hidden.addAll(userService.suspendedUserIds());
        return hidden.isEmpty() ? Set.of(NO_USER) : hidden;
    }

    private String nameOf(UUID userId) {
        return userService.displayNamesByIds(List.of(userId)).getOrDefault(userId, UNKNOWN_NAME);
    }

    private Page<NeedResponse> toResponses(Page<NeedRequest> page, UUID callerId) {
        Set<UUID> requesterIds = new HashSet<>();
        List<UUID> ownNeedIds = new ArrayList<>();
        for (NeedRequest need : page.getContent()) {
            requesterIds.add(need.getRequesterId());
            if (need.getRequesterId().equals(callerId)) {
                ownNeedIds.add(need.getId());
            }
        }
        Map<UUID, String> names = requesterIds.isEmpty() ? Map.of() : userService.displayNamesByIds(requesterIds);
        Map<UUID, Long> pending = offerRepository.pendingCounts(ownNeedIds);
        return page.map(need -> NeedResponse.from(
                need,
                names.getOrDefault(need.getRequesterId(), UNKNOWN_NAME),
                need.getRequesterId().equals(callerId) ? pending.getOrDefault(need.getId(), 0L) : null));
    }

    private List<OfferResponse> toOfferResponses(List<NeedOffer> offers, Map<UUID, NeedRequest> needs) {
        Set<UUID> donorIds = new HashSet<>();
        Set<UUID> donationIds = new HashSet<>();
        for (NeedOffer offer : offers) {
            donorIds.add(offer.getDonorId());
            donationIds.add(offer.getDonationId());
        }
        Map<UUID, String> names = donorIds.isEmpty() ? Map.of() : userService.displayNamesByIds(donorIds);
        Map<UUID, DonationSummary> donations = donationService.summariesByIds(donationIds);
        Map<UUID, String> images = new HashMap<>();
        for (UUID donationId : donationIds) {
            DonationSummary summary = donations.get(donationId);
            if (summary != null && !"REMOVED".equals(summary.status())) {
                DonationResponse full = donationService.getOne(donationId);
                images.put(donationId, full.imageUrls().isEmpty() ? null : full.imageUrls().get(0));
            }
        }
        List<OfferResponse> responses = new ArrayList<>();
        for (NeedOffer offer : offers) {
            NeedRequest need = needs.get(offer.getNeedId());
            DonationSummary donation = donations.get(offer.getDonationId());
            responses.add(new OfferResponse(
                    offer.getId(),
                    offer.getNeedId(),
                    need == null ? "" : need.getTitle(),
                    offer.getDonationId(),
                    donation == null ? "" : donation.title(),
                    donation == null ? null : donation.status(),
                    images.get(offer.getDonationId()),
                    offer.getDonorId(),
                    names.getOrDefault(offer.getDonorId(), UNKNOWN_NAME),
                    offer.getMessage(),
                    offer.getStatus().name(),
                    offer.getCreatedAt()));
        }
        return responses;
    }

    private static ResponseStatusException needNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Need not found");
    }

    private static ResponseStatusException offerNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found");
    }

    private static ResponseStatusException alreadyOffered() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "You have already offered this item for this need");
    }
}
