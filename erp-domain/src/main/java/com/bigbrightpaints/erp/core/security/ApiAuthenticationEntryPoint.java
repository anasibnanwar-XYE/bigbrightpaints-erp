package com.bigbrightpaints.erp.core.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bigbrightpaints.erp.core.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final SecurityErrorResponseWriter responseWriter;

  public ApiAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {
    this.responseWriter = responseWriter;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    responseWriter.write(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorCode.AUTH_TOKEN_INVALID,
        ErrorCode.AUTH_TOKEN_INVALID.getDefaultMessage());
  }
}
