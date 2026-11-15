package com.umeed.claims;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface ClaimRepository extends JpaRepository<Claim, UUID> {

    boolean existsByDonationIdAndClaimantId(UUID donationId, UUID claimantId);

    List<Claim> findByDonationIdOrderByCreatedAtAsc(UUID donationId);

    Page<Claim> findByClaimantIdOrderByCreatedAtDesc(UUID claimantId, Pageable pageable);

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
}
