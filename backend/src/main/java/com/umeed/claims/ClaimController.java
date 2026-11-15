package com.umeed.claims;

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

import java.util.List;
import java.util.UUID;

@RestController
class ClaimController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ClaimService claimService;

    ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    /** Claim an item. The body is optional: {"message": "..."} */
    @PostMapping("/api/v1/donations/{donationId}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    ClaimResponse submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("donationId") UUID donationId,
            @Valid @RequestBody(required = false) CreateClaimRequest request) {
        return claimService.submit(jwt.getSubject(), donationId, request == null ? null : request.message());
    }

    /** The donor sees who claimed their item. */
    @GetMapping("/api/v1/donations/{donationId}/claims")
    List<ClaimResponse> listForDonation(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("donationId") UUID donationId) {
        return claimService.listForDonation(jwt.getSubject(), donationId);
    }

    @PostMapping("/api/v1/claims/{claimId}/accept")
    ClaimResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable("claimId") UUID claimId) {
        return claimService.accept(jwt.getSubject(), claimId);
    }

    @PostMapping("/api/v1/claims/{claimId}/withdraw")
    ClaimResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable("claimId") UUID claimId) {
        return claimService.withdraw(jwt.getSubject(), claimId);
    }

    @GetMapping("/api/v1/claims/mine")
    PageResponse<ClaimResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return claimService.listMine(jwt.getSubject(), pageRequest);
    }
}
