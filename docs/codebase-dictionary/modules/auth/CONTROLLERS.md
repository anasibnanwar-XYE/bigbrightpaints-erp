# Auth Controllers

## AuthController
**Location:** `controller/AuthController.java`
**Base Path:** `/api/v1/auth`

Primary authentication endpoint handling login, logout, token refresh, and password management.

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/login` | Authenticate user and issue tokens | No |
| POST | `/refresh-token` | Refresh access token using refresh token | No |
| POST | `/logout` | Revoke tokens and invalidate session | Yes |
| GET | `/me` | Get current user identity and permissions | Yes |
| POST | `/password/change` | Change password for authenticated user | Yes |
| POST | `/password/forgot` | Request password reset email | No |
| POST | `/password/reset` | Reset password using token | No |

### Request/Response Models

#### POST /login
```java
// Request: LoginRequest
record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String companyCode,
    String mfaCode,      // Optional: TOTP code if MFA enabled
    String recoveryCode  // Optional: Recovery code if MFA enabled
)

// Response: AuthResponse
record AuthResponse(
    String tokenType,        // "Bearer"
    String accessToken,      // JWT access token
    String refreshToken,     // Long-lived refresh token
    long expiresIn,          // Access token TTL in seconds
    String companyCode,      // Resolved company code
    String displayName,      // User display name
    boolean mustChangePassword  // Force password change flag
)
```

#### GET /me
```java
// Response: MeResponse
record MeResponse(
    String email,
    String displayName,
    String companyCode,
    boolean mfaEnabled,
    boolean mustChangePassword,
    List<String> roles,
    List<String> permissions
)
```

---

## MfaController
**Location:** `controller/MfaController.java`
**Base Path:** `/api/v1/auth/mfa`

MFA enrollment and management for authenticated users.

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/setup` | Begin MFA enrollment | Yes |
| POST | `/activate` | Activate MFA with verification code | Yes |
| POST | `/disable` | Disable MFA (requires code or recovery code) | Yes |

### Request/Response Models

#### POST /setup
```java
// Response: MfaSetupResponse
record MfaSetupResponse(
    String secret,           // Base32 TOTP secret
    String qrUri,            // otpauth:// URI for QR code
    List<String> recoveryCodes  // 8 one-time recovery codes
)
```

#### POST /activate
```java
// Request: MfaActivateRequest
record MfaActivateRequest(
    @NotBlank @Pattern(regexp = "\\d{6}") String code
)
```

#### POST /disable
```java
// Request: MfaDisableRequest
record MfaDisableRequest(
    String code,          // TOTP code (mutually exclusive with recoveryCode)
    String recoveryCode   // Recovery code (mutually exclusive with code)
)
```

---

## UserProfileController
**Location:** `controller/UserProfileController.java`
**Base Path:** `/api/v1/auth/profile`

User profile management for authenticated users.

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/` | View current user profile | Yes |
| PUT | `/` | Update user profile | Yes |

### Request/Response Models

```java
// Response: ProfileResponse
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

// Request: UpdateProfileRequest
record UpdateProfileRequest(
    @Size(min = 1, max = 255) String displayName,
    @Size(max = 255) String preferredName,
    @Size(max = 255) String jobTitle,
    @Size(max = 512) String profilePictureUrl,
    @Size(max = 64) String phoneSecondary,
    @Email @Size(max = 255) String secondaryEmail
)
```

## Security Notes
- All endpoints except `/login`, `/refresh-token`, `/password/forgot`, and `/password/reset` require authentication
- MFA enrollment requires verification before activation
- Password reset tokens expire after 1 hour
