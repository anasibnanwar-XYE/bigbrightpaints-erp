package com.bigbrightpaints.erp.modules.company.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OwnerSetupInviteTeamRequest(
    Boolean skip, List<@NotNull @Valid Invitation> invitations) {
  public record Invitation(
      @Email @NotBlank String email, @NotBlank String displayName, String role) {}
}
