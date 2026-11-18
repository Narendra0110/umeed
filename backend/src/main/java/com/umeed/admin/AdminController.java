package com.umeed.admin;

import com.umeed.common.PageResponse;
import com.umeed.reports.Report;
import com.umeed.reports.ReviewedReport;
import com.umeed.users.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/** The admin dashboard's API. Every endpoint answers 403 to anyone who is not an active admin. */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminService adminService;

    AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return adminService.me(jwt.getSubject());
    }

    @GetMapping("/stats")
    AdminViews.Stats stats(@AuthenticationPrincipal Jwt jwt) {
        return adminService.stats(jwt.getSubject());
    }

    /** Reports in one status (OPEN by default). */
    @GetMapping("/reports")
    PageResponse<AdminViews.ReportRow> reports(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "status", defaultValue = "OPEN") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminService.reports(jwt.getSubject(), reportStatus(status), Math.max(page, 0), pageSize(size));
    }

    /** Body: {"decision": "REVIEWED" or "DISMISSED", "note": "..."} */
    @PostMapping("/reports/{id}/review")
    ReviewedReport review(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminViews.ReviewRequest request) {
        return adminService.review(jwt.getSubject(), id, request.decision(), request.note());
    }

    @GetMapping("/donations/{id}")
    AdminViews.DonationDetail donation(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return adminService.donation(jwt.getSubject(), id);
    }

    /** Body: {"note": "why"} */
    @PostMapping("/donations/{id}/remove")
    AdminViews.DonationDetail removeDonation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminViews.NoteRequest request) {
        return adminService.removeDonation(jwt.getSubject(), id, request.note());
    }

    @GetMapping("/needs/{id}")
    AdminViews.NeedDetail need(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return adminService.need(jwt.getSubject(), id);
    }

    /** Body: {"note": "why"} */
    @PostMapping("/needs/{id}/close")
    AdminViews.NeedDetail closeNeed(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminViews.NoteRequest request) {
        return adminService.closeNeed(jwt.getSubject(), id, request.note());
    }

    /** Search by name or phone fragment; optional status ACTIVE or SUSPENDED. */
    @GetMapping("/users")
    PageResponse<AdminViews.UserRow> users(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "q", required = false) String text,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (text != null && text.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is too long");
        }
        return adminService.users(jwt.getSubject(), text, userStatus(status), Math.max(page, 0), pageSize(size));
    }

    @GetMapping("/users/{id}")
    AdminViews.UserDetail user(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return adminService.user(jwt.getSubject(), id);
    }

    /** Body: {"note": "why"} */
    @PostMapping("/users/{id}/suspend")
    AdminViews.UserDetail suspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminViews.NoteRequest request) {
        return adminService.suspend(jwt.getSubject(), id, request.note());
    }

    /** Body: {"note": "why"} */
    @PostMapping("/users/{id}/unsuspend")
    AdminViews.UserDetail unsuspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @Valid @RequestBody AdminViews.NoteRequest request) {
        return adminService.unsuspend(jwt.getSubject(), id, request.note());
    }

    /** The admin log, newest first. With targetId, only the steps about that report, listing, need or user. */
    @GetMapping("/actions")
    PageResponse<AdminViews.ActionRow> actions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "targetId", required = false) UUID targetId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminService.actions(jwt.getSubject(), targetId, Math.max(page, 0), pageSize(size));
    }

    private static int pageSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static Report.Status reportStatus(String value) {
        try {
            return Report.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown report status");
        }
    }

    private static String userStatus(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String status = value.toUpperCase(Locale.ROOT);
        if (!status.equals("ACTIVE") && !status.equals("SUSPENDED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user status");
        }
        return status;
    }
}
