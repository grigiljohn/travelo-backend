package com.travelo.adservice.service;

import com.travelo.adservice.dto.CampaignMetricDailyResponse;
import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.Campaign;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CampaignMetricsService {

    /**
     * Whether the campaign can still receive at least one more estimated-cost delivery
     * for the current budget (lifetime vs daily).
     */
    boolean hasRemainingDeliveryBudget(Campaign campaign);

    /**
     * Record impressions (and small estimated spend) for served ads. Uses a separate
     * transaction from read-only ad delivery.
     */
    void recordDeliveryImpressions(List<Ad> deliveredAds);

    List<CampaignMetricDailyResponse> getDailyMetrics(
            UUID businessAccountId,
            LocalDate from,
            LocalDate to,
            UUID campaignId);
}
