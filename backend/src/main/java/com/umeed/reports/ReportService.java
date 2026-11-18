package com.umeed.reports;

import com.umeed.donations.DonationService;
import com.umeed.donations.DonationSummary;
import com.umeed.needs.NeedService;
import com.umeed.needs.NeedSummary;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final DonationService donationService;
    private final NeedService needService;

    ReportService(
            ReportRepository reportRepository,
            UserService userService,
            DonationService donationService,
            NeedService needService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.donationService = donationService;
        this.needService = needService;
    }

    /**
     * Records a report about a listing. The listing is not hidden automatically; an admin reviews it.
     */
    @Transactional
    public ReportResponse report(String firebaseUid, UUID donationId, Report.Reason reason, String details) {
        UserResponse reporter = userService.requireActiveUser(firebaseUid);
        DonationSummary donation = donationService.getSummary(donationId);
        if (donation.donorId().equals(reporter.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot report your own listing");
        }
        requireDetailsForOther(reason, details);
        if (reportRepository.existsByDonationIdAndReporterId(donationId, reporter.id())) {
            throw alreadyReported("listing");
        }
        return save(Report.aboutDonation(donationId, reporter.id(), reason, details), "listing");
    }

    /**
     * Records a report about a need (F06). Same rules as for a listing.
     */
    @Transactional
    public ReportResponse reportNeed(String firebaseUid, UUID needId, Report.Reason reason, String details) {
        UserResponse reporter = userService.requireActiveUser(firebaseUid);
        NeedSummary need = needService.getSummary(needId);
        if (need.requesterId().equals(reporter.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot report your own need");
        }
        requireDetailsForOther(reason, details);
        if (reportRepository.existsByNeedIdAndReporterId(needId, reporter.id())) {
            throw alreadyReported("need");
        }
        return save(Report.aboutNeed(needId, reporter.id(), reason, details), "need");
    }

    // ----- For the admin module. The caller must already have checked that the person is an admin. -----

    /**
     * Closes one open report as REVIEWED or DISMISSED. Returns the report as it is now.
     */
    @Transactional
    public ReviewedReport review(UUID reportId, Report.Status decision, UUID adminId, String note) {
        if (decision == Report.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose REVIEWED or DISMISSED");
        }
        Report report = reportRepository.findForUpdateById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (report.getStatus() != Report.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report has already been handled");
        }
        report.review(decision, adminId, note);
        return ReviewedReport.from(report);
    }

    /** After an admin removes a listing, its open reports are marked REVIEWED with the same note. */
    @Transactional
    public int resolveOpenForDonation(UUID donationId, UUID adminId, String note) {
        return resolve(reportRepository.findByDonationIdAndStatus(donationId, Report.Status.OPEN), adminId, note);
    }

    /** After an admin closes a need, its open reports are marked REVIEWED with the same note. */
    @Transactional
    public int resolveOpenForNeed(UUID needId, UUID adminId, String note) {
        return resolve(reportRepository.findByNeedIdAndStatus(needId, Report.Status.OPEN), adminId, note);
    }

    private static int resolve(List<Report> reports, UUID adminId, String note) {
        reports.forEach(report -> report.review(Report.Status.REVIEWED, adminId, note));
        return reports.size();
    }

    private ReportResponse save(Report report, String what) {
        try {
            return ReportResponse.from(reportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException e) {
            throw alreadyReported(what);
        }
    }

    private static void requireDetailsForOther(Report.Reason reason, String details) {
        if (reason == Report.Reason.OTHER && (details == null || details.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please describe the problem");
        }
    }

    private static ResponseStatusException alreadyReported(String what) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this " + what);
    }
}
