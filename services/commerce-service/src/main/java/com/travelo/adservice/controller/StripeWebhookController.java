package com.travelo.adservice.controller;

import com.stripe.exception.StripeException;
import com.travelo.adservice.config.StripeProperties;
import com.travelo.adservice.service.StripeWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Ingests Stripe webhooks. Configure the same path in the Stripe dashboard (e.g. local: Stripe CLI
 * <code>stripe listen --forward-to</code>).
 */
@RestController
@RequestMapping("/api/v1/billing/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeProperties stripeProperties, StripeWebhookService stripeWebhookService) {
        this.stripeProperties = stripeProperties;
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String signature) {
        if (!stripeProperties.isReady() || (stripeProperties.getWebhookSecret() != null
                && stripeProperties.getWebhookSecret().isBlank())) {
            log.warn("Webhook received but STRIPE_SECRET_KEY or STRIPE_WEBHOOK_SECRET is not set");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("not configured");
        }
        try {
            stripeWebhookService.processEventPayload(payload, signature);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("misconfigured");
        } catch (StripeException e) {
            log.warn("Invalid Stripe webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid");
        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
        return ResponseEntity.ok("ok");
    }
}
