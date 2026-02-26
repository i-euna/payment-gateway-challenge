package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.dto.BankResponse;
import com.checkout.payment.gateway.model.dto.PaymentResponse;
import com.checkout.payment.gateway.model.dto.PaymentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentGatewayE2ETest {

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int port;

  @MockBean
  private BankClient bankClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private String getBaseUrl() {
    return "http://localhost:" + port + "/payments";
  }

  @BeforeEach
  void setup() {
    restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @Test
  @DisplayName("Full Flow: Valid payment should return 201 and be retrievable")
  void fullPaymentFlowTest() {
    String key = UUID.randomUUID().toString();
    PaymentRequest request = new PaymentRequest("4242424242424242", 12, 2028, "USD", 5000L, "123");

    when(bankClient.authorize(any())).thenReturn(new BankResponse(true, "uuid"));

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    headers.set("Idempotency-Key", key);
    HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<PaymentResponse> postResponse = restTemplate.postForEntity(
        getBaseUrl(), entity, PaymentResponse.class);

    assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
    UUID paymentId = postResponse.getBody().getId();
    assertNotNull(paymentId);
    assertEquals(5000L, postResponse.getBody().getAmount());

    HttpEntity<Void> getEntity = new HttpEntity<>(headers);

    ResponseEntity<PaymentResponse> getResponse = restTemplate.exchange(
        getBaseUrl() + "/" + paymentId,
        HttpMethod.GET,
        getEntity,
        PaymentResponse.class
    );

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertNotNull(getResponse.getBody());
    assertEquals(4242, getResponse.getBody().getCardNumberLastFour());
  }

  @Test
  @DisplayName("E2E Idempotency: Duplicate key with different amount returns 409")
  void idempotencyConflictE2ETest() {
    String key = "shared-key-1";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    headers.set("Idempotency-Key", key);

    PaymentRequest req1 = new PaymentRequest("4242424242424242", 12, 2028, "USD", 1000L, "123");
    when(bankClient.authorize(any())).thenReturn(new BankResponse(true, "uuid"));

    restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(req1, headers), PaymentResponse.class);

    PaymentRequest req2 = new PaymentRequest("4242424242424242", 12, 2028, "USD", 2000L, "123");

    ResponseEntity<Map> conflictResponse = restTemplate.postForEntity(
        getBaseUrl(), new HttpEntity<>(req2, headers), Map.class);

    assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
  }

  @Test
  @DisplayName("E2E POST: Multiple Validation Failures")
  void postPaymentValidationFailures() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    headers.set("Idempotency-Key", "validation-test");

    PaymentRequest reqA = new PaymentRequest("4242A24242424242", 12, 2028, "USD", 100L, "123");
    ResponseEntity<Map> respA = restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(reqA, headers), Map.class);
    assertEquals(HttpStatus.BAD_REQUEST, respA.getStatusCode());

    PaymentRequest reqB = new PaymentRequest("4242424242424242", 12, 2028, "USD", 0L, "123");
    ResponseEntity<Map> respB = restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(reqB, headers), Map.class);
    assertEquals(HttpStatus.BAD_REQUEST, respB.getStatusCode());

    PaymentRequest reqC = new PaymentRequest("4242424242424242", 12, 2028, "JPY", 100L, "123");
    ResponseEntity<Map> respC = restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(reqC, headers), Map.class);
    assertEquals(HttpStatus.BAD_REQUEST, respC.getStatusCode());
  }

  @Test
  @DisplayName("E2E GET: Retrieval Failure Conditions")
  void getPaymentFailureConditions() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // Invalid UUID format - Should return 400 Bad Request
    ResponseEntity<String> respA = restTemplate.exchange(
        getBaseUrl() + "/not-a-uuid",
        HttpMethod.GET,
        entity,
        String.class);
    assertEquals(HttpStatus.BAD_REQUEST, respA.getStatusCode());

    // Unknown UUID - Should return 404 Not Found
    UUID randomId = UUID.randomUUID();
    ResponseEntity<Map> respB = restTemplate.exchange(
        getBaseUrl() + "/" + randomId,
        HttpMethod.GET,
        entity,
        Map.class);
    assertEquals(HttpStatus.NOT_FOUND, respB.getStatusCode());
  }

  @Test
  @DisplayName("E2E Security: Ensure Card Masking in Retrieval")
  void verifyCardMaskingInRetrieval() throws JsonProcessingException {
    String fullCard = "1234567812345678";
    PaymentRequest req = new PaymentRequest(fullCard, 12, 2028, "USD", 100L, "123");

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    when(bankClient.authorize(any())).thenReturn(new BankResponse(true, "BANK-UUID-123"));

    ResponseEntity<PaymentResponse> postResp = restTemplate.postForEntity(
        getBaseUrl(),
        new HttpEntity<>(req, headers),
        PaymentResponse.class);

    assertEquals(HttpStatus.CREATED, postResp.getStatusCode());
    UUID id = postResp.getBody().getId();

    HttpEntity<Void> getEntity = new HttpEntity<>(headers);
    ResponseEntity<Map> getResp = restTemplate.exchange(
        getBaseUrl() + "/" + id,
        HttpMethod.GET,
        getEntity,
        Map.class);

    assertEquals(HttpStatus.OK, getResp.getStatusCode());
    assertNotNull(getResp.getBody().get("cardNumberLastFour"));
    assertEquals(5678, getResp.getBody().get("cardNumberLastFour"));

    String jsonString = objectMapper.writeValueAsString(getResp.getBody());
    assertFalse(jsonString.contains(fullCard), "The full card number must not be present in the response!");
  }

  @Test
  @DisplayName("E2E Security: Reject requests with missing or invalid API Key")
  void securityFailureE2ETest() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer invalid_key_123");
    headers.set("Idempotency-Key", UUID.randomUUID().toString());

    PaymentRequest request = new PaymentRequest("4242424242424242", 12, 2028, "USD", 1000L, "123");
    HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<Map> postResp = restTemplate.postForEntity(getBaseUrl(), entity, Map.class);
    assertEquals(HttpStatus.UNAUTHORIZED, postResp.getStatusCode());

    HttpHeaders noAuthHeaders = new HttpHeaders();
    HttpEntity<Void> noAuthEntity = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<Map> getResp = restTemplate.exchange(
        getBaseUrl() + "/" + UUID.randomUUID(),
        HttpMethod.GET,
        noAuthEntity,
        Map.class);

    assertEquals(HttpStatus.UNAUTHORIZED, getResp.getStatusCode());
  }

  @Test
  @DisplayName("E2E Validation: Reject decimal amounts")
  void shouldRejectDecimalAmount() {
    String jsonWithDecimal = """
        {
            "cardNumber": "4242424242424242",
            "expiryMonth": 12,
            "expiryYear": 2028,
            "currency": "USD",
            "amount": 10.5,
            "cvv": "123"
        }
        """;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer sk_test_cko_12345");
    headers.set("Idempotency-Key", "decimal-test");
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<>(jsonWithDecimal, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl(), entity, Map.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}