package com.umeed.needs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

interface NeedOfferRepository extends JpaRepository<NeedOffer, UUID> {

    boolean existsByNeedIdAndDonationId(UUID needId, UUID donationId);

    /** Oldest first; offers made in the same instant are ordered by id, so the order never changes. */
    List<NeedOffer> findByNeedIdOrderByCreatedAtAscIdAsc(UUID needId);

    Page<NeedOffer> findByDonorIdOrderByCreatedAtDesc(UUID donorId, Pageable pageable);

    @Query("select o.donorId from NeedOffer o where o.needId = :needId and o.status = :status")
    List<UUID> findDonorIdsByNeedIdAndStatus(@Param("needId") UUID needId, @Param("status") NeedOffer.Status status);

    @Query("select o.needId, count(o) from NeedOffer o "
            + "where o.needId in :needIds and o.status = com.umeed.needs.NeedOffer.Status.PENDING group by o.needId")
    List<Object[]> countPendingByNeedIds(@Param("needIds") Collection<UUID> needIds);

    /** Changes one offer only if it has the expected status. Returns 1 if it changed, 0 if not. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = :newStatus, o.updatedAt = :now where o.id = :id and o.status = :expected")
    int changeStatus(
            @Param("id") UUID id,
            @Param("expected") NeedOffer.Status expected,
            @Param("newStatus") NeedOffer.Status newStatus,
            @Param("now") Instant now);

    /** Changes every offer on a need that has the expected status. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = :newStatus, o.updatedAt = :now "
            + "where o.needId = :needId and o.status = :expected")
    int changeStatusForNeed(
            @Param("needId") UUID needId,
            @Param("expected") NeedOffer.Status expected,
            @Param("newStatus") NeedOffer.Status newStatus,
            @Param("now") Instant now);

    /** Changes one donor's offers that have the expected status, on the needs of one requester. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = :newStatus, o.updatedAt = :now "
            + "where o.donorId = :donorId and o.status = :expected "
            + "and o.needId in (select n.id from NeedRequest n where n.requesterId = :requesterId)")
    int changeStatusForDonorOnNeedsOf(
            @Param("donorId") UUID donorId,
            @Param("requesterId") UUID requesterId,
            @Param("expected") NeedOffer.Status expected,
            @Param("newStatus") NeedOffer.Status newStatus,
            @Param("now") Instant now);

    /** Changes all of one donor's offers that have the expected status. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = :newStatus, o.updatedAt = :now "
            + "where o.donorId = :donorId and o.status = :expected")
    int changeStatusForDonor(
            @Param("donorId") UUID donorId,
            @Param("expected") NeedOffer.Status expected,
            @Param("newStatus") NeedOffer.Status newStatus,
            @Param("now") Instant now);

    /** The offered item was taken down: its waiting offers are withdrawn. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = com.umeed.needs.NeedOffer.Status.WITHDRAWN, o.updatedAt = :now "
            + "where o.donationId = :donationId and o.status = com.umeed.needs.NeedOffer.Status.PENDING")
    int withdrawPendingForDonation(@Param("donationId") UUID donationId, @Param("now") Instant now);

    /** Account deletion: declines pending offers on all of a requester's needs. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedOffer o set o.status = com.umeed.needs.NeedOffer.Status.DECLINED, o.updatedAt = :now "
            + "where o.status = com.umeed.needs.NeedOffer.Status.PENDING "
            + "and o.needId in (select n.id from NeedRequest n where n.requesterId = :requesterId)")
    int declineAllPendingOnNeedsOf(@Param("requesterId") UUID requesterId, @Param("now") Instant now);

    default Map<UUID, Long> pendingCounts(Collection<UUID> needIds) {
        if (needIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : countPendingByNeedIds(needIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }
}
