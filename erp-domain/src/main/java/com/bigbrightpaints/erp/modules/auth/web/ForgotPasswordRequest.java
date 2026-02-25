package com.bigbrightpaints.erp.modules.auth.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @JsonAlias({"userid", "userId"}) @Email @NotBlank String email
) {}
