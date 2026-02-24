package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.dto.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import com.checkout.payment.gateway.repository.PaymentsRepository.IdempotencyMetadata;
import com.checkout.payment.gateway.validator.PaymentValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentGatewayService {
  private final PaymentsRepository repository;
  private final BankClient bankClient;
  private final PaymentValidator validator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public PaymentGatewayService(PaymentsRepository repository, BankClient bankClient,
      PaymentValidator validator) {
    this.repository = repository;
    this.bankClient = bankClient;
    this.validator = validator;
  }

  public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
    validator.validate(request);
    String currentHash = calculateHash(request);

    Optional<IdempotencyMetadata> existing = repository.getByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      if (!existing.get().requestHash().equals(currentHash)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key reused with different payload");
      }
      return existing.get().response();
    }
    BankResponse bankResponse = bankClient.authorize(request);

    PaymentResponse response = new PaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);
    response.setCardNumberLastFour(Integer.parseInt(request.cardNumber().substring(request.cardNumber().length() - 4)));
    response.setAmount(request.amount());
    response.setExpiryMonth(request.expiryMonth());
    response.setExpiryYear(request.expiryYear());
    response.setCurrency(request.currency());

    repository.add(response, idempotencyKey, currentHash);

    return response;
  }

  private String calculateHash(PaymentRequest request) {
    try {
      String json = objectMapper.writeValueAsString(request);
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException("Hashing failed", e);
    }
  }

  public PaymentResponse getPaymentById(UUID id) {
    return repository.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid ID"));
  }
}