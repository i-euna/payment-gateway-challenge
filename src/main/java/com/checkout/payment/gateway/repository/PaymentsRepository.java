package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.dto.PaymentResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {

  private final Map<UUID, PaymentResponse> payments = new ConcurrentHashMap<>();

  private final Map<String, IdempotencyMetadata> idempotencyStore = new ConcurrentHashMap<>();

  public void add(PaymentResponse payment, String idempotencyKey, String hash) {
    payments.put(payment.getId(), payment);
    idempotencyStore.put(idempotencyKey, new IdempotencyMetadata(hash, payment));
  }

  public Optional<PaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<IdempotencyMetadata> getByIdempotencyKey(String key) {
    return Optional.ofNullable(idempotencyStore.get(key));
  }

  public record IdempotencyMetadata(String requestHash, PaymentResponse response) {}
}
