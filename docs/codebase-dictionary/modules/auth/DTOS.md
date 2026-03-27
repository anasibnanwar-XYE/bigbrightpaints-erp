# Auth DTOs

## Location
`modules/auth/web/` and `modules/auth/dto/`

## Request DTOs

### LoginRequest
**Location:** `web/LoginRequest.java`
```java
record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String companyCode,
    String mfaCode,        // Optional: TOTP code if MFA enabled
    String recoveryCode    // Optional: Recovery code if MFA enabled
)
```

### RefreshTokenRequest
**Location:** `web/RefreshTokenRequest.java`
```java
record RefreshTokenRequest(
    @NotBlank String refreshToken,
    @NotBlank String companyCode
)
```

### ChangePasswordRequest
**Location:** `web/ChangePasswordRequest.java`
```java
record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword,
    @NotBlank String confirmPassword
)
```

### ForgotPasswordRequest
**Location:** `web/ForgotPasswordRequest.java`
```java
record ForgotPasswordRequest(
    @JsonAlias({"userid", "userId"}) @Email @NotBlank String email
)
```

### ResetPasswordRequest
**Location:** `web/ResetPasswordRequest.java`
```java
record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank String newPassword,
    @NotBlank String confirmPassword
)
```

### MfaActivateRequest
**Location:** `web/MfaActivateRequest.java`
```java
record MfaActivateRequest(
    @NotBlank @Pattern(regexp = "\\d{6}", message = "MFA code must be 6 digits") String code
)
```

### MfaDisableRequest
**Location:** `web/MfaDisableRequest.java`
```java
record MfaDisableRequest(
    String code,          // TOTP code (mutually exclusive with recoveryCode)
    String recoveryCode   // Recovery code (mutually exclusive with code)
) {
    @AssertTrue(message = "Provide either code or recoveryCode")
    boolean hasVerifier()
}
```

### UpdateProfileRequest
**Location:** `web/UpdateProfileRequest.java`
```java
record UpdateProfileRequest(
    @Size(min = 1, max = 255) String displayName,
    @Size(max = 255) String preferredName,
    @Size(max = 255) String jobTitle,
    @Size(max = 512) String profilePictureUrl,
    @Size(max = 64) String phoneSecondary,
    @Email @Size(max = 255) String secondaryEmail
)
```

---

## Response DTOs

### AuthResponse
**Location:** `web/AuthResponse.java`
```java
record AuthResponse(
    String tokenType,           // "Bearer"
    String accessToken,         // JWT access token
    String refreshToken,        // Long-lived refresh token
    long expiresIn,             // Access token TTL in seconds
    String companyCode,         // Resolved company code
    String displayName,         // User display name
    boolean mustChangePassword  // Force password change flag
)
```

### MeResponse
**Location:** `web/MeResponse.java`
```java
record MeResponse(
    String email,
    String displayName,
    String companyCode,
    boolean mfaEnabled,
    boolean mustChangePassword,
    List<String> roles,
    List<String> permissions
) {
    @Deprecated
    @JsonProperty("companyId")
    String legacyCompanyId() { return companyCode; }  // Backward compatibility
}
```

### ProfileResponse
**Location:** `web/ProfileResponse.java`
```java
record ProfileResponse(
    String email,
    String displayName,
    String preferredName,
    String jobTitle,
    String profilePictureUrl,
    String phoneSecondary,
    String secondaryEmail,
    boolean mfaEnabled,
    List<String> companies,  // Company codes user belongs to
    Instant createdAt,
    UUID publicId
)
```

### MfaSetupResponse
**Location:** `web/MfaSetupResponse.java`
```java
record MfaSetupResponse(
    String secret,           // Base32 TOTP secret
    String qrUri,            // otpauth:// URI for QR code generation
    List<String> recoveryCodes  // 8 one-time recovery codes
)
```

---

## Legacy DTOs (in dto/ package)

### ResetPasswordRequest (Legacy)
**Location:** `dto/ResetPasswordRequest.java`
Duplicate of `web/ResetPasswordRequest.java` - prefer web version.

### ForgotPasswordRequest (Legacy)
**Location:** `dto/ForgotPasswordRequest.java`
Duplicate of `web/ForgotPasswordRequest.java` - prefer web version.

---

## Validation Notes

| DTO | Validation Rules |
|-----|------------------|
| LoginRequest | Email format, all core fields required |
| RefreshTokenRequest | Both fields required |
| ChangePasswordRequest | All fields required, newPassword must match confirmPassword |
| ResetPasswordRequest | Token required, passwords must match |
| MfaActivateRequest | Exactly 6 digits |
| MfaDisableRequest | At least one verifier required |
| UpdateProfileRequest | Size constraints on all optional fields |
