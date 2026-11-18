package com.umeed.admin;

import com.umeed.donations.DonationResponse;
import com.umeed.needs.NeedResponse;
import com.umeed.users.UserResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** What the admin endpoints send and receive. */
final class AdminViews {

    private AdminViews() {
    }

    record Stats(
            long activeUsers,
            long suspendedUsers,
            long newUsersLast7Days,
            long availableListings,
            long reservedListings,
            long completedHandovers,
            long removedListings,
            long newListingsLast7Days,
            long openNeeds,
            long openReports) {
    }

    /** One report, with what it is about and who is involved. Exactly one of donationId and needId is set. */
    record ReportRow(
            UUID id,
            String reason,
            String details,
            String status,
            Instant createdAt,
            UUID donationId,
            UUID needId,
            String targetTitle,
            String targetStatus,
            UUID ownerId,
            String ownerName,
            UUID reporterId,
            String reporterName,
            long reportsOnTarget,
            String reviewNote,
            Instant reviewedAt) {
    }

    record UserRow(
            UUID id,
            String displayName,
            String phoneNumber,
            String role,
            String status,
            Instant createdAt) {
    }

    record UserCounts(
            long listings,
            long availableListings,
            long openNeeds,
            long claimsMade,
            long reportsAgainst,
            long reportsMade) {
    }

    record UserDetail(UserResponse user, Instant createdAt, UserCounts counts, List<ActionRow> history) {
    }

    record DonationDetail(DonationResponse donation, List<ReportRow> reports) {
    }

    record NeedDetail(NeedResponse need, List<ReportRow> reports) {
    }

    record ActionRow(
            UUID id,
            UUID adminId,
            String adminName,
            String action,
            String targetType,
            UUID targetId,
            String note,
            Instant createdAt) {
    }

    record ReviewRequest(
            @NotNull(message = "Decision is required")
            Decision decision,

            @Size(max = 500, message = "The note must be at most 500 characters")
            String note) {
    }

    enum Decision { REVIEWED, DISMISSED }

    /** A reason is required for every step that affects a person. */
    record NoteRequest(
            @NotBlank(message = "Please give a reason")
            @Size(max = 500, message = "The reason must be at most 500 characters")
            String note) {
    }
}
