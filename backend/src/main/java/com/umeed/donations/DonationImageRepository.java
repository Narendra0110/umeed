package com.umeed.donations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface DonationImageRepository extends JpaRepository<DonationImage, UUID> {

    List<DonationImage> findByDonationIdOrderBySortOrderAsc(UUID donationId);

    List<DonationImage> findByDonationIdInOrderByDonationIdAscSortOrderAsc(Collection<UUID> donationIds);
}
