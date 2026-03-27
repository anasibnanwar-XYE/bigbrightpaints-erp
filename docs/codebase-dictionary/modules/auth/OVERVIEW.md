# Auth Module Overview

## Purpose
The Auth module handles authentication, authorization, and identity management for BigBrightPaints ERP. It provides login, MFA (Multi-Factor Authentication), password management, token refresh, and user profile management.

## Location
`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`

## Architecture

```
modules/auth/
├── controller/           # REST endpoints
│   ├── AuthController.java
│   ├── MfaController.java
│   └── UserProfileController.java
├── service/              # Business logic
│   ├── AuthService.java
│   ├── MfaService.java
│   ├── PasswordResetService.java
│   ├── PasswordService.java
│   ├── PasswordPolicy.java
│   ├── RefreshTokenService.java
│   ├── UserProfileService.java
│   ├── UserAccountDetailsService.java
│   ├── TenantAdminProvisioningService.java
│   ├── AuthTokenDigests.java
│   └── AuthSecretStorageBackfillRunner.java
├── domain/               # JPA entities
│   ├── UserAccount.java
│   ├── UserPrincipal.java
│   ├── RefreshToken.java
│   ├── PasswordResetToken.java
│   ├── BlacklistedToken.java
│   ├── MfaRecoveryCode.java
│   ├── UserPasswordHistory.java
│   ├── UserTokenRevocation.java
│   └── repositories (in domain package)
├── web/                  # Request/Response DTOs
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── RefreshTokenRequest.java
│   ├── MeResponse.java
│   ├── ProfileResponse.java
│   ├── ChangePasswordRequest.java
│   ├── ForgotPasswordRequest.java
│   ├── ResetPasswordRequest.java
│   ├── MfaSetupResponse.java
│   ├── MfaActivateRequest.java
│   ├── MfaDisableRequest.java
│   └── UpdateProfileRequest.java
└── exception/             # Custom exceptions
    ├── MfaRequiredException.java
    └── InvalidMfaException.java
```

## Key Features

### 1. Login Flow
- Email/password authentication with company context
- Account lockout after 5 failed attempts (15-minute lockout)
- JWT token generation with company code claims
- MFA verification during login

### 2. Multi-Factor Authentication (MFA)
- TOTP-based authenticator app support
- 8 recovery codes generated at enrollment
- MFA secret encrypted at rest using CryptoService
- Supports both TOTP code and recovery code during login

### 3. Token Management
- JWT access tokens with configurable TTL
- Refresh tokens stored as SHA-256 digests (not plaintext)
- Token blacklisting for logout/revocation
- Automatic expired token cleanup (hourly)

### 4. Password Management
- Password policy enforcement (10+ chars, mixed case, digit, special char)
- Password history tracking (last 5 passwords)
- Self-service password reset via email tokens
- Admin-initiated password reset for tenant admins

### 5. Session Management
- Global identity policy (user spans all company memberships)
- Forced password change corridor for new/reset passwords
- Complete session revocation on password change

## Dependencies
- `core/security/JwtTokenService` - JWT token generation
- `core/security/TokenBlacklistService` - Token revocation
- `core/security/CryptoService` - MFA secret encryption
- `core/notification/EmailService` - Password reset emails
- `modules/company/service/TenantRuntimeEnforcementService` - Tenant access control
- `modules/rbac` - Role and permission management

## Security Considerations
- All tokens stored as SHA-256 digests (no plaintext token storage)
- MFA secrets encrypted using CryptoService before database storage
- Failed login tracking with automatic account lockout
- Password reuse prevention (last 5 passwords)
- Audit logging for all authentication events
- Tenant runtime enforcement integration for multi-tenant access control
