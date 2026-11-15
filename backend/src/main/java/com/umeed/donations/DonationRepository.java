package com.umeed.donations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByStatusOrderByCreatedAtDesc(Donation.Status status, Pageable pageable);

    Page<Donation> findByDonorIdOrderByCreatedAtDesc(UUID donorId, Pageable pageable);

    Page<Donation> findByDonorIdAndStatusNotOrderByCreatedAtDesc(UUID donorId, Donation.Status status, Pageable pageable);

    /**
     * Available donations, nearest to the given point first, then newest first.
     * Distance is the great-circle distance in kilometres between the point and the stored approximate location.
     */
    @Query(value = """
            SELECT d.*
            FROM donations d
            WHERE d.status = 'AVAILABLE'
            ORDER BY 6371 * acos(LEAST(1.0, GREATEST(-1.0,
                         cos(radians(:lat)) * cos(radians(d.latitude)) * cos(radians(d.longitude) - radians(:lng))
                         + sin(radians(:lat)) * sin(radians(d.latitude))))) ASC,
                     d.created_at DESC
            """,
            countQuery = "SELECT count(*) FROM donations d WHERE d.status = 'AVAILABLE'",
            nativeQuery = true)
    Page<Donation> findAvailableNearestFirst(@Param("lat") double latitude, @Param("lng") double longitude, Pageable pageable);
}
