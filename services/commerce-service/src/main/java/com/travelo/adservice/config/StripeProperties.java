package com.travelo.adservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API keys: use env STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, STRIPE_PUBLISHABLE_KEY
 * in production — never commit real secrets.
 */
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /**
     * Stripe secret (sk_…); required for server-side API calls and webhooks verification.
     */
    private String secretKey = "";

    private String webhookSecret = "";

    /**
     * Public key (pk_…); passed to the ads manager SPA for Stripe.js.
     */
    private String publishableKey = "";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public boolean isReady() {
        return secretKey != null && !secretKey.isBlank();
    }
}
