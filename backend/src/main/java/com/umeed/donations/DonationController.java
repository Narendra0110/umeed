package com.umeed.donations;

import com.umeed.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations")
class DonationController {

    private static final int MAX_PAGE_SIZE = 50;

    private final DonationService donationService;

    DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    /** One multipart form: the text fields of the donation plus 1 to 5 files in the part named "photos". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    DonationResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute CreateDonationRequest request,
            @RequestParam(name = "photos", required = false) List<MultipartFile> photos) {
        return donationService.create(jwt.getSubject(), request, photos == null ? List.of() : photos);
    }

    /** The feed. With lat and lng it is sorted nearest first; without them, newest first. */
    @GetMapping
    PageResponse<DonationResponse> feed(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "lat", required = false) Double latitude,
            @RequestParam(name = "lng", required = false) Double longitude) {
        PageRequest pageRequest = pageRequest(page, size);
        if (latitude == null && longitude == null) {
            return donationService.listFeed(pageRequest);
        }
        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send both lat and lng, or neither");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location");
        }
        return donationService.listFeedNearest(latitude, longitude, pageRequest);
    }

    @GetMapping("/mine")
    PageResponse<DonationResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return donationService.listMine(jwt.getSubject(), pageRequest(page, size));
    }

    @GetMapping("/{id}")
    DonationResponse getOne(@PathVariable("id") UUID id) {
        return donationService.getOne(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        donationService.remove(jwt.getSubject(), id);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
