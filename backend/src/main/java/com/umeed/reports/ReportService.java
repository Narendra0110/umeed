package com.umeed.reports;

import com.umeed.donations.DonationService;
import com.umeed.donations.DonationSummary;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final DonationService donationService;

    ReportService(ReportRepository reportRepository, UserService userService, DonationService donationService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.donationService = donationService;
    }

    /**
     * Records a report about a listing. The listing is not hidden automatically; reports are reviewed by hand.
     */
    @Transactional
    public ReportResponse report(String firebaseUid, UUID donationId, Report.Reason reason, String details) {
        UserResponse reporter = userService.requireActiveUser(firebaseUid);
        DonationSummary donation = donationService.getSummary(donationId);
        if (donation.donorId().equals(reporter.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot report your own listing");
        }
        if (reason == Report.Reason.OTHER && (details == null || details.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please describe the problem");
        }
        if (reportRepository.existsByDonationIdAndReporterId(donationId, reporter.id())) {
            throw alreadyReported();
        }
        try {
            Report report = reportRepository.saveAndFlush(Report.create(donationId, reporter.id(), reason, details));
            return ReportResponse.from(report);
        } catch (DataIntegrityViolationException e) {
            throw alreadyReported();
        }
    }

    private static ResponseStatusException alreadyReported() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this listing");
    }
}
