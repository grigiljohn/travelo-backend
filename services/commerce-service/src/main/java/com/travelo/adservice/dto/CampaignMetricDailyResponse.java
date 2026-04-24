package com.travelo.adservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One day of reporting for a single campaign.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignMetricDailyResponse {

    private UUID campaignId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    private long impressions;
    private long clicks;
    private double spend;
    private long conversions;

    public static CampaignMetricDailyResponse of(
            UUID campaignId,
            LocalDate date,
            long impressions,
            long clicks,
            double spend,
            long conversions) {
        CampaignMetricDailyResponse r = new CampaignMetricDailyResponse();
        r.campaignId = campaignId;
        r.date = date;
        r.impressions = impressions;
        r.clicks = clicks;
        r.spend = spend;
        r.conversions = conversions;
        return r;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getImpressions() {
        return impressions;
    }

    public void setImpressions(long impressions) {
        this.impressions = impressions;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

    public double getSpend() {
        return spend;
    }

    public void setSpend(double spend) {
        this.spend = spend;
    }

    public long getConversions() {
        return conversions;
    }

    public void setConversions(long conversions) {
        this.conversions = conversions;
    }
}
