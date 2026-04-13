package com.travelo.adservice.dto;

public class AdPerformanceSummaryDto {
    private Long totalImpressions;
    private Long totalClicks;
    private Long totalConversions;
    private Double totalSpend;
    private Double totalRevenue;
    private Double ctr;
    private Double cpc;
    private Double cpm;
    private Double conversionRate;
    private Double roas;

    // Getters and Setters
    public Long getTotalImpressions() {
        return totalImpressions;
    }

    public void setTotalImpressions(Long totalImpressions) {
        this.totalImpressions = totalImpressions;
    }

    public Long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(Long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public Long getTotalConversions() {
        return totalConversions;
    }

    public void setTotalConversions(Long totalConversions) {
        this.totalConversions = totalConversions;
    }

    public Double getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(Double totalSpend) {
        this.totalSpend = totalSpend;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getCtr() {
        return ctr;
    }

    public void setCtr(Double ctr) {
        this.ctr = ctr;
    }

    public Double getCpc() {
        return cpc;
    }

    public void setCpc(Double cpc) {
        this.cpc = cpc;
    }

    public Double getCpm() {
        return cpm;
    }

    public void setCpm(Double cpm) {
        this.cpm = cpm;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Double getRoas() {
        return roas;
    }

    public void setRoas(Double roas) {
        this.roas = roas;
    }
}

