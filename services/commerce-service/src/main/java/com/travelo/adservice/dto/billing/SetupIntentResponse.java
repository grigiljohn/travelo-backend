package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetupIntentResponse(
        @JsonProperty("clientSecret")
        String clientSecret,
        @JsonProperty("customerId")
        String customerId,
        @JsonProperty("publishableKey")
        String publishableKey) {
}
