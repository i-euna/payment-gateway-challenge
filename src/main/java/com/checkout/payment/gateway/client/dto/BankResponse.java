package com.checkout.payment.gateway.client.dto;

public record BankResponse(
    boolean authorized,
    String authorization_code
) {}
