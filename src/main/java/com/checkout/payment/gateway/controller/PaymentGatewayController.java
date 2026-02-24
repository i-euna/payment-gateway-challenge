package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  public ResponseEntity<PaymentResponse> createPayment(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody PaymentRequest request) {

    PaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return ResponseEntity.ok(paymentGatewayService.getPaymentById(id));
  }
}