package com.travelo.adservice.config;

import com.stripe.Stripe;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Wires the global Stripe API key. Without {@link StripeProperties#secretKey}, server-side
 * billing end-points will return 503.
 */
@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private final StripeProperties properties;

    public StripeConfig(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void setApiKey() {
        if (properties.getSecretKey() != null && !properties.getSecretKey().isBlank()) {
            Stripe.apiKey = properties.getSecretKey();
        } else {
            Stripe.apiKey = null;
        }
    }
}
