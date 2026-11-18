package com.umeed.needs;

import com.umeed.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
class NeedController {

    private static final int MAX_PAGE_SIZE = 50;

    private final NeedService needService;

    NeedController(NeedService needService) {
        this.needService = needService;
    }

    @PostMapping("/api/v1/needs")
    @ResponseStatus(HttpStatus.CREATED)
    NeedResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateNeedRequest request) {
        return needService.create(jwt.getSubject(), request);
    }

    /** Open needs the caller can help with. With lat and lng, nearest first; without them, newest first. */
    @GetMapping("/api/v1/needs")
    PageResponse<NeedResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "lat", required = false) Double latitude,
            @RequestParam(name = "lng", required = false) Double longitude) {
        PageRequest pageRequest = pageRequest(page, size);
        if (latitude == null && longitude == null) {
            return needService.listOpen(jwt.getSubject(), pageRequest);
        }
        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send both lat and lng, or neither");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location");
        }
        return needService.listOpenNearest(jwt.getSubject(), latitude, longitude, pageRequest);
    }

    @GetMapping("/api/v1/needs/mine")
    PageResponse<NeedResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return needService.listMine(jwt.getSubject(), pageRequest(page, size));
    }

    @GetMapping("/api/v1/needs/{id}")
    NeedResponse getOne(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return needService.getOne(jwt.getSubject(), id);
    }

    @PostMapping("/api/v1/needs/{id}/close")
    NeedResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return needService.close(jwt.getSubject(), id);
    }

    @PostMapping("/api/v1/needs/{id}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    OfferResponse offer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateOfferRequest request) {
        return needService.offer(jwt.getSubject(), id, request.donationId(), request.message());
    }

    @GetMapping("/api/v1/needs/{id}/offers")
    List<OfferResponse> offers(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return needService.listOffers(jwt.getSubject(), id);
    }

    @GetMapping("/api/v1/offers/mine")
    PageResponse<OfferResponse> myOffers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return needService.listMyOffers(jwt.getSubject(), pageRequest(page, size));
    }

    @PostMapping("/api/v1/offers/{id}/accept")
    AcceptOfferResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return needService.accept(jwt.getSubject(), id);
    }

    @PostMapping("/api/v1/offers/{id}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        needService.withdraw(jwt.getSubject(), id);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
