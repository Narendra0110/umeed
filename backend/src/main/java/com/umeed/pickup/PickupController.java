package com.umeed.pickup;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations/{donationId}/pickup")
class PickupController {

    private final PickupService pickupService;

    PickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping
    PickupResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable("donationId") UUID donationId) {
        return pickupService.getDetails(jwt.getSubject(), donationId);
    }

    @PostMapping("/confirm")
    PickupResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable("donationId") UUID donationId) {
        return pickupService.confirm(jwt.getSubject(), donationId);
    }

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable("donationId") UUID donationId) {
        pickupService.cancel(jwt.getSubject(), donationId);
    }
}
