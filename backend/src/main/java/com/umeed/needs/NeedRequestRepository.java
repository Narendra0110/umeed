package com.umeed.needs;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

interface NeedRequestRepository extends JpaRepository<NeedRequest, UUID> {

    /**
     * Open needs the caller can help with, newest first: not their own, and not from people blocked
     * either way. {@code hiddenRequesters} must not be empty; the service passes a placeholder id.
     */
    @Query("""
            select n from NeedRequest n
            where n.status = com.umeed.needs.NeedRequest.Status.OPEN
              and n.requesterId <> :callerId
              and n.requesterId not in :hiddenRequesters
            order by n.createdAt desc
            """)
    Page<NeedRequest> findOpenNewestFirst(
            @Param("callerId") UUID callerId,
            @Param("hiddenRequesters") Collection<UUID> hiddenRequesters,
            Pageable pageable);

    /** The same list, nearest to the given point first, then newest. Distance as in the donations feed. */
    @Query(value = """
            SELECT n.*
            FROM need_requests n
            WHERE n.status = 'OPEN'
              AND n.requester_id <> :callerId
              AND n.requester_id NOT IN (:hiddenRequesters)
            ORDER BY 6371 * acos(LEAST(1.0, GREATEST(-1.0,
                         cos(radians(:lat)) * cos(radians(n.latitude)) * cos(radians(n.longitude) - radians(:lng))
                         + sin(radians(:lat)) * sin(radians(n.latitude))))) ASC,
                     n.created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM need_requests n
            WHERE n.status = 'OPEN' AND n.requester_id <> :callerId AND n.requester_id NOT IN (:hiddenRequesters)
            """,
            nativeQuery = true)
    Page<NeedRequest> findOpenNearestFirst(
            @Param("callerId") UUID callerId,
            @Param("hiddenRequesters") Collection<UUID> hiddenRequesters,
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            Pageable pageable);

    Page<NeedRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    /** Loads a need and locks its row until the transaction ends, so two offers cannot be accepted at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NeedRequest n where n.id = :id")
    Optional<NeedRequest> findForUpdateById(@Param("id") UUID id);

    /** Account deletion: closes the user's open needs. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NeedRequest n set n.status = com.umeed.needs.NeedRequest.Status.CLOSED, n.updatedAt = :now "
            + "where n.requesterId = :requesterId and n.status = com.umeed.needs.NeedRequest.Status.OPEN")
    int closeAllOpenOf(@Param("requesterId") UUID requesterId, @Param("now") Instant now);
}
