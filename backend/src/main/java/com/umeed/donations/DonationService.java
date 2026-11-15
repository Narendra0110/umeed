package com.umeed.donations;

import com.umeed.categories.CategoryService;
import com.umeed.common.PageResponse;
import com.umeed.storage.FileStorage;
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

    private final DonationRepository donationRepository;
    private final DonationImageRepository donationImageRepository;
    private final FileStorage fileStorage;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ApplicationEventPublisher eventPublisher;

    DonationService(
            DonationRepository donationRepository,
            DonationImageRepository donationImageRepository,
            FileStorage fileStorage,
            UserService userService,
            CategoryService categoryService,
            ApplicationEventPublisher eventPublisher) {
        this.donationRepository = donationRepository;
        this.donationImageRepository = donationImageRepository;
        this.fileStorage = fileStorage;
        this.userService = userService;
        this.categoryService = categoryService;
        this.eventPublisher = eventPublisher;
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
     * The feed: available donations, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listFeed(Pageable pageable) {
        Page<Donation> page = donationRepository.findByStatusOrderByCreatedAtDesc(Donation.Status.AVAILABLE, pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * The feed sorted by distance from the caller's location, nearest first. The location is not stored.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listFeedNearest(double latitude, double longitude, Pageable pageable) {
        Page<Donation> page = donationRepository.findAvailableNearestFirst(latitude, longitude, pageable);
        return PageResponse.of(toResponses(page));
    }

    /**
     * The caller's own donations in every status except removed, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> listMine(String firebaseUid, Pageable pageable) {
        UserResponse donor = userService.requireActiveUser(firebaseUid);
        Page<Donation> page = donationRepository.findByDonorIdAndStatusNotOrderByCreatedAtDesc(
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
     * Titles for the given donation ids, including reserved, completed and removed ones.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> titlesByIds(Collection<UUID> donationIds) {
        Map<UUID, String> titles = new HashMap<>();
        for (Donation donation : donationRepository.findAllById(donationIds)) {
            titles.put(donation.getId(), donation.getTitle());
        }
        return titles;
    }

    /**
     * One donation. Removed donations are treated as not found.
     */
    @Transactional(readOnly = true)
    public DonationResponse getOne(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .filter(found -> found.getStatus() != Donation.Status.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));
        String donorName = userService.displayNamesByIds(List.of(donation.getDonorId()))
                .getOrDefault(donation.getDonorId(), "Umeed user");
        List<String> imageUrls = donationImageRepository.findByDonationIdOrderBySortOrderAsc(donationId).stream()
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
