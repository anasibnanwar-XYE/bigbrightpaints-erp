package com.bigbrightpaints.erp.modules.auth.controller;

import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.UserProfileService;
import com.bigbrightpaints.erp.modules.auth.web.ProfileResponse;
import com.bigbrightpaints.erp.modules.auth.web.UpdateProfileRequest;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/profile")
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> profile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(userProfileService.view(principal.getUser())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> update(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
        }
        ProfileResponse updated = userProfileService.update(principal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }
}
