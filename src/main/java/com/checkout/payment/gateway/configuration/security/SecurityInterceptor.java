package com.checkout.payment.gateway.configuration.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

  /*
   * Hardcoded the secret key for the test,
   * but in company's real code, we would save it in a vault
   */
  private static final String VALID_API_KEY = "sk_test_cko_12345";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String apiKey = request.getHeader("Authorization");
    if (apiKey == null || !apiKey.equals("Bearer " + VALID_API_KEY)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing API Key");
    }
    return true;
  }
}
