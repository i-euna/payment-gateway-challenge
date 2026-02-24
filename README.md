# Payment Gateway API

- An idempotent and secure payment processing gateway built with Spring Boot. 
- This service handles payment submissions, retrieval, and integrates with a bank simulator.

## Requirements
- JDK 17
- Docker

# Quick Start
To clean the environment, build the application, and start all services (including the Bank Simulator):

```
docker-compose down
docker-compose up --build
```

### Postman Testing:
- Base URL: http://localhost:8080
- Auth Header: `Authorization: Bearer sk_test_cko_12345`
- Required Header: `Idempotency-Key: <unique-uuid>`
- A postman collection is available in [postman_collection](/postman/Checkout_Test.postman_collection.json) folder.

## Key Architectural Decisions

### Validation

* **Validation & Financial Integrity**
  * Minor Units: All amounts are handled as long integers representing the smallest currency unit. This prevents floating-point rounding errors.
  * Strict Validation:
    * Card numbers are numeric and valid lengths (14–19 digits).
    * Expiry dates are strictly in the future.
    * Currencies are restricted to an allow-list of 3 ISO codes as per requirements.
    * Amount cannot be a decimal (like 10.1).

* **Error Handling**
  * Global Exception Handler: Centralized `ControllerAdvice` translates internal exceptions into a standardized error:

```JSON
{
"status": "Rejected",
"reason": "Validation Failed",
"message": "Card number must contain only numeric digits."
}
```

### Security & Authentication

* **HandlerInterceptor for API Key validation**
  * Implemented a Secret Key (sk_) pattern common in the payment industry. This ensures that only authorized merchants can access the gateway.

* **Data Privacy**
  * Implemented Card Masking. The full card number and CVV are never returned in the `GET` API; only the last four digits are exposed.

* **Idempotency (Replay Protection)**
  * To prevent duplicate charges the gateway hashes the Idempotency-Key + Request Body.
  * Conflict Handling: If a key is reused with a different payload, the API returns a 409 Conflict to prevent data corruption.

### Testing Philosophy: E2E over Unit Testing
* For this specific implementation, a **deliberate decision** was made to focus exclusively on End-to-End (E2E) Integration Tests rather than Unit Tests.
  * This gateway primarily functions as an orchestrator that is validating input, checking idempotency, and communicating with an external bank. 
    * Testing these components in isolation (Unit Testing) would require heavy mocking, which often misses bugs in how components interact.
  * **High-Confidence Validation**: By using TestRestTemplate to hit the actual API endpoints, we ensure that the Security Validation, Global Exception Handler, and Validation (such as the strict integer check) are all working.
  * **Lean Maintenance**: In a CRUD-adjacent microservice, E2E tests provide 100% path coverage with less boilerplate.

## Template structure

- src/ - Contains the main code of the application.
- test/ - Contains E2E tests.
- imposters/ - contains the bank simulator configuration.
- .editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code.
- docker-compose.yml - configures the bank simulator and the application.
