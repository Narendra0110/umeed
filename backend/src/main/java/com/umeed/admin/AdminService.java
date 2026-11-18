package com.umeed.admin;

import com.umeed.common.PageResponse;
import com.umeed.donations.DonationService;
import com.umeed.needs.NeedService;
import com.umeed.reports.Report;
import com.umeed.reports.ReportService;
import com.umeed.reports.ReviewedReport;
import com.umeed.users.UserResponse;
import com.umeed.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The admin dashboard (F06). Every method first checks that the caller is an active admin; the role is read
 * from the database. Every change is made through the owning module and written to the admin log.
 */
@Service
class AdminService {

    private static final int HISTORY_SIZE = 20;

    private final UserService userService;
    private final DonationService donationService;
    private final NeedService needService;
    private final ReportService reportService;
    private final AdminQueries queries;
    private final AdminActionRepository actionRepository;

    AdminService(
            UserService userService,
            DonationService donationService,
            NeedService needService,
            ReportService reportService,
            AdminQueries queries,
            AdminActionRepository actionRepository) {
        this.userService = userService;
        this.donationService = donationService;
        this.needService = needService;
        this.reportService = reportService;
        this.queries = queries;
        this.actionRepository = actionRepository;
    }

    /** Who is logged in to the dashboard. Refused with 403 for anyone who is not an admin. */
    @Transactional(readOnly = true)
    UserResponse me(String firebaseUid) {
        return userService.requireAdmin(firebaseUid);
    }

    @Transactional(readOnly = true)
    AdminViews.Stats stats(String firebaseUid) {
        userService.requireAdmin(firebaseUid);
        return queries.stats();
    }

    // ----- Reports -----

    @Transactional(readOnly = true)
    PageResponse<AdminViews.ReportRow> reports(String firebaseUid, Report.Status status, int page, int size) {
        userService.requireAdmin(firebaseUid);
        return queries.reports(status.name(), page, size);
    }

    @Transactional
    ReviewedReport review(String firebaseUid, UUID reportId, AdminViews.Decision decision, String note) {
        UserResponse admin = userService.requireAdmin(firebaseUid);
        Report.Status status = decision == AdminViews.Decision.REVIEWED ? Report.Status.REVIEWED : Report.Status.DISMISSED;
        ReviewedReport report = reportService.review(reportId, status, admin.id(), note);
        log(admin, decision == AdminViews.Decision.REVIEWED
                ? AdminAction.Action.REVIEW_REPORT : AdminAction.Action.DISMISS_REPORT,
                AdminAction.TargetType.REPORT, reportId, note);
        return report;
    }

    // ----- Listings and needs -----

    @Transactional(readOnly = true)
    AdminViews.DonationDetail donation(String firebaseUid, UUID donationId) {
        userService.requireAdmin(firebaseUid);
        return new AdminViews.DonationDetail(
                donationService.getForAdmin(donationId), queries.reportsOnDonation(donationId));
    }

    /** Removes an available listing. Its open reports are marked reviewed with the same reason. */
    @Transactional
    AdminViews.DonationDetail removeDonation(String firebaseUid, UUID donationId, String note) {
        UserResponse admin = userService.requireAdmin(firebaseUid);
        donationService.removeByAdmin(donationId);
        reportService.resolveOpenForDonation(donationId, admin.id(), note);
        log(admin, AdminAction.Action.REMOVE_DONATION, AdminAction.TargetType.DONATION, donationId, note);
        return new AdminViews.DonationDetail(
                donationService.getForAdmin(donationId), queries.reportsOnDonation(donationId));
    }

    @Transactional(readOnly = true)
    AdminViews.NeedDetail need(String firebaseUid, UUID needId) {
        userService.requireAdmin(firebaseUid);
        return new AdminViews.NeedDetail(needService.getForAdmin(needId), queries.reportsOnNeed(needId));
    }

    /** Closes an open need. Its open reports are marked reviewed with the same reason. */
    @Transactional
    AdminViews.NeedDetail closeNeed(String firebaseUid, UUID needId, String note) {
        UserResponse admin = userService.requireAdmin(firebaseUid);
        needService.closeByAdmin(needId);
        reportService.resolveOpenForNeed(needId, admin.id(), note);
        log(admin, AdminAction.Action.CLOSE_NEED, AdminAction.TargetType.NEED, needId, note);
        return new AdminViews.NeedDetail(needService.getForAdmin(needId), queries.reportsOnNeed(needId));
    }

    // ----- Users -----

    @Transactional(readOnly = true)
    PageResponse<AdminViews.UserRow> users(String firebaseUid, String text, String status, int page, int size) {
        userService.requireAdmin(firebaseUid);
        return queries.users(text, status, page, size);
    }

    @Transactional(readOnly = true)
    AdminViews.UserDetail user(String firebaseUid, UUID userId) {
        userService.requireAdmin(firebaseUid);
        return userDetail(userService.getForAdmin(userId));
    }

    /** Suspends a user: they can no longer act, and their listings and needs disappear from everyone's lists. */
    @Transactional
    AdminViews.UserDetail suspend(String firebaseUid, UUID userId, String note) {
        UserResponse admin = userService.requireAdmin(firebaseUid);
        UserResponse user = userService.suspend(userId);
        log(admin, AdminAction.Action.SUSPEND_USER, AdminAction.TargetType.USER, userId, note);
        return userDetail(user);
    }

    /** Lifts a suspension. Nothing was deleted, so everything of theirs comes back. */
    @Transactional
    AdminViews.UserDetail unsuspend(String firebaseUid, UUID userId, String note) {
        UserResponse admin = userService.requireAdmin(firebaseUid);
        UserResponse user = userService.unsuspend(userId);
        log(admin, AdminAction.Action.UNSUSPEND_USER, AdminAction.TargetType.USER, userId, note);
        return userDetail(user);
    }

    // ----- Log -----

    @Transactional(readOnly = true)
    PageResponse<AdminViews.ActionRow> actions(String firebaseUid, UUID targetId, int page, int size) {
        userService.requireAdmin(firebaseUid);
        return queries.actions(targetId, page, size);
    }

    private AdminViews.UserDetail userDetail(UserResponse user) {
        actionRepository.flush();
        return new AdminViews.UserDetail(
                user,
                queries.userCreatedAt(user.id()),
                queries.userCounts(user.id()),
                queries.actions(user.id(), 0, HISTORY_SIZE).items());
    }

    private void log(UserResponse admin, AdminAction.Action action, AdminAction.TargetType type, UUID targetId,
                     String note) {
        actionRepository.saveAndFlush(AdminAction.of(admin.id(), action, type, targetId, note));
    }
}
