package com.umeed.reports;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByDonationIdAndReporterId(UUID donationId, UUID reporterId);
}
