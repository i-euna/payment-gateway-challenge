package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.client.dto.BankRequest;
import com.checkout.payment.gateway.client.dto.BankResponse;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BankClient {
  private final WebClient webClient;

  public BankClient(
      WebClient.Builder builder,
      @Value("${bank.simulator.url:http://localhost:8080}") String baseUrl
  ) {
    this.webClient = builder.baseUrl(baseUrl.trim()).build();
  }

  public BankResponse authorize(PaymentRequest request) {

    BankRequest bankRequest = new BankRequest(
        request.cardNumber(),
        request.expiryMonth() + "/" + request.expiryYear(),
        request.currency(),
        request.amount(),
        request.cvv()
    );

    return webClient.post()
        .uri("/payments")
        .bodyValue(bankRequest)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response ->
            Mono.error(new RuntimeException("Invalid request to bank")))
        .bodyToMono(BankResponse.class)
        .block();
  }
}
