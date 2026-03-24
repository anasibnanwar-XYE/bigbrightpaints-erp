package com.bigbrightpaints.erp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.util.CompanyTime;

@RestController
@RequestMapping("/api/integration")
public class IntegrationHealthController {

  @GetMapping("/health")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of("status", "UP", "timestamp", CompanyTime.now().toString()));
  }
}
