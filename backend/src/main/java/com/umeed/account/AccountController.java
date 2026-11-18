package com.umeed.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccountController {

    private final AccountService accountService;

    AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Deletes the caller's own account. The app then deletes the Firebase sign-in and logs out. */
    @DeleteMapping("/api/v1/users/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMe(@AuthenticationPrincipal Jwt jwt) {
        accountService.deleteAccount(jwt.getSubject());
    }
}
