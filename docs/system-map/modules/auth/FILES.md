# AUTH Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/AuthController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/MfaController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/UserProfileController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/BlacklistedToken.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/BlacklistedTokenRepository.java | JPA repository for BlacklistedToken persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/MfaRecoveryCode.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/MfaRecoveryCodeRepository.java | JPA repository for MfaRecoveryCode persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/PasswordResetToken.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/PasswordResetTokenRepository.java | JPA repository for PasswordResetToken persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/RefreshToken.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/RefreshTokenRepository.java | JPA repository for RefreshToken persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserAccount.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserAccountRepository.java | JPA repository for UserAccount persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserPasswordHistory.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserPasswordHistoryRepository.java | JPA repository for UserPasswordHistory persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserPrincipal.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserTokenRevocation.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/UserTokenRevocationRepository.java | JPA repository for UserTokenRevocation persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/dto/ForgotPasswordRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/dto/ResetPasswordRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/exception/InvalidMfaException.java | module-specific exception definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/exception/MfaRequiredException.java | module-specific exception definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/MfaService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordPolicy.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/RefreshTokenService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/UserAccountDetailsService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/UserProfileService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/AuthResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/ChangePasswordRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/ForgotPasswordRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/LoginRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MeResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MfaActivateRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MfaChallengeResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MfaDisableRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MfaSetupResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/MfaStatusResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/ProfileResponse.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/RefreshTokenRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/ResetPasswordRequest.java | web DTO classes for request and response
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/web/UpdateProfileRequest.java | web DTO classes for request and response
erp-domain/src/main/resources/db/migration/V51__auth_lockout_controls.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V1__core_auth_rbac.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthAuditIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthDisabledUserTokenIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthHardeningIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/MfaControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/ProfileControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/domain/UserPrincipalTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/AuthServiceAuditAttributionTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/MfaServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordPolicyTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/RefreshTokenServiceIT.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/AuthController.java | Login/logout and token APIs
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/MfaController.java | MFA challenge and recovery
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/UserProfileController.java | Profile read/update endpoint

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java | Core authentication and token behavior
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/MfaService.java | MFA policy and challenge state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/RefreshTokenService.java | Refresh token lifecycle
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordPolicy.java | Password policy enforcement
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordService.java | Credential reset/encryption path
