# Auth Entities

## UserAccount
**Location:** `domain/UserAccount.java`
**Table:** `app_users`

Core user identity entity spanning all company memberships.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK, auto-generated | Surrogate key |
| `publicId` | UUID | NOT NULL | External identifier |
| `email` | String | NOT NULL, UNIQUE | Login email |
| `passwordHash` | String | NOT NULL | BCrypt hash |
| `displayName` | String | NOT NULL | Display name |
| `enabled` | boolean | NOT NULL, default true | Account status |
| `createdAt` | Instant | NOT NULL | Creation timestamp |
| `mfaSecret` | String | NULL | Encrypted TOTP secret |
| `mfaEnabled` | boolean | NOT NULL, default false | MFA status |
| `mfaRecoveryCodes` | String | NULL | Comma-separated BCrypt hashes |
| `preferredName` | String | NULL | Optional preferred name |
| `jobTitle` | String | NULL | Optional job title |
| `profilePictureUrl` | String | NULL | Profile picture URL |
| `phoneSecondary` | String | NULL | Secondary phone |
| `secondaryEmail` | String | NULL | Secondary email |
| `failedLoginAttempts` | int | NOT NULL, default 0 | Failed attempt counter |
| `lockedUntil` | Instant | NULL | Lockout expiration |
| `mustChangePassword` | boolean | NOT NULL, default false | Force password change |

### Relationships

| Relationship | Type | Target |
|--------------|------|--------|
| `roles` | Many-to-Many (EAGER) | `Role` |
| `companies` | Many-to-Many (EAGER) | `Company` |

---

## UserPrincipal
**Location:** `domain/UserPrincipal.java`

Spring Security `UserDetails` implementation wrapping `UserAccount`.

### Implementation
- `getAuthorities()` - Flattens role names and permissions
- `isAccountNonLocked()` - Checks `lockedUntil` against current time
- `isEnabled()` - Delegates to `UserAccount.enabled`

---

## RefreshToken
**Location:** `domain/RefreshToken.java`
**Table:** `refresh_tokens`

Long-lived token for session persistence.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `token` | String | NULL | Legacy plaintext (deprecated) |
| `tokenDigest` | String | Length 64 | SHA-256 digest |
| `userEmail` | String | NOT NULL, Length 255 | Owner email |
| `issuedAt` | Instant | NOT NULL | Issuance timestamp |
| `expiresAt` | Instant | NOT NULL | Expiration timestamp |

### Indexes
- `idx_refresh_tokens_token_digest` on `token_digest`
- `idx_refresh_tokens_user_email` on `user_email`
- `idx_refresh_tokens_expires_at` on `expires_at`

### Factory Method
```java
static RefreshToken digestOnly(String tokenDigest, String userEmail, Instant issuedAt, Instant expiresAt)
```

---

## PasswordResetToken
**Location:** `domain/PasswordResetToken.java`
**Table:** `password_reset_tokens`

Self-service password reset tokens.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `user` | UserAccount | FK, NOT NULL | Target user |
| `token` | String | Length 255 | Legacy plaintext (deprecated) |
| `tokenDigest` | String | Length 64 | SHA-256 digest |
| `expiresAt` | Instant | NOT NULL | Expiration (1 hour) |
| `usedAt` | Instant | NULL | Consumption timestamp |
| `createdAt` | Instant | NOT NULL | Creation timestamp |
| `deliveredAt` | Instant | NULL | Email delivery confirmation |

### Indexes
- `idx_password_reset_tokens_token_digest` on `token_digest`

### Factory Method
```java
static PasswordResetToken digestOnly(UserAccount user, String tokenDigest, Instant expiresAt)
```

---

## BlacklistedToken
**Location:** `domain/BlacklistedToken.java`
**Table:** `blacklisted_tokens`

Revoked JWT access tokens.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `tokenId` | String | NOT NULL, UNIQUE | JWT ID (jti claim) |
| `userId` | String | NULL | User email |
| `expiresAt` | Instant | NOT NULL | Token expiration |
| `blacklistedAt` | Instant | NOT NULL | Revocation timestamp |
| `reason` | String | NULL | Revocation reason |

### Indexes
- `idx_blacklisted_tokens_token_id` on `token_id`
- `idx_blacklisted_tokens_user_id` on `user_id`
- `idx_blacklisted_tokens_expires_at` on `expires_at`

---

## MfaRecoveryCode
**Location:** `domain/MfaRecoveryCode.java`
**Table:** `mfa_recovery_codes`

One-time MFA recovery codes (currently stored inline in UserAccount).

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `user` | UserAccount | FK, NOT NULL | Owner |
| `codeHash` | String | NOT NULL | BCrypt hash |
| `usedAt` | LocalDateTime | NULL | Usage timestamp |
| `createdAt` | LocalDateTime | NOT NULL | Creation timestamp |

---

## UserPasswordHistory
**Location:** `domain/UserPasswordHistory.java`
**Table:** `user_password_history`

Password history for reuse prevention.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `user` | UserAccount | FK, NOT NULL | Owner |
| `passwordHash` | String | NOT NULL | BCrypt hash |
| `changedAt` | Instant | NOT NULL | Change timestamp |

---

## UserTokenRevocation
**Location:** `domain/UserTokenRevocation.java`
**Table:** `user_token_revocations`

Global token revocation records.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `userId` | String | NOT NULL, UNIQUE | User email |
| `revokedAt` | Instant | NOT NULL | Revocation timestamp |
| `reason` | String | NULL | Revocation reason |

### Index
- `idx_user_token_revocations_user_id` on `user_id`

---

## Exception Classes

### MfaRequiredException
**Location:** `exception/MfaRequiredException.java`

Signals that login requires MFA verification.

### InvalidMfaException
**Location:** `exception/InvalidMfaException.java`

Signals that provided MFA code or recovery code was invalid.
