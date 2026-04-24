package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * The Stripe <code>paymentMethod</code> id returned to the client after
 * <code>confirmSetup</code> (or from the SetupIntent).
 */
public record SavePaymentMethodRequest(
        @NotBlank
        @JsonProperty("paymentMethodId")
        String paymentMethodId,
        @JsonProperty("setAsDefault")
        Boolean setAsDefault) {
}
