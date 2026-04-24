package com.travelo.adservice.repository;

import com.travelo.adservice.entity.CampaignMetricDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMetricDailyRepository extends JpaRepository<CampaignMetricDaily, UUID> {

    Optional<CampaignMetricDaily> findByCampaignIdAndMetricDate(UUID campaignId, LocalDate metricDate);

    List<CampaignMetricDaily> findByBusinessAccountIdAndMetricDateBetweenOrderByMetricDateAsc(
            UUID businessAccountId, LocalDate from, LocalDate to);
}
