package com.bigbrightpaints.erp.modules.accounting.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/accounting")
public class AccountingController {

  /**
   * Keep accounting error payloads structured and explicit for UI diagnostics.
   */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    if (usesMappedConcurrencyStatus(ex)) {
      return AccountingApplicationExceptionResponses.mappedStatus(ex, request);
    }
    return AccountingApplicationExceptionResponses.badRequest(ex, request);
  }

  private boolean usesMappedConcurrencyStatus(ApplicationException ex) {
    return ex != null
        && ex.getErrorCode() != null
        && ex.getErrorCode().getCode().startsWith("CONC_");
  }
}
