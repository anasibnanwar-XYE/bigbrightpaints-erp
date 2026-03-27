# Core Infrastructure: Security

This document covers all security classes in `com.bigbrightpaints.erp.core.security`.

## Overview

Security classes handle authentication, authorization, JWT token management, company context enforcement, tenant runtime protection, and security monitoring. They implement a multi-layered defense with filters, services, and monitoring components.

---

## SecurityConfig

| Field | Value |
|-------|-------|
| **Name** | SecurityConfig |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/SecurityConfig.java |
| **Responsibility** | Main Spring Security configuration with filter chain and role hierarchy |
| **Use when** | Understanding security filter order and permissions |
| **Do not use when** | N/A |
| **Public methods** | `@Bean SecurityFilterChain securityFilterChain(HttpSecurity http)`<br>`@Bean PasswordEncoder passwordEncoder()`<br>`@Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)`<br>`@Bean RoleHierarchy roleHierarchy()`<br>`@Bean MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy)` |
| **Callers** | Spring Security framework |
| **Dependencies** | JwtAuthenticationFilter, CompanyContextFilter, MustChangePasswordCorridorFilter, UserAccountDetailsService |
| **Side effects** | Configures security filter chain |
| **Invariants protected** | Stateless session; JWT-only auth; CSRF disabled for REST API |
| **Status** | Canonical |

### Filter Chain Order

1. JwtAuthenticationFilter (validates token, sets authentication)
2. CompanyContextFilter (validates company access, tenant lifecycle)
3. MustChangePasswordCorridorFilter (enforces password change requirement)

### Public Endpoints

| Endpoint | Description |
|----------|-------------|
| /api/v1/auth/login | User login |
| /api/v1/auth/refresh-token | Token refresh |
| /api/v1/auth/password/forgot | Password reset request |
| /api/v1/auth/password/reset | Password reset completion |
| /api/v1/changelog | Changelog access |
| /actuator/health | Health check |

---

## CompanyContextHolder

| Field | Value |
|-------|-------|
| **Name** | CompanyContextHolder |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextHolder.java |
| **Responsibility** | Thread-local storage for current company code |
| **Use when** | Accessing company context from services without explicit Company parameter |
| **Do not use when** | Company is already in scope |
| **Public methods** | `static void setCompanyCode(String companyCode)`<br>`static String getCompanyCode()`<br>`static void clear()`<br>`@Deprecated static void setCompanyId(String companyId)`<br>`@Deprecated static String getCompanyId()` |
| **Callers** | CompanyContextFilter, AsyncConfig, services requiring company context |
| **Dependencies** | None |
| **Side effects** | Thread-local modification |
| **Invariants protected** | Thread isolation; context cleanup |
| **Status** | Canonical |

---

## CompanyContextFilter

| Field | Value |
|-------|-------|
| **Name** | CompanyContextFilter |
| **Type** | Filter (Spring OncePerRequestFilter) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java |
| **Responsibility** | Validates company access, tenant lifecycle state, and runtime admission |
| **Use when** | Understanding why requests are denied for company/tenant reasons |
| **Do not use when** | N/A |
| **Public methods** | `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`<br>`protected boolean shouldNotFilter(HttpServletRequest request)` |
| **Callers** | Spring Security filter chain |
| **Dependencies** | TenantRuntimeEnforcementService, CompanyService, ObjectMapper |
| **Side effects** | Sets CompanyContextHolder; tenant admission tracking |
| **Invariants protected** | Company membership; tenant lifecycle state (ACTIVE/SUSPENDED/DEACTIVATED); super-admin platform-only access |
| **Status** | Canonical |

### Access Control Logic

1. **Public Password Reset**: Skips all checks for /api/v1/auth/password/* endpoints
2. **Super-Admin Restrictions**: Blocked from tenant business workflows (sales, inventory, etc.)
3. **Company Context Validation**: Token company code must match X-Company-Code header
4. **Lifecycle Enforcement**: 
   - ACTIVE: All requests allowed
   - SUSPENDED: Read-only (GET/HEAD/OPTIONS)
   - DEACTIVATED: All requests denied
5. **Runtime Admission**: Concurrent request limits, rate limits

### Error Responses

| Code | Message |
|------|---------|
| COMPANY_CONTEXT_MISMATCH | Header does not match authenticated company |
| COMPANY_ACCESS_DENIED | User not member of company |
| TENANT_LIFECYCLE_RESTRICTED | Tenant is suspended or deactivated |
| SUPER_ADMIN_PLATFORM_ONLY | Super-admin cannot access tenant workflows |

---

## JwtAuthenticationFilter

| Field | Value |
|-------|-------|
| **Name** | JwtAuthenticationFilter |
| **Type** | Filter (Spring OncePerRequestFilter) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtAuthenticationFilter.java |
| **Responsibility** | Validates JWT tokens, checks blacklist, and sets authentication |
| **Use when** | Understanding JWT authentication flow |
| **Do not use when** | N/A |
| **Public methods** | `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)` |
| **Callers** | Spring Security filter chain |
| **Dependencies** | JwtTokenService, UserAccountDetailsService, TokenBlacklistService, RoleHierarchy |
| **Side effects** | Sets SecurityContext; stores claims in request attribute |
| **Invariants protected** | Token validity; blacklist check; user revocation check; role hierarchy resolution |
| **Status** | Canonical |

### Token Validation Steps

1. Extract Bearer token from Authorization header
2. Parse and validate JWT signature/expiration
3. Check if token ID is blacklisted
4. Check if user tokens were revoked after token issue time
5. Load UserPrincipal and validate account status
6. Apply role hierarchy for authority resolution

---

## JwtTokenService

| Field | Value |
|-------|-------|
| **Name** | JwtTokenService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtTokenService.java |
| **Responsibility** | Generates and parses JWT access and refresh tokens |
| **Use when** | Creating or validating JWT tokens |
| **Do not use when** | N/A |
| **Public methods** | `String generateAccessToken(String subject, String companyCode, Map<String, Object> claims)`<br>`String generateAccessToken(String subject, String companyCode, Map<String, Object> claims, Instant issuedAt)`<br>`String generateRefreshToken(String subject)`<br>`Claims parse(String token)` |
| **Callers** | AuthController, JwtAuthenticationFilter |
| **Dependencies** | JwtProperties |
| **Side effects** | None |
| **Invariants protected** | 256-bit minimum secret; unique token IDs (jti); companyCode claim |
| **Status** | Canonical |

### Token Claims

| Claim | Description |
|-------|-------------|
| sub | User ID |
| jti | Unique token ID (UUID) |
| companyCode | User's company code |
| iatMs | Issued-at timestamp in milliseconds |
| exp | Expiration timestamp |

---

## JwtProperties

| Field | Value |
|-------|-------|
| **Name** | JwtProperties |
| **Type** | Configuration Properties |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtProperties.java |
| **Responsibility** | Binds JWT configuration with secret validation |
| **Use when** | Understanding JWT token TTLs and secret requirements |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters; `@PostConstruct void validate()` |
| **Callers** | JwtTokenService, TokenBlacklistService |
| **Dependencies** | Spring Environment |
| **Side effects** | Throws on invalid configuration |
| **Invariants protected** | Secret ≥32 bytes; no unsafe static secrets in non-test profiles |
| **Status** | Canonical |

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| jwt.secret | (required) | Signing secret (≥256 bits) |
| jwt.access-token-ttl-seconds | 900 | Access token TTL (15 min) |
| jwt.refresh-token-ttl-seconds | 2592000 | Refresh token TTL (30 days) |

---

## TokenBlacklistService

| Field | Value |
|-------|-------|
| **Name** | TokenBlacklistService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TokenBlacklistService.java |
| **Responsibility** | Manages JWT token blacklist and user revocation |
| **Use when** | Logging out, revoking sessions, checking token validity |
| **Do not use when** | N/A |
| **Public methods** | `void blacklistToken(String tokenId, Instant expirationTime)`<br>`void blacklistToken(String tokenId, Instant expirationTime, String userId, String reason)`<br>`void revokeAllUserTokens(String userId)`<br>`boolean isTokenBlacklisted(String tokenId)`<br>`boolean isUserTokenRevoked(String userId, Instant tokenIssuedAt)`<br>`void removeFromBlacklist(String tokenId)`<br>`void clearUserRevocation(String userId)`<br>`long getBlacklistedTokenCount()`<br>`long getRevokedUserCount()`<br>`@Scheduled void cleanupExpiredTokens()`<br>`void clearAll()` |
| **Callers** | AuthController, JwtAuthenticationFilter, SecurityMonitoringService |
| **Dependencies** | BlacklistedTokenRepository, UserTokenRevocationRepository, JwtProperties |
| **Side effects** | Database writes; scheduled cleanup |
| **Invariants protected** | Revocation timestamp precision (milliseconds); retention ≥ refresh token TTL |
| **Status** | Canonical |

---

## MustChangePasswordCorridorFilter

| Field | Value |
|-------|-------|
| **Name** | MustChangePasswordCorridorFilter |
| **Type** | Filter (Spring OncePerRequestFilter) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/MustChangePasswordCorridorFilter.java |
| **Responsibility** | Enforces password change requirement before full API access |
| **Use when** | Understanding why users with mustChangePassword=true have limited access |
| **Do not use when** | N/A |
| **Public methods** | `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)` |
| **Callers** | Spring Security filter chain |
| **Dependencies** | ObjectMapper |
| **Side effects** | Returns 403 if password change required |
| **Invariants protected** | Limited corridor access during password change requirement |
| **Status** | Canonical |

### Corridor Endpoints (Allowed During Password Change)

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/v1/auth/me | View profile |
| GET | /api/v1/auth/profile | View profile |
| POST | /api/v1/auth/password/change | Change password |
| POST | /api/v1/auth/logout | Logout |
| POST | /api/v1/auth/refresh-token | Refresh token |
| OPTIONS | * | CORS preflight |

---

## CryptoService

| Field | Value |
|-------|-------|
| **Name** | CryptoService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CryptoService.java |
| **Responsibility** | Encrypts and decrypts sensitive data at rest using AES-256-GCM |
| **Use when** | Storing sensitive data (API keys, tokens) in database |
| **Do not use when** | Password hashing (use PasswordEncoder) |
| **Public methods** | `String encrypt(String plaintext)`<br>`String decrypt(String encryptedData)`<br>`boolean isEncrypted(String data)` |
| **Callers** | Services storing sensitive configuration |
| **Dependencies** | None (uses javax.crypto) |
| **Side effects** | None |
| **Invariants protected** | Authenticated encryption; random IV and salt per encryption; payload validation |
| **Status** | Canonical |

### Configuration

```properties
erp.security.encryption.key= # Required: 256-bit encryption key
```

### Encryption Format

```
Base64(salt[16] + iv[12] + ciphertext) 
```

---

## TenantRuntimeEnforcementService

| Field | Value |
|-------|-------|
| **Name** | TenantRuntimeEnforcementService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TenantRuntimeEnforcementService.java |
| **Responsibility** | Enforces tenant-level concurrency limits, rate limits, and runtime states |
| **Use when** | Understanding tenant throttling and admission control |
| **Do not use when** | N/A |
| **Public methods** | `AccessHandle acquire(String companyCode, HttpServletRequest request)`<br>`Optional<TenantRuntimeMetricsSnapshot> snapshot(String companyCode)`<br>`Map<String, TenantRuntimeMetricsSnapshot> snapshotAll()`<br>`void evictPolicyCache(String companyCode)` |
| **Callers** | CompanyContextFilter |
| **Dependencies** | CompanyRepository, SystemSettingsRepository, AuditService, EnterpriseAuditTrailService, MeterRegistry |
| **Side effects** | Policy caching; metrics publishing; audit logging |
| **Invariants protected** | Concurrency limits; rate limits per minute; HOLD/BLOCKED state enforcement |
| **Status** | Canonical |

### Tenant Runtime States

| State | Effect |
|-------|--------|
| ACTIVE | All requests allowed |
| HOLD | Only read operations (GET/HEAD/OPTIONS) allowed |
| BLOCKED | All requests denied |

### Configuration

```properties
erp.tenant.runtime.default.max-concurrent-requests=0  # 0 = unlimited
erp.tenant.runtime.default.max-requests-per-minute=0  # 0 = unlimited
erp.tenant.runtime.policy-cache-seconds=15
```

### Database Settings (system_settings table)

| Key Pattern | Example |
|-------------|---------|
| tenant.runtime.hold-state.{companyId} | HOLD, ACTIVE, BLOCKED |
| tenant.runtime.hold-reason.{companyId} | Maintenance mode |
| tenant.runtime.max-concurrent-requests.{companyId} | 50 |
| tenant.runtime.max-requests-per-minute.{companyId} | 1000 |

---

## SecurityMonitoringService

| Field | Value |
|-------|-------|
| **Name** | SecurityMonitoringService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/SecurityMonitoringService.java |
| **Responsibility** | Monitors security events, detects brute force attacks, and rate limit violations |
| **Use when** | Understanding security alerting and monitoring |
| **Do not use when** | N/A |
| **Public methods** | `void recordFailedLogin(String username, String ipAddress)`<br>`void recordSuccessfulLogin(String username, String ipAddress)`<br>`boolean isBlocked(String username, String ipAddress)`<br>`boolean checkRateLimit(String identifier)`<br>`Map<String, Object> getSecurityMetrics()`<br>`@Scheduled void cleanupTracking()`<br>`@Scheduled void analyzeSecurityTrends()` |
| **Callers** | AuthController, rate limiting interceptors |
| **Dependencies** | AuditService, AuditLogRepository, TokenBlacklistService |
| **Side effects** | Blocks users/IPs on threshold breach; revokes tokens |
| **Invariants protected** | Failed login tracking; brute force detection; rate limit enforcement |
| **Status** | Canonical |

### Configuration

```properties
security.monitoring.max-failed-logins=5
security.monitoring.failed-login-window-minutes=15
security.monitoring.max-requests-per-minute=100
security.monitoring.suspicious-activity-threshold=10
```

### Blocking Durations

| Action | Block Duration |
|--------|----------------|
| Brute force (username) | 30 minutes |
| Suspicious IP | 1 hour |

---

## PortalRoleActionMatrix

| Field | Value |
|-------|-------|
| **Name** | PortalRoleActionMatrix |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/PortalRoleActionMatrix.java |
| **Responsibility** | Defines role/action SpEL expressions and access denied messages |
| **Use when** | Understanding role-based access control expressions |
| **Do not use when** | N/A |
| **Public methods** | `static String resolveAccessDeniedMessage(Authentication authentication, HttpServletRequest request)`<br>`static String transporterOrDriverRequiredMessage()`<br>`static String vehicleNumberRequiredMessage()`<br>`static String challanReferenceRequiredMessage()` |
| **Callers** | CoreFallbackExceptionHandler, controllers |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | Role hierarchy; action-specific permissions |
| **Status** | Canonical |

### Role Expression Constants

| Constant | Expression |
|----------|------------|
| DEALER_ONLY | hasAuthority('ROLE_DEALER') |
| ADMIN_ONLY | hasAuthority('ROLE_ADMIN') |
| SUPER_ADMIN_ONLY | hasAuthority('ROLE_SUPER_ADMIN') |
| ADMIN_OR_ACCOUNTING | hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING') |
| ADMIN_SALES_ACCOUNTING | hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING') |
| ADMIN_SALES_FACTORY_ACCOUNTING | hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_FACTORY','ROLE_ACCOUNTING') |
| ADMIN_FACTORY | hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY') |
| ADMIN_FACTORY_SALES | hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES') |

---

## SecurityActorResolver

| Field | Value |
|-------|-------|
| **Name** | SecurityActorResolver |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/SecurityActorResolver.java |
| **Responsibility** | Resolves current authenticated actor for audit attribution |
| **Use when** | Getting actor identifier for audit logs |
| **Do not use when** | N/A |
| **Public methods** | `static String resolveActorOrUnknown()`<br>`static String resolveActorWithSystemProcessFallback()`<br>`static String resolvePrincipalOrUnknown(Principal principal)`<br>`static Authentication systemProcessAuthentication()` |
| **Callers** | AuditService, EnterpriseAuditTrailService |
| **Dependencies** | SecurityContextHolder |
| **Side effects** | None |
| **Invariants protected** | Never returns null; system process detection |
| **Status** | Canonical |

### Actor Resolution Priority

1. Authenticated user's email/name
2. "SYSTEM_PROCESS" if in async thread (erp-async-, orchestrator-scheduler-)
3. "UNKNOWN_AUTH_ACTOR" as final fallback

---

## LicensingGuard

| Field | Value |
|-------|-------|
| **Name** | LicensingGuard |
| **Type** | ApplicationRunner (Spring @Component) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/LicensingGuard.java |
| **Responsibility** | Validates CryptoLens license configuration on startup |
| **Use when** | Understanding license enforcement |
| **Do not use when** | N/A |
| **Public methods** | `void run(ApplicationArguments args)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | LicensingProperties |
| **Side effects** | Throws IllegalStateException on missing license when enforce=true |
| **Invariants protected** | License presence when enforcement enabled |
| **Status** | Scoped (non-test profiles only) |

### Configuration

```properties
erp.licensing.enforce=false
erp.licensing.product-id=31720
erp.licensing.license-key=
erp.licensing.access-token=
```

---

## TenantRuntimeRequestAttributes

| Field | Value |
|-------|-------|
| **Name** | TenantRuntimeRequestAttributes |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.security |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TenantRuntimeRequestAttributes.java |
| **Responsibility** | Defines request attribute keys for tenant runtime enforcement |
| **Use when** | Checking if runtime admission was applied |
| **Do not use when** | N/A |
| **Public methods** | None (constants only) |
| **Callers** | CompanyContextFilter, interceptors |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Constants

| Key | Purpose |
|-----|---------|
| CANONICAL_ADMISSION_APPLIED | Set when CompanyContextFilter applied admission |
| INTERCEPTOR_FALLBACK_ADMISSION | Set by interceptors for fallback admission |
