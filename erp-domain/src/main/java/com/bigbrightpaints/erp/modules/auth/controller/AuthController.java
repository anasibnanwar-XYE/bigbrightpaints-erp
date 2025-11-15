package com.bigbrightpaints.erp.modules.auth.controller;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.AuthService;
import com.bigbrightpaints.erp.modules.auth.service.PasswordService;
import com.bigbrightpaints.erp.modules.auth.web.AuthResponse;
import com.bigbrightpaints.erp.modules.auth.web.LoginRequest;
import com.bigbrightpaints.erp.modules.auth.web.MeResponse;
import com.bigbrightpaints.erp.modules.auth.web.RefreshTokenRequest;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;

    public AuthController(AuthService authService,
                          PasswordService passwordService) {
        this.authService = authService;
        this.passwordService = passwordService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam(required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
        }
        String companyId = CompanyContextHolder.getCompanyId();
        List<String> roles = principal.getUser().getRoles().stream()
                .map(role -> role.getName())
                .sorted()
                .toList();
        List<String> permissions = principal.getUser().getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        MeResponse payload = new MeResponse(principal.getUsername(), principal.getUser().getDisplayName(),
                companyId, principal.getUser().isMfaEnabled(), roles, permissions);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<String>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
        }
        passwordService.changePassword(principal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "OK"));
    }
}
