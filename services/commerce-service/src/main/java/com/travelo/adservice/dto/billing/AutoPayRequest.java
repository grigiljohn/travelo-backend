package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AutoPayRequest(
        @NotNull
        @JsonProperty("enabled")
        Boolean enabled,
        @JsonProperty("internalPaymentMethodId")
        UUID internalPaymentMethodId,
        @JsonProperty("threshold")
        Double threshold) {
}
