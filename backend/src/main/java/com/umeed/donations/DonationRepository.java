package com.umeed.donations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByStatusOrderByCreatedAtDesc(Donation.Status status, Pageable pageable);

    Page<Donation> findByDonorIdOrderByCreatedAtDesc(UUID donorId, Pageable pageable);
}
