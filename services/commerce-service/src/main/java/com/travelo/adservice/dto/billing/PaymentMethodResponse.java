package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record PaymentMethodResponse(
        @JsonProperty("id")
        UUID id,
        @JsonProperty("type")
        String type,
        @JsonProperty("provider")
        String provider,
        @JsonProperty("last4")
        String last4,
        @JsonProperty("brand")
        String brand,
        @JsonProperty("expiryMonth")
        Integer expiryMonth,
        @JsonProperty("expiryYear")
        Integer expiryYear,
        @JsonProperty("isDefault")
        boolean isDefault,
        @JsonProperty("status")
        String status) {
}
