package com.travelo.adservice.service.impl;

import com.travelo.adservice.dto.CampaignMetricDailyResponse;
import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.Campaign;
import com.travelo.adservice.entity.CampaignMetricDaily;
import com.travelo.adservice.entity.enums.BudgetType;
import com.travelo.adservice.repository.CampaignMetricDailyRepository;
import com.travelo.adservice.repository.CampaignRepository;
import com.travelo.adservice.service.CampaignMetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class CampaignMetricsServiceImpl implements CampaignMetricsService {

    private static final double TOLERANCE = 1e-6;

    private final CampaignRepository campaignRepository;
    private final CampaignMetricDailyRepository metricDailyRepository;

    public CampaignMetricsServiceImpl(
            CampaignRepository campaignRepository,
            CampaignMetricDailyRepository metricDailyRepository) {
        this.campaignRepository = campaignRepository;
        this.metricDailyRepository = metricDailyRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public boolean hasRemainingDeliveryBudget(Campaign c) {
        if (c == null) {
            return false;
        }
        Double b = c.getBudget();
        if (b == null || b <= 0) {
            return true;
        }
        double budget = b;
        double nextCost = estimatedImpressionCost(c);
        if (c.getBudgetType() == BudgetType.DAILY) {
            double today = getTodaySpend(c.getId());
            return today + nextCost <= budget + TOLERANCE;
        }
        double spent = c.getSpend() != null ? c.getSpend() : 0.0;
        return spent + nextCost <= budget + TOLERANCE;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "adTransactionManager")
    public void recordDeliveryImpressions(List<Ad> deliveredAds) {
        if (deliveredAds == null || deliveredAds.isEmpty()) {
            return;
        }
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        for (Ad ad : deliveredAds) {
            if (ad.getAdGroup() == null) {
                continue;
            }
            Campaign c = ad.getAdGroup().getCampaign();
            if (c == null) {
                continue;
            }
            double cost = estimatedImpressionCost(c);
            campaignRepository.findById(c.getId())
                    .ifPresent(live -> {
                        upsertDaily(
                                live.getBusinessAccountId(), live.getId(), day, 1, cost, 0, 0);
                        long imp = (live.getImpressions() != null) ? live.getImpressions() : 0L;
                        double sp = (live.getSpend() != null) ? live.getSpend() : 0.0;
                        live.setImpressions(imp + 1);
                        live.setSpend(sp + cost);
                        campaignRepository.save(live);
                    });
        }
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public List<CampaignMetricDailyResponse> getDailyMetrics(
            UUID businessAccountId,
            LocalDate from,
            LocalDate to,
            UUID campaignId) {
        List<CampaignMetricDaily> rows = metricDailyRepository
                .findByBusinessAccountIdAndMetricDateBetweenOrderByMetricDateAsc(
                        businessAccountId, from, to);
        if (campaignId != null) {
            rows = rows.stream().filter(r -> campaignId.equals(r.getCampaignId())).toList();
        }
        return rows.stream()
                .map(r -> CampaignMetricDailyResponse.of(
                        r.getCampaignId(),
                        r.getMetricDate(),
                        r.getImpressions(),
                        r.getClicks(),
                        r.getSpend(),
                        r.getConversions()))
                .toList();
    }

    private void upsertDaily(
            UUID businessAccountId,
            UUID campaignId,
            LocalDate day,
            long addImpressions,
            double addSpend,
            long addClicks,
            long addConversions) {
        CampaignMetricDaily m = metricDailyRepository
                .findByCampaignIdAndMetricDate(campaignId, day)
                .orElseGet(() -> {
                    CampaignMetricDaily n = new CampaignMetricDaily();
                    n.setBusinessAccountId(businessAccountId);
                    n.setCampaignId(campaignId);
                    n.setMetricDate(day);
                    return n;
                });
        m.setImpressions(m.getImpressions() + addImpressions);
        m.setSpend(m.getSpend() + addSpend);
        m.setClicks(m.getClicks() + addClicks);
        m.setConversions(m.getConversions() + addConversions);
        metricDailyRepository.save(m);
    }

    private double getTodaySpend(UUID campaignId) {
        return metricDailyRepository
                .findByCampaignIdAndMetricDate(
                        campaignId, LocalDate.now(ZoneOffset.UTC))
                .map(CampaignMetricDaily::getSpend)
                .orElse(0.0);
    }

    private static double estimatedImpressionCost(Campaign c) {
        Double bid = c.getBidAmount();
        if (bid != null && bid > 0) {
            return Math.min(0.01, bid * 0.0001);
        }
        return 0.0001;
    }
}