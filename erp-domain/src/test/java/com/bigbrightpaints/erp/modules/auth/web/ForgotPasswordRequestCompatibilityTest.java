package com.bigbrightpaints.erp.modules.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ForgotPasswordRequestCompatibilityTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void mapsUseridAliasToEmail() throws Exception {
    ForgotPasswordRequest request =
        objectMapper.readValue("{\"userid\":\"superadmin@bbp.com\"}", ForgotPasswordRequest.class);

    assertThat(request.email()).isEqualTo("superadmin@bbp.com");
  }

  @Test
  void mapsUserIdAliasToEmail() throws Exception {
    ForgotPasswordRequest request =
        objectMapper.readValue("{\"userId\":\"superadmin@bbp.com\"}", ForgotPasswordRequest.class);

    assertThat(request.email()).isEqualTo("superadmin@bbp.com");
  }
}
