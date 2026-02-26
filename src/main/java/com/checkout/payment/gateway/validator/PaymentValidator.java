package com.checkout.payment.gateway.validator;

import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PaymentValidator {

  private final Set<String> supportedCurrencies;

  public PaymentValidator(@Value("${gateway.supported-currencies}") List<String> currencies) {
    this.supportedCurrencies = new HashSet<>(currencies);
  }

  public void validate(PaymentRequest request) {
    try {
      YearMonth expiry = YearMonth.of(request.expiryYear(), request.expiryMonth());
      if (expiry.isBefore(YearMonth.now())) {
        throw new PaymentValidationException("The expiry date must be in the future.");
      }
    } catch (DateTimeException e) {
      throw new PaymentValidationException("The provided expiry month or year is invalid.");
    }

    if (request.cardNumber() == null || !request.cardNumber().matches("\\d+")) {
      throw new PaymentValidationException("Card number must contain only numeric digits.");
    }

    if (request.cardNumber().length() < 14 || request.cardNumber().length() > 19) {
      throw new PaymentValidationException("Card number must be between 14 and 19 digits.");
    }

    if (request.amount() <= 0) {
      throw new PaymentValidationException("Amount must be a positive integer greater than zero.");
    }
    if (request.currency() == null || !supportedCurrencies.contains(request.currency().toUpperCase())) {
      throw new PaymentValidationException(
          "Invalid or unsupported currency. Supported codes are: " + supportedCurrencies
      );
    }
  }
}