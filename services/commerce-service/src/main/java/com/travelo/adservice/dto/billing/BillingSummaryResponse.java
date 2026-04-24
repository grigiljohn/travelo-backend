package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shaped for the ads manager SPA wallet & billing table.
 */
public class BillingSummaryResponse {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("businessId")
    private UUID businessId;
    @JsonProperty("walletBalance")
    private double walletBalance;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("autoPayEnabled")
    private boolean autoPayEnabled;
    @JsonProperty("spendingLimit")
    private Double spendingLimit;
    @JsonProperty("invoiceProfile")
    private Map<String, Object> invoiceProfile;
    @JsonProperty("paymentMethods")
    private List<PaymentMethodResponse> paymentMethods;
    @JsonProperty("invoices")
    private List<InvoiceItemResponse> invoices;

    public static BillingSummaryResponse of(
            UUID id,
            UUID businessId,
            double walletBalance,
            String currency,
            boolean autoPayEnabled,
            Double spendingLimit,
            Map<String, Object> invoiceProfile,
            List<PaymentMethodResponse> paymentMethods,
            List<InvoiceItemResponse> invoices) {
        BillingSummaryResponse r = new BillingSummaryResponse();
        r.id = id;
        r.businessId = businessId;
        r.walletBalance = walletBalance;
        r.currency = currency;
        r.autoPayEnabled = autoPayEnabled;
        r.spendingLimit = spendingLimit;
        r.invoiceProfile = invoiceProfile;
        r.paymentMethods = paymentMethods;
        r.invoices = invoices;
        return r;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public double getWalletBalance() { return walletBalance; }
    public String getCurrency() { return currency; }
    public boolean isAutoPayEnabled() { return autoPayEnabled; }
    public Double getSpendingLimit() { return spendingLimit; }
    public Map<String, Object> getInvoiceProfile() { return invoiceProfile; }
    public List<PaymentMethodResponse> getPaymentMethods() { return paymentMethods; }
    public List<InvoiceItemResponse> getInvoices() { return invoices; }
}
