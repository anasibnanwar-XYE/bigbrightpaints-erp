# Auth Services

## AuthService
**Location:** `service/AuthService.java`

Core authentication service handling login, logout, and token refresh flows.

### Dependencies
- `AuthenticationManager` - Spring Security authentication
- `JwtTokenService` - JWT generation and parsing
- `RefreshTokenService` - Refresh token lifecycle
- `MfaService` - MFA verification during login
- `TokenBlacklistService` - Token revocation
- `TenantRuntimeEnforcementService` - Tenant access control

### Key Methods

| Method | Description |
|--------|-------------|
| `login(LoginRequest)` | Authenticate user, verify MFA, generate tokens |
| `refresh(RefreshTokenRequest)` | Exchange refresh token for new access token |
| `logout(String refreshToken, String accessToken)` | Revoke all user sessions |

### Security Features
- **Account Lockout**: 5 failed attempts → 15-minute lockout
- **Session Revocation**: Lockout triggers revocation of all active sessions
- **Audit Logging**: All login success/failure events logged
- **Tenant Enforcement**: Validates tenant access via `TenantRuntimeEnforcementService`

---

## MfaService
**Location:** `service/MfaService.java`

TOTP-based multi-factor authentication implementation.

### Configuration
- `security.mfa.issuer` - Issuer name for authenticator apps (default: "BigBright ERP")
- 8 recovery codes generated per enrollment
- 30-second time step for TOTP validation
- 1-step clock drift tolerance

### Key Methods

| Method | Description |
|--------|-------------|
| `beginEnrollment(UserAccount)` | Generate TOTP secret and recovery codes |
| `activate(UserAccount, String code)` | Verify and enable MFA |
| `disable(UserAccount, String totpCode, String recoveryCode)` | Disable MFA with verification |
| `verifyDuringLogin(UserAccount, String totpCode, String recoveryCode)` | Verify MFA during authentication |

### Implementation Notes
- MFA secret encrypted using `CryptoService` before database storage
- Recovery codes stored as BCrypt hashes
- TOTP validation uses HMAC-SHA1 with 6-digit codes

---

## PasswordResetService
**Location:** `service/PasswordResetService.java`

Self-service password reset via email tokens.

### Configuration
- Reset token TTL: 1 hour
- Global identity policy: one user identity spans all company memberships

### Key Methods

| Method | Description |
|--------|-------------|
| `requestReset(String email)` | Generate and email reset token |
| `requestResetForSuperAdmin(String email)` | Super-admin specific reset flow |
| `requestResetByAdmin(UserAccount)` | Admin-initiated reset |
| `resetPassword(String token, String newPassword, String confirmPassword)` | Consume token and reset password |

### Transaction Management
- Uses `REQUIRES_NEW` propagation for token lifecycle operations
- After-commit email dispatch for consistency
- Automatic cleanup of superseded tokens

---

## PasswordService
**Location:** `service/PasswordService.java`

Password change and reset operations with policy enforcement.

### Dependencies
- `PasswordEncoder` - BCrypt password hashing
- `PasswordPolicy` - Password validation rules
- `TokenBlacklistService` - Session revocation
- `RefreshTokenService` - Refresh token cleanup

### Key Methods

| Method | Description |
|--------|-------------|
| `changePassword(UserAccount, ChangePasswordRequest)` | Self-service password change |
| `resetPassword(UserAccount, String newPassword, String confirmPassword)` | Token-based password reset |

### Password History
- Tracks last 5 passwords per user
- Prevents password reuse
- History stored in `UserPasswordHistory` entity

---

## PasswordPolicy
**Location:** `service/PasswordPolicy.java`

Password validation rules (stateless component).

### Rules
- Minimum 10 characters
- At least one lowercase letter
- At least one uppercase letter
- At least one digit
- At least one special character
- No whitespace allowed

```java
List<String> validate(String password) // Returns list of violation messages
```

---

## RefreshTokenService
**Location:** `service/RefreshTokenService.java`

Long-lived token management for session persistence.

### Key Methods

| Method | Description |
|--------|-------------|
| `issue(String userEmail, Instant issuedAt, Instant expiresAt)` | Create new refresh token |
| `consume(String refreshToken)` | Validate and consume token (single-use) |
| `revoke(String refreshToken)` | Revoke specific token |
| `revokeAllForUser(String userEmail)` | Revoke all user tokens |
| `cleanupExpiredTokens()` | Scheduled cleanup (hourly) |

### Token Storage
- Tokens stored as SHA-256 digests only (no plaintext)
- Digest format: `sha256Hex("refresh-token:" + token)`

---

## UserProfileService
**Location:** `service/UserProfileService.java`

User profile viewing and updates.

### Key Methods

| Method | Description |
|--------|-------------|
| `view(UserAccount)` | Get profile data |
| `update(UserAccount, UpdateProfileRequest)` | Update profile fields |

---

## UserAccountDetailsService
**Location:** `service/UserAccountDetailsService.java`

Spring Security `UserDetailsService` implementation.

```java
UserDetails loadUserByUsername(String username) // Email-based user lookup
```

Returns `UserPrincipal` wrapping `UserAccount`.

---

## TenantAdminProvisioningService
**Location:** `service/TenantAdminProvisioningService.java`

Provisions initial tenant administrators during onboarding.

### Key Methods

| Method | Description |
|--------|-------------|
| `provisionInitialAdmin(Company, String email, String displayName)` | Create first admin for new tenant |
| `resetTenantAdminPassword(Company, String adminEmail)` | Admin password reset with email notification |
| `isCredentialEmailDeliveryEnabled()` | Check if email delivery is configured |

### Provisioning Flow
1. Generate 14-character temporary password
2. Set `mustChangePassword=true`
3. Assign `ROLE_ADMIN`
4. Email credentials to new admin

---

## AuthTokenDigests
**Location:** `service/AuthTokenDigests.java`

Utility class for generating token digests (package-private).

```java
static String refreshTokenDigest(String token)      // SHA-256 of "refresh-token:" + token
static String passwordResetTokenDigest(String token) // SHA-256 of "password-reset-token:" + token
```

---

## AuthSecretStorageBackfillRunner
**Location:** `service/AuthSecretStorageBackfillRunner.java`

Application runner that logs retirement of legacy secret storage (no-op).
