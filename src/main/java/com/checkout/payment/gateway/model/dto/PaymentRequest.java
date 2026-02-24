package com.checkout.payment.gateway.model.dto;

public record PaymentRequest(String cardNumber,
                             int expiryMonth,
                             int expiryYear,
                             String currency,
                             long amount,
                             String cvv) {}
