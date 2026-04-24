package com.travelo.adservice.controller;

import com.stripe.exception.StripeException;
import com.travelo.adservice.dto.billing.*;
import com.travelo.adservice.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing", description = "Payments, wallet, Stripe setup")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @Operation(summary = "Billing & wallet summary")
    public ResponseEntity<BillingSummaryResponse> getSummary(
            @RequestParam("businessAccountId") UUID businessAccountId) {
        return ResponseEntity.ok(billingService.getSummary(businessAccountId));
    }

    @PostMapping("/setup-intent")
    @Operation(summary = "Create Stripe SetupIntent to collect a card")
    public ResponseEntity<SetupIntentResponse> createSetupIntent(
            @RequestParam("businessAccountId") UUID businessAccountId) throws StripeException {
        return ResponseEntity.ok(billingService.createSetupIntent(businessAccountId));
    }

    @PostMapping("/payment-methods")
    @Operation(summary = "Store a Stripe payment method after the client has confirmed the SetupIntent")
    public ResponseEntity<PaymentMethodResponse> savePaymentMethod(
            @RequestParam("businessAccountId") UUID businessAccountId,
            @Valid @RequestBody SavePaymentMethodRequest body) throws StripeException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.savePaymentMethod(businessAccountId, body));
    }

    @PostMapping("/wallet/payment-intent")
    @Operation(summary = "Create a PaymentIntent to add funds to the ad wallet")
    public ResponseEntity<PaymentIntentResponse> createWalletTopUp(
            @RequestParam("businessAccountId") UUID businessAccountId,
            @Valid @RequestBody WalletTopUpRequest body) throws StripeException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createWalletTopUpIntent(businessAccountId, body));
    }

    @PatchMapping("/wallet/auto-pay")
    @Operation(summary = "Enable or disable auto-pay on the wallet")
    public ResponseEntity<Void> autoPay(
            @RequestParam("businessAccountId") UUID businessAccountId,
            @Valid @RequestBody AutoPayRequest body) {
        billingService.updateAutoPay(businessAccountId, body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}")
    @Operation(summary = "Remove a saved card")
    public ResponseEntity<Void> deletePaymentMethod(
            @RequestParam("businessAccountId") UUID businessAccountId,
            @PathVariable UUID paymentMethodId) throws StripeException {
        billingService.removePaymentMethod(businessAccountId, paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, String>> onStripe(StripeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> onStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() != null ? e.getReason() : e.getClass().getSimpleName()));
    }
}
