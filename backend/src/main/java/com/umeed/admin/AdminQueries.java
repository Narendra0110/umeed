package com.umeed.admin;

import com.umeed.common.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Read-only views across modules for the admin dashboard. Plain SQL over the tables, so one screen needs one
 * query. Every change still goes through the owning module's service; nothing here writes.
 */
@Component
class AdminQueries {

    /** A person's name as the admin sees it. Deleted accounts have no name left. */
    private static final String NAME = """
            CASE WHEN %1$s.status = 'DELETED' THEN 'Deleted account'
                 ELSE COALESCE(NULLIF(%1$s.display_name, ''), 'Umeed user') END""";

    private static final String REPORT_SELECT = """
            SELECT r.id, r.reason, r.details, r.status, r.created_at, r.donation_id, r.need_id,
                   COALESCE(d.title, n.title) AS target_title,
                   COALESCE(d.status, n.status) AS target_status,
                   o.id AS owner_id, %s AS owner_name,
                   rep.id AS reporter_id, %s AS reporter_name,
                   (SELECT count(*) FROM reports x
                     WHERE x.donation_id = r.donation_id OR x.need_id = r.need_id) AS reports_on_target,
                   r.review_note, r.reviewed_at
              FROM reports r
              LEFT JOIN donations d ON d.id = r.donation_id
              LEFT JOIN need_requests n ON n.id = r.need_id
              JOIN users o ON o.id = COALESCE(d.donor_id, n.requester_id)
              JOIN users rep ON rep.id = r.reporter_id
            """.formatted(NAME.formatted("o"), NAME.formatted("rep"));

    private final NamedParameterJdbcTemplate jdbc;

    AdminQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    AdminViews.Stats stats() {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT count(*) FROM users WHERE status = 'ACTIVE') AS active_users,
                  (SELECT count(*) FROM users WHERE status = 'SUSPENDED') AS suspended_users,
                  (SELECT count(*) FROM users WHERE status <> 'DELETED'
                      AND created_at > now() - interval '7 days') AS new_users,
                  (SELECT count(*) FROM donations WHERE status = 'AVAILABLE') AS available,
                  (SELECT count(*) FROM donations WHERE status = 'RESERVED') AS reserved,
                  (SELECT count(*) FROM donations WHERE status = 'COMPLETED') AS completed,
                  (SELECT count(*) FROM donations WHERE status = 'REMOVED') AS removed,
                  (SELECT count(*) FROM donations WHERE created_at > now() - interval '7 days') AS new_listings,
                  (SELECT count(*) FROM need_requests WHERE status = 'OPEN') AS open_needs,
                  (SELECT count(*) FROM reports WHERE status = 'OPEN') AS open_reports
                """, new MapSqlParameterSource(), (rs, row) -> new AdminViews.Stats(
                rs.getLong("active_users"),
                rs.getLong("suspended_users"),
                rs.getLong("new_users"),
                rs.getLong("available"),
                rs.getLong("reserved"),
                rs.getLong("completed"),
                rs.getLong("removed"),
                rs.getLong("new_listings"),
                rs.getLong("open_needs"),
                rs.getLong("open_reports")));
    }

    /** Reports in one status. Open reports oldest first (they have waited longest); handled ones newest first. */
    @Transactional(readOnly = true)
    PageResponse<AdminViews.ReportRow> reports(String status, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource("status", status)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        String order = "OPEN".equals(status) ? "r.created_at ASC, r.id" : "r.reviewed_at DESC NULLS LAST, r.id";
        List<AdminViews.ReportRow> rows = jdbc.query(
                REPORT_SELECT + " WHERE r.status = :status ORDER BY " + order + " LIMIT :limit OFFSET :offset",
                params, AdminQueries::reportRow);
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM reports WHERE status = :status", params, Long.class);
        return page(rows, page, size, total == null ? 0 : total);
    }

    /** Every report about one listing, newest first. */
    @Transactional(readOnly = true)
    List<AdminViews.ReportRow> reportsOnDonation(UUID donationId) {
        return jdbc.query(REPORT_SELECT + " WHERE r.donation_id = :id ORDER BY r.created_at DESC, r.id",
                new MapSqlParameterSource("id", donationId), AdminQueries::reportRow);
    }

    /** Every report about one need, newest first. */
    @Transactional(readOnly = true)
    List<AdminViews.ReportRow> reportsOnNeed(UUID needId) {
        return jdbc.query(REPORT_SELECT + " WHERE r.need_id = :id ORDER BY r.created_at DESC, r.id",
                new MapSqlParameterSource("id", needId), AdminQueries::reportRow);
    }

    /**
     * Users matching a name or phone fragment, newest first. Deleted accounts are left out.
     * An empty status means any status.
     */
    @Transactional(readOnly = true)
    PageResponse<AdminViews.UserRow> users(String text, String status, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", likePattern(text))
                .addValue("status", status == null ? "" : status)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        String where = """
                 WHERE u.status <> 'DELETED'
                   AND (:status = '' OR u.status = :status)
                   AND (:pattern = '%' OR lower(u.display_name) LIKE :pattern OR u.phone_number LIKE :pattern)
                """;
        List<AdminViews.UserRow> rows = jdbc.query("""
                SELECT u.id, u.display_name, u.phone_number, u.role, u.status, u.created_at FROM users u
                """ + where + " ORDER BY u.created_at DESC, u.id LIMIT :limit OFFSET :offset",
                params, (rs, row) -> new AdminViews.UserRow(
                        uuid(rs, "id"),
                        rs.getString("display_name"),
                        rs.getString("phone_number"),
                        rs.getString("role"),
                        rs.getString("status"),
                        instant(rs, "created_at")));
        Long total = jdbc.queryForObject("SELECT count(*) FROM users u " + where, params, Long.class);
        return page(rows, page, size, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    Instant userCreatedAt(UUID userId) {
        return jdbc.queryForObject("SELECT created_at FROM users WHERE id = :id",
                new MapSqlParameterSource("id", userId), (rs, row) -> instant(rs, "created_at"));
    }

    @Transactional(readOnly = true)
    AdminViews.UserCounts userCounts(UUID userId) {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT count(*) FROM donations WHERE donor_id = :id AND status <> 'REMOVED') AS listings,
                  (SELECT count(*) FROM donations WHERE donor_id = :id AND status = 'AVAILABLE') AS available,
                  (SELECT count(*) FROM need_requests WHERE requester_id = :id AND status = 'OPEN') AS open_needs,
                  (SELECT count(*) FROM claims WHERE claimant_id = :id) AS claims_made,
                  (SELECT count(*) FROM reports r
                     LEFT JOIN donations d ON d.id = r.donation_id
                     LEFT JOIN need_requests n ON n.id = r.need_id
                    WHERE COALESCE(d.donor_id, n.requester_id) = :id) AS reports_against,
                  (SELECT count(*) FROM reports WHERE reporter_id = :id) AS reports_made
                """, new MapSqlParameterSource("id", userId), (rs, row) -> new AdminViews.UserCounts(
                rs.getLong("listings"),
                rs.getLong("available"),
                rs.getLong("open_needs"),
                rs.getLong("claims_made"),
                rs.getLong("reports_against"),
                rs.getLong("reports_made")));
    }

    /** Admin steps, newest first. With a target id, only the steps about that target. */
    @Transactional(readOnly = true)
    PageResponse<AdminViews.ActionRow> actions(UUID targetId, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource("target", targetId)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        String where = targetId == null ? "" : " WHERE a.target_id = :target";
        List<AdminViews.ActionRow> rows = jdbc.query("""
                SELECT a.id, a.admin_id, %s AS admin_name, a.action, a.target_type, a.target_id, a.note, a.created_at
                  FROM admin_actions a JOIN users u ON u.id = a.admin_id
                """.formatted(NAME.formatted("u")) + where
                        + " ORDER BY a.created_at DESC, a.id LIMIT :limit OFFSET :offset",
                params, (rs, row) -> new AdminViews.ActionRow(
                        uuid(rs, "id"),
                        uuid(rs, "admin_id"),
                        rs.getString("admin_name"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        uuid(rs, "target_id"),
                        rs.getString("note"),
                        instant(rs, "created_at")));
        Long total = jdbc.queryForObject("SELECT count(*) FROM admin_actions a" + where, params, Long.class);
        return page(rows, page, size, total == null ? 0 : total);
    }

    /** Lower-case "contains" pattern with LIKE wildcards escaped; "%" (matches everything) when empty. */
    static String likePattern(String text) {
        if (text == null || text.isBlank()) {
            return "%";
        }
        String escaped = text.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static AdminViews.ReportRow reportRow(ResultSet rs, int row) throws SQLException {
        return new AdminViews.ReportRow(
                uuid(rs, "id"),
                rs.getString("reason"),
                rs.getString("details"),
                rs.getString("status"),
                instant(rs, "created_at"),
                uuid(rs, "donation_id"),
                uuid(rs, "need_id"),
                rs.getString("target_title"),
                rs.getString("target_status"),
                uuid(rs, "owner_id"),
                rs.getString("owner_name"),
                uuid(rs, "reporter_id"),
                rs.getString("reporter_name"),
                rs.getLong("reports_on_target"),
                rs.getString("review_note"),
                instant(rs, "reviewed_at"));
    }

    private static <T> PageResponse<T> page(List<T> rows, int page, int size, long total) {
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(rows, page, size, total, totalPages, (long) (page + 1) * size < total);
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
