package com.umeed.donations;

import com.umeed.categories.CategoryService;
import com.umeed.common.PageResponse;
import com.umeed.storage.FileStorage;
import com.umeed.users.BlockedUsers;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    /** The all-zero UUID. Never generated for a donation; used only to keep "not in" lists non-empty. */
    private static final UUID NO_DONATION = new UUID(0L, 0L);

    private final DonationRepository donationRepository;
    private final DonationImageRepository donationImageRepository;
    private final FileStorage fileStorage;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final ClaimedDonations claimedDonations;
    private final BlockedUsers blockedUsers;
    private final DonationSkipRepository donationSkipRepository;

    DonationService(
            DonationRepository donationRepository,
            DonationImageRepository donationImageRepository,
            DonationSkipRepository donationSkipRepository,
            FileStorage fileStorage,
            UserService userService,
            CategoryService categoryService,
            ApplicationEventPublisher eventPublisher,
            ClaimedDonations claimedDonations,
            BlockedUsers blockedUsers) {
        this.donationRepository = donationRepository;
        this.donationImageRepository = donationImageRepository;
        this.donationSkipRepository = donationSkipRepository;
        this.fileStorage = fileStorage;
        this.userService = userService;
        this.categoryService = categoryService;
        this.eventPublisher = eventPublisher;
        this.claimedDonations = claimedDonations;
        this.blockedUsers = blockedUsers;
    }

    /**
     * Posts a donation with its photos. The donor is always taken from the verified identity.
     */
    @Transactional
    public DonationResponse create(String firebaseUid, CreateDonationRequest request, List<MultipartFile> photos) {
        UserResponse donor = userService.requireActiveUser(firebaseUid);
        if (!donor.profileComplete()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Complete your profile before posting a donation");
        }
        if (!categoryService.isActiveCategory(request.categoryId())) {
            throw badRequest("Unknown category");
        }
        List<String> extensions = checkPhotos(photos);

        Donation donation = donationRepository.save(Donation.create(
                donor.id(),
                request.categoryId(),
                request.title(),
                request.description(),
                request.itemCondition(),
                request.city(),
                request.area(),
                request.latitude(),
                request.longitude(),
                request.pickupAddress()));

        List<String> storedKeys = new ArrayList<>();
        try {
            for (int position = 0; position < photos.size(); position++) {
                String key = newStorageKey(extensions.get(position));
                storedKeys.add(key);
                try (InputStream content = photos.get(position).getInputStream()) {
                    fileStorage.store(key, content);
                }
                donationImageRepository.save(DonationImage.of(donation.getId(), key, position));
            }
            donationImageRepository.flush();
        } catch (IOException e) {
            deleteQuietly(storedKeys);
            log.error("Could not store photos for a new donation", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the photos. Please try again.");
        } catch (RuntimeException e) {
            deleteQuietly(storedKeys);
            throw e;
        }

        List<String> imageUrls = storedKeys.stream().map(fileStorage::urlFor).toList();
        return DonationResponse.from(donation, donor.displayName(), imageUrls);
    }

    /**
     * The feed: available donations the caller can claim, newest first. The caller's own donations,
     * the ones they have already claimed (in any claim status), the ones they marked "Not interested"
     * and the items of people blocked either way are left out.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listFeed(String firebaseUid, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<Donation> page = donationRepository.findClaimableNewestFirst(
                caller.id(), alreadyClaimedBy(caller.id()), hiddenDonorsFor(caller.id()), pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * The same feed sorted by distance from the caller's location, nearest first. The location is not stored.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listFeedNearest(
            String firebaseUid, double latitude, double longitude, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Page<Donation> page = donationRepository.findClaimableNearestFirst(
                caller.id(), alreadyClaimedBy(caller.id()), hiddenDonorsFor(caller.id()),
                latitude, longitude, pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * Search with filters (F05). Same exclusions as the feed, except that items marked "Not interested"
     * are still found: a search is deliberate. With a location, only items within maxKm, nearest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> search(String firebaseUid, DonationSearch filters, Pageable pageable) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Set<UUID> claimed = claimedDonations.donationIdsClaimedBy(caller.id());
        Set<UUID> excluded = claimed.isEmpty() ? Set.of(NO_DONATION) : claimed;
        String text = DonationSearch.likePattern(filters.text());
        String city = DonationSearch.likePattern(filters.city());
        String category = filters.categoryId() == null ? "" : filters.categoryId().toString();
        Page<Donation> page = filters.hasLocation()
                ? donationRepository.searchNearestFirst(
                        caller.id(), excluded, hiddenDonorsFor(caller.id()), text, category, city,
                        filters.latitude(), filters.longitude(),
                        filters.maxKm() == null ? DonationSearch.ANY_DISTANCE_KM : filters.maxKm(),
                        pageable)
                : donationRepository.searchNewestFirst(
                        caller.id(), excluded, hiddenDonorsFor(caller.id()), text, category, city, pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * "Not interested": the item no longer appears on the caller's Home. Marking twice changes nothing.
     */
    @Transactional
    public void skip(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        if (!donationSkipRepository.existsById(new DonationSkip.Key(caller.id(), donationId))) {
            donationSkipRepository.saveAndFlush(DonationSkip.of(caller.id(), donationId));
        }
    }

    /** "Show hidden items again": clears the caller's "Not interested" list. */
    @Transactional
    public void clearSkips(String firebaseUid) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        donationSkipRepository.deleteAllByUserId(caller.id());
    }

    /** For account deletion: forgets the user's "Not interested" list. */
    @Transactional
    public void forgetSkipsOf(UUID userId) {
        donationSkipRepository.deleteAllByUserId(userId);
    }

    /**
     * The ids to leave out of the caller's Home: items they already claimed and items they marked
     * "Not interested". SQL "not in ()" with an empty list is invalid, so an empty result is replaced
     * by one id that no donation can have (donation ids are random UUIDs).
     */
    private Set<UUID> alreadyClaimedBy(UUID userId) {
        Set<UUID> excluded = new HashSet<>(claimedDonations.donationIdsClaimedBy(userId));
        excluded.addAll(donationSkipRepository.findDonationIdsByUserId(userId));
        return excluded.isEmpty() ? Set.of(NO_DONATION) : excluded;
    }

    /**
     * Donors whose items the caller must not see: blocked either way, or suspended by an admin (F06).
     * Same placeholder rule as alreadyClaimedBy.
     */
    private Set<UUID> hiddenDonorsFor(UUID userId) {
        Set<UUID> hidden = new HashSet<>(blockedUsers.blockedEitherWay(userId));
        hidden.addAll(userService.suspendedUserIds());
        return hidden.isEmpty() ? Set.of(NO_DONATION) : hidden;
    }

    /**
     * The caller's own donations in every status except removed, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listMine(String firebaseUid, Pageable pageable) {
        UserResponse donor = userService.requireActiveUser(firebaseUid);
        Page<Donation> page = donationRepository.findByDonorIdAndStatusNotOrderByCreatedAtDescIdDesc(
                donor.id(), Donation.Status.REMOVED, pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * Takes a listing down. Only its donor may do this, and only while it is still available.
     */
    @Transactional
    public void remove(String firebaseUid, UUID donationId) {
        UserResponse caller = userService.requireActiveUser(firebaseUid);
        Donation donation = donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        if (!donation.getDonorId().equals(caller.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor can remove this donation");
        }
        if (donation.getStatus() != Donation.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an available donation can be removed");
        }
        donation.markRemoved();
        eventPublisher.publishEvent(new DonationRemovedEvent(donationId));
    }

    // ----- For the admin module. The caller must already have checked that the person is an admin. -----

    /**
     * An admin takes a listing down, with the same effects as the donor removing it: waiting claims are
     * rejected and waiting offers withdrawn. Only an available listing can be removed; a reserved one has a
     * pickup in progress, which its two people finish or cancel.
     */
    @Transactional
    public DonationSummary removeByAdmin(UUID donationId) {
        Donation donation = lockExisting(donationId);
        if (donation.getStatus() != Donation.Status.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only an available listing can be removed. This one is " + donation.getStatus());
        }
        donation.markRemoved();
        DonationSummary summary = DonationSummary.from(donation);
        // Saved before the event: its listeners run bulk updates that detach loaded entities.
        donationRepository.flush();
        eventPublisher.publishEvent(new DonationRemovedEvent(donationId));
        return summary;
    }

    /** One donation in any status, including removed, with its photos if they still exist. */
    @Transactional(readOnly = true)
    public DonationResponse getForAdmin(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        return toResponse(donation);
    }

    // ----- For other modules (claims, pickup). These never expose private details. -----

    /**
     * Basic facts about a donation. Removed donations are treated as not found.
     */
    @Transactional(readOnly = true)
    public DonationSummary getSummary(UUID donationId) {
        return donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .map(DonationSummary::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
    }

    /**
     * Confirms a donation can be claimed and holds it steady until the caller's transaction ends,
     * so it cannot be reserved or removed while a claim is being saved.
     */
    @Transactional
    public DonationSummary lockClaimable(UUID donationId) {
        Donation donation = donationRepository.findForShareById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        if (donation.getStatus() != Donation.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This donation is no longer available");
        }
        return DonationSummary.from(donation);
    }

    /**
     * Reserves a donation for an accepted claim. Only its donor may do this, and only while it is available.
     * The row is locked, so two acceptances cannot both succeed.
     */
    @Transactional
    public DonationSummary reserve(UUID donationId, UUID donorUserId) {
        Donation donation = donationRepository.findForUpdateById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        if (!donation.getDonorId().equals(donorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor can accept a claim");
        }
        if (donation.getStatus() != Donation.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This donation is no longer available");
        }
        donation.markReserved();
        return DonationSummary.from(donation);
    }

    /**
     * PRIVATE pickup details. The caller must already have checked who is asking.
     */
    @Transactional(readOnly = true)
    public DonationPickupDetails getPickupDetails(UUID donationId) {
        return donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .map(DonationPickupDetails::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
    }

    /**
     * PRIVATE pickup details, with the donation locked until the caller's transaction ends.
     * Used before confirming or cancelling, so the check of who is asking stays true while the change is made.
     */
    @Transactional
    public DonationPickupDetails lockForPickup(UUID donationId) {
        return DonationPickupDetails.from(lockExisting(donationId));
    }

    /**
     * One side confirms the handover. When both have confirmed, the donation becomes completed.
     * Confirming twice, or after completion, changes nothing.
     */
    @Transactional
    public DonationPickupDetails confirmHandover(UUID donationId, boolean byDonor) {
        Donation donation = lockExisting(donationId);
        if (donation.getStatus() == Donation.Status.COMPLETED) {
            return DonationPickupDetails.from(donation);
        }
        if (donation.getStatus() != Donation.Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This donation is not reserved");
        }
        donation.confirmHandover(byDonor);
        return DonationPickupDetails.from(donation);
    }

    /**
     * A reservation is cancelled: the donation goes back to the feed.
     */
    @Transactional
    public void reopen(UUID donationId) {
        Donation donation = lockExisting(donationId);
        if (donation.getStatus() != Donation.Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a reserved donation can be cancelled");
        }
        donation.reopen();
    }

    /**
     * The ids of every donation this donor posted, in any status. Used to close claims after a block.
     */
    @Transactional(readOnly = true)
    public List<UUID> idsByDonor(UUID donorId) {
        return donationRepository.findIdsByDonorId(donorId);
    }

    /**
     * The ids of this donor's reserved donations, that is, the ones with a pickup in progress.
     */
    @Transactional(readOnly = true)
    public List<UUID> reservedIdsByDonor(UUID donorId) {
        return donationRepository.findIdsByDonorIdAndStatus(donorId, Donation.Status.RESERVED);
    }

    /**
     * Account deletion, donor side. Refused while one of the donor's items is reserved. Otherwise:
     * available listings are removed (their open claims are closed through the usual event), every
     * pickup address is erased, and the photos of all removed listings are deleted. Photos of items
     * already handed over stay, because they belong to the receiver's history too.
     * The rows stay locked until the caller's transaction ends, so no claim can be accepted meanwhile.
     */
    @Transactional
    public void eraseDonor(UUID donorId) {
        List<Donation> donations = donationRepository.findAllForUpdateByDonorId(donorId);
        for (Donation donation : donations) {
            if (donation.getStatus() == Donation.Status.RESERVED) {
                throw pickupInProgress();
            }
        }
        List<UUID> newlyRemovedIds = new ArrayList<>();
        List<UUID> removedIds = new ArrayList<>();
        for (Donation donation : donations) {
            if (donation.getStatus() == Donation.Status.AVAILABLE) {
                donation.markRemoved();
                newlyRemovedIds.add(donation.getId());
            }
            if (donation.getStatus() == Donation.Status.REMOVED) {
                removedIds.add(donation.getId());
            }
            donation.erasePickupAddress();
        }
        // Saved before the events: their listeners run bulk updates that detach loaded entities.
        donationRepository.flush();
        for (UUID donationId : newlyRemovedIds) {
            eventPublisher.publishEvent(new DonationRemovedEvent(donationId));
        }
        if (!removedIds.isEmpty()) {
            List<DonationImage> images =
                    donationImageRepository.findByDonationIdInOrderByDonationIdAscSortOrderAsc(removedIds);
            List<String> keys = images.stream().map(DonationImage::getStorageKey).toList();
            donationImageRepository.deleteAllInBatch(images);
            deleteFilesAfterCommit(keys);
        }
    }

    /** The error used whenever an account cannot be deleted because of an unfinished pickup. */
    public static ResponseStatusException pickupInProgress() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "Finish or cancel your pickups before deleting your account");
    }

    /**
     * Files are deleted only once the database change is saved, so a failed deletion never leaves
     * listings pointing at missing photos.
     */
    private void deleteFilesAfterCommit(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(keys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(keys);
            }
        });
    }

    private Donation lockExisting(UUID donationId) {
        return donationRepository.findForUpdateById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
    }

    /**
     * Summaries (title, status) for the given donation ids, including reserved, completed and removed ones.
     */
    @Transactional(readOnly = true)
    public Map<UUID, DonationSummary> summariesByIds(Collection<UUID> donationIds) {
        Map<UUID, DonationSummary> summaries = new HashMap<>();
        for (Donation donation : donationRepository.findAllById(donationIds)) {
            summaries.put(donation.getId(), DonationSummary.from(donation));
        }
        return summaries;
    }

    /**
     * One donation. Removed donations are treated as not found.
     */
    @Transactional(readOnly = true)
    public DonationResponse getOne(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        return toResponse(donation);
    }

    private DonationResponse toResponse(Donation donation) {
        String donorName = userService.displayNamesByIds(List.of(donation.getDonorId()))
                .getOrDefault(donation.getDonorId(), "Umeed user");
        List<String> imageUrls = donationImageRepository.findByDonationIdOrderBySortOrderAsc(donation.getId()).stream()
                .map(image -> fileStorage.urlFor(image.getStorageKey()))
                .toList();
        return DonationResponse.from(donation, donorName, imageUrls);
    }

    private Page<DonationResponse> toResponses(Page<Donation> page) {
        List<Donation> donations = page.getContent();
        if (donations.isEmpty()) {
            return page.map(donation -> DonationResponse.from(donation, "Umeed user", List.of()));
        }

        Set<UUID> donorIds = new HashSet<>();
        List<UUID> donationIds = new ArrayList<>();
        for (Donation donation : donations) {
            donorIds.add(donation.getDonorId());
            donationIds.add(donation.getId());
        }

        Map<UUID, String> donorNames = userService.displayNamesByIds(donorIds);
        Map<UUID, List<String>> imageUrls = new HashMap<>();
        for (DonationImage image : donationImageRepository.findByDonationIdInOrderByDonationIdAscSortOrderAsc(donationIds)) {
            imageUrls.computeIfAbsent(image.getDonationId(), id -> new ArrayList<>())
                    .add(fileStorage.urlFor(image.getStorageKey()));
        }

        return page.map(donation -> DonationResponse.from(
                donation,
                donorNames.getOrDefault(donation.getDonorId(), "Umeed user"),
                imageUrls.getOrDefault(donation.getId(), List.of())));
    }

    /** Checks the number, size and real type of the photos. Returns one file extension per photo. */
    private static List<String> checkPhotos(List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty() || photos.size() > MAX_PHOTOS) {
            throw badRequest("Add between 1 and 5 photos");
        }
        List<String> extensions = new ArrayList<>();
        for (MultipartFile photo : photos) {
            if (photo.isEmpty()) {
                throw badRequest("A photo is empty");
            }
            if (photo.getSize() > MAX_PHOTO_BYTES) {
                throw badRequest("Each photo must be 5 MB or smaller");
            }
            String extension;
            try {
                extension = detectImageExtension(photo);
            } catch (IOException e) {
                throw badRequest("A photo could not be read");
            }
            if (extension == null) {
                throw badRequest("Photos must be JPEG, PNG or WebP images");
            }
            extensions.add(extension);
        }
        return extensions;
    }

    /**
     * Looks at the first bytes of the file, not at the name or the type the client claims,
     * so a renamed non-image file is rejected.
     */
    private static String detectImageExtension(MultipartFile photo) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream content = photo.getInputStream()) {
            read = content.readNBytes(header, 0, header.length);
        }
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return "png";
        }
        if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static String newStorageKey(String extension) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return String.format("donations/%d/%02d/%s.%s",
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
    }

    private void deleteQuietly(List<String> keys) {
        for (String key : keys) {
            try {
                fileStorage.delete(key);
            } catch (IOException | RuntimeException e) {
                log.warn("Could not remove stored file {}", key, e);
            }
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
