package com.bigbrightpaints.erp.modules.accounting.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(
    assignableTypes = {
      AccountController.class,
      JournalController.class,
      SettlementController.class,
      PeriodController.class
    })
public class AccountingApplicationExceptionAdvice {

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    if (ex != null
        && ex.getErrorCode() != null
        && ex.getErrorCode().getCode().startsWith("CONC_")) {
      return AccountingApplicationExceptionResponses.mappedStatus(ex, request);
    }
    return AccountingApplicationExceptionResponses.badRequest(ex, request);
  }
}
