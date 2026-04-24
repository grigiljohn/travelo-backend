package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record PaymentIntentResponse(
        @JsonProperty("clientSecret")
        String clientSecret,
        @JsonProperty("paymentId")
        UUID paymentId) {
}
