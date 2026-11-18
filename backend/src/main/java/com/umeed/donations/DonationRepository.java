package com.umeed.donations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

interface DonationRepository extends JpaRepository<Donation, UUID> {

    /**
     * The feed for one user: available donations they can claim, newest first. Leaves out the
     * user's own donations and the ids in {@code excluded} (items they already claimed).
     * {@code excluded} must not be empty; DonationService passes a placeholder id when there is nothing to leave out.
     */
    @Query("""
            select d from Donation d
            where d.status = com.umeed.donations.Donation.Status.AVAILABLE
              and d.donorId <> :callerId
              and d.id not in :excluded
            order by d.createdAt desc
            """)
    Page<Donation> findClaimableNewestFirst(
            @Param("callerId") UUID callerId, @Param("excluded") Collection<UUID> excluded, Pageable pageable);

    Page<Donation> findByDonorIdOrderByCreatedAtDesc(UUID donorId, Pageable pageable);

    Page<Donation> findByDonorIdAndStatusNotOrderByCreatedAtDesc(UUID donorId, Donation.Status status, Pageable pageable);

    /**
     * The feed for one user, nearest to the given point first, then newest first. Same filter as
     * findClaimableNewestFirst. Distance is the great-circle distance in kilometres between the point
     * and the stored approximate location.
     */
    @Query(value = """
            SELECT d.*
            FROM donations d
            WHERE d.status = 'AVAILABLE'
              AND d.donor_id <> :callerId
              AND d.id NOT IN (:excluded)
            ORDER BY 6371 * acos(LEAST(1.0, GREATEST(-1.0,
                         cos(radians(:lat)) * cos(radians(d.latitude)) * cos(radians(d.longitude) - radians(:lng))
                         + sin(radians(:lat)) * sin(radians(d.latitude))))) ASC,
                     d.created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM donations d
            WHERE d.status = 'AVAILABLE' AND d.donor_id <> :callerId AND d.id NOT IN (:excluded)
            """,
            nativeQuery = true)
    Page<Donation> findClaimableNearestFirst(
            @Param("callerId") UUID callerId,
            @Param("excluded") Collection<UUID> excluded,
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            Pageable pageable);

    /** Loads a donation and locks its row until the transaction ends, so two changes cannot run at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> findForUpdateById(@Param("id") UUID id);

    /** Loads a donation with a shared lock: it cannot be changed by others until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> findForShareById(@Param("id") UUID id);
}
