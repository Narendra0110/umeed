package com.umeed.notifications;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** The app calls this after login and whenever Firebase gives the phone a new token. */
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
        deviceService.register(jwt.getSubject(), request.token(), request.platform());
    }

    /** The app calls this just before logging out. A POST, because the token is too long for an address. */
    @PostMapping("/unregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unregister(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
        deviceService.unregister(jwt.getSubject(), request.token());
    }
}
