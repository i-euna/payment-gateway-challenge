package com.checkout.payment.gateway.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    @JsonProperty("currency") String currency,
    @JsonProperty("amount") Long amount,
    @JsonProperty("cvv") String cvv
) {}
