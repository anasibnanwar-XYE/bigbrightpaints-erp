package com.bigbrightpaints.erp.modules.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPasswordChangeResponseDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileSessionDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileUpdateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSessionRevokeResponseDto;
import com.bigbrightpaints.erp.modules.admin.service.SuperAdminProfileService;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;

class SuperAdminProfileControllerTest {

  @Test
  void profileController_isSuperAdminOnlyAndDelegatesSafeShellOperations() throws Exception {
    PreAuthorize annotation = SuperAdminProfileController.class.getAnnotation(PreAuthorize.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo("hasAuthority('ROLE_SUPER_ADMIN')");

    SuperAdminProfileService service = mock(SuperAdminProfileService.class);
    SuperAdminProfileController controller = new SuperAdminProfileController(service);
    SuperAdminProfileDto profile =
        new SuperAdminProfileDto(
            "Platform Admin",
            "platform@example.com",
            "+10000000000",
            "https://cdn.example/avatar.png",
            "UTC",
            "en",
            Instant.parse("2026-04-28T09:00:00Z"),
            List.of(
                new SuperAdminProfileSessionDto(
                    "refresh:10",
                    "PLATFORM",
                    Instant.parse("2026-04-28T09:00:00Z"),
                    Instant.parse("2026-05-28T09:00:00Z"),
                    true,
                    "not-captured",
                    "redacted")));
    SuperAdminProfileUpdateRequest update =
        new SuperAdminProfileUpdateRequest(
            "Platform Admin", "+19999999999", "https://cdn.example/new.png", "Asia/Kolkata", "en");
    ChangePasswordRequest passwordRequest =
        new ChangePasswordRequest("Current!234", "Changed!234", "Changed!234");
    SuperAdminPasswordChangeResponseDto passwordResponse =
        new SuperAdminPasswordChangeResponseDto(
            "PASSWORD_CHANGED",
            Instant.parse("2026-04-28T09:05:00Z"),
            "all-user-sessions-revoked",
            "audit:event=PASSWORD_CHANGED");
    SuperAdminSessionRevokeResponseDto revokeResponse =
        new SuperAdminSessionRevokeResponseDto(
            "refresh:10",
            true,
            Instant.parse("2026-04-28T09:10:00Z"),
            "audit:event=CONFIGURATION_CHANGED");

    when(service.profile(null)).thenReturn(profile);
    when(service.updateProfile(null, update)).thenReturn(profile);
    when(service.changePassword(null, passwordRequest)).thenReturn(passwordResponse);
    when(service.sessions(null)).thenReturn(profile.sessions());
    when(service.revokeSession(null, "refresh:10")).thenReturn(revokeResponse);

    assertThat(controller.getProfile(null).getBody().data()).isEqualTo(profile);
    assertThat(controller.updateProfile(null, update).getBody().data()).isEqualTo(profile);
    assertThat(controller.changePassword(null, passwordRequest).getBody().data())
        .isEqualTo(passwordResponse);
    assertThat(controller.sessions(null).getBody().data()).isEqualTo(profile.sessions());
    assertThat(controller.revokeSession(null, "refresh:10").getBody().data())
        .isEqualTo(revokeResponse);

    verify(service).profile(null);
    verify(service).updateProfile(null, update);
    verify(service).changePassword(null, passwordRequest);
    verify(service).sessions(null);
    verify(service).revokeSession(null, "refresh:10");
  }
}
