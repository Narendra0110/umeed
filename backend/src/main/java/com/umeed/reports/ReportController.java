package com.umeed.reports;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class ReportController {

    private final ReportService reportService;

    ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Report a listing. Body: {"reason": "PROHIBITED_ITEM", "details": "..."} */
    @PostMapping("/api/v1/donations/{donationId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    ReportResponse report(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("donationId") UUID donationId,
            @Valid @RequestBody CreateReportRequest request) {
        return reportService.report(jwt.getSubject(), donationId, request.reason(), request.details());
    }
}
