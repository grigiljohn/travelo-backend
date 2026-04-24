package com.travelo.adservice.service;

import com.travelo.adservice.dto.billing.*;
import com.travelo.adservice.entity.BusinessAccount;
import com.stripe.exception.StripeException;

import java.util.UUID;

public interface BillingService {

    BillingSummaryResponse getSummary(UUID businessAccountId);

    /**
     * Ensure {@link BusinessAccount#billingProfileId} and wallet row exist; create Stripe
     * customer on first use.
     */
    BusinessAccount requireBusinessAccount(UUID businessAccountId);

    SetupIntentResponse createSetupIntent(UUID businessAccountId) throws StripeException;

    PaymentMethodResponse savePaymentMethod(
            UUID businessAccountId, SavePaymentMethodRequest request) throws StripeException;

    PaymentIntentResponse createWalletTopUpIntent(
            UUID businessAccountId, WalletTopUpRequest request) throws StripeException;

    void updateAutoPay(UUID businessAccountId, AutoPayRequest request) throws StripeException;

    void removePaymentMethod(UUID businessAccountId, UUID localPaymentMethodId) throws StripeException;
}
