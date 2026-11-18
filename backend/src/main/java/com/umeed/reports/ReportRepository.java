package com.umeed.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByDonationIdAndReporterId(UUID donationId, UUID reporterId);

    boolean existsByNeedIdAndReporterId(UUID needId, UUID reporterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.id = :id")
    Optional<Report> findForUpdateById(@Param("id") UUID id);

    List<Report> findByDonationIdAndStatus(UUID donationId, Report.Status status);

    List<Report> findByNeedIdAndStatus(UUID needId, Report.Status status);
}
