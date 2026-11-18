package com.umeed.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One step an admin took. Never changed or deleted. */
@Entity
@Table(name = "admin_actions")
class AdminAction {

    enum Action { REVIEW_REPORT, DISMISS_REPORT, REMOVE_DONATION, CLOSE_NEED, SUSPEND_USER, UNSUSPEND_USER }

    enum TargetType { REPORT, DONATION, NEED, USER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false, updatable = false)
    private UUID adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminAction() {
        // Required by JPA
    }

    static AdminAction of(UUID adminId, Action action, TargetType targetType, UUID targetId, String note) {
        AdminAction entry = new AdminAction();
        entry.adminId = adminId;
        entry.action = action;
        entry.targetType = targetType;
        entry.targetId = targetId;
        entry.note = note == null || note.isBlank() ? null : note.strip();
        return entry;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
