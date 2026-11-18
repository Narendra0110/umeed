package com.umeed.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        String phoneNumber = jwt.getClaimAsString("phone_number");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phone sign-in is required");
        }
        return userService.findOrRegister(jwt.getSubject(), phoneNumber);
    }

    @PutMapping("/me")
    UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateDisplayName(jwt.getSubject(), request.displayName());
    }

    /** The app's language for this account (F07). Body: {"language": "en" or "hi"} */
    @PutMapping("/me/language")
    UserResponse updateLanguage(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateLanguageRequest request) {
        return userService.updateLanguage(jwt.getSubject(), request.language());
    }
}
