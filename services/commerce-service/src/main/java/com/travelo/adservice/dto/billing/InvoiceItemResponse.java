package com.travelo.adservice.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record InvoiceItemResponse(
        @JsonProperty("id")
        UUID id,
        @JsonProperty("date")
        String date,
        @JsonProperty("amount")
        double amount,
        @JsonProperty("status")
        String status,
        @JsonProperty("description")
        String description) {
}
