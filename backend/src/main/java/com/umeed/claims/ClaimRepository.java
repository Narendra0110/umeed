package com.umeed.claims;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface ClaimRepository extends JpaRepository<Claim, UUID> {

    boolean existsByDonationIdAndClaimantId(UUID donationId, UUID claimantId);

    /** Oldest first. Two claims made at the same instant are ordered by id, so the order never changes between calls. */
    List<Claim> findByDonationIdOrderByCreatedAtAscIdAsc(UUID donationId);

    Page<Claim> findByClaimantIdOrderByCreatedAtDesc(UUID claimantId, Pageable pageable);

    Optional<Claim> findFirstByDonationIdAndStatus(UUID donationId, Claim.Status status);

    /** Every donation this user has claimed, whatever the claim's status. */
    @Query("select c.donationId from Claim c where c.claimantId = :claimantId")
    Set<UUID> findDonationIdsByClaimantId(@Param("claimantId") UUID claimantId);

    /**
     * Changes one claim's status only if it currently has the expected status.
     * Returns 1 if it changed and 0 if it did not, so a claim that was changed by someone else is never overwritten.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Claim c set c.status = :newStatus, c.updatedAt = :now where c.id = :id and c.status = :expected")
    int changeStatus(
            @Param("id") UUID id,
            @Param("expected") Claim.Status expected,
            @Param("newStatus") Claim.Status newStatus,
            @Param("now") Instant now);

    /** Changes every claim of a donation that currently has the expected status. Returns how many changed. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Claim c set c.status = :newStatus, c.updatedAt = :now "
            + "where c.donationId = :donationId and c.status = :expected")
    int changeStatusForDonation(
            @Param("donationId") UUID donationId,
            @Param("expected") Claim.Status expected,
            @Param("newStatus") Claim.Status newStatus,
            @Param("now") Instant now);

    /**
     * Like changeStatusForDonation, but only for claims whose last change happened at exactly the given moment.
     * Used to reopen precisely the claims that were rejected by one particular acceptance.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Claim c set c.status = :newStatus, c.updatedAt = :now "
            + "where c.donationId = :donationId and c.status = :expected and c.updatedAt = :changedAt")
    int changeStatusForDonationChangedAt(
            @Param("donationId") UUID donationId,
            @Param("expected") Claim.Status expected,
            @Param("changedAt") Instant changedAt,
            @Param("newStatus") Claim.Status newStatus,
            @Param("now") Instant now);
}
