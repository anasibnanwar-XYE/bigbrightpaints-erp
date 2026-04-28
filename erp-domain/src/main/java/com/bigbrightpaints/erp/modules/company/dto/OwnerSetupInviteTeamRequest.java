package com.bigbrightpaints.erp.modules.company.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OwnerSetupInviteTeamRequest(Boolean skip, @Valid List<Invitation> invitations) {
  public record Invitation(
      @Email @NotBlank String email, @NotBlank String displayName, String role) {}
}
