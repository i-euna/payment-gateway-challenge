package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<Map<String, Object>> handlePaymentValidation(PaymentValidationException ex) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("status", PaymentStatus.DECLINED);
    response.put("reason", "Validation Failed");
    response.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleJsonErrors(HttpMessageNotReadableException ex) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("status", "Rejected");
    response.put("reason", "Invalid Data Format");
    response.put("message", "Amount must be a whole number (minor units). Decimals are not allowed.");

    return ResponseEntity.badRequest().body(response);
  }
}
