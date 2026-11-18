package com.umeed.donations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

interface DonationSkipRepository extends JpaRepository<DonationSkip, DonationSkip.Key> {

    @Query("select s.donationId from DonationSkip s where s.userId = :userId")
    Set<UUID> findDonationIdsByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DonationSkip s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
