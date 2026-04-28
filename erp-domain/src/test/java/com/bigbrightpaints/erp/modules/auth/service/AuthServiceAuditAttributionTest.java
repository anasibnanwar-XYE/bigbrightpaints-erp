package com.bigbrightpaints.erp.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.security.AuthScopeService;
import com.bigbrightpaints.erp.core.security.JwtProperties;
import com.bigbrightpaints.erp.core.security.JwtTokenService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.exception.InvalidMfaException;
import com.bigbrightpaints.erp.modules.auth.web.LoginRequest;
import com.bigbrightpaints.erp.modules.auth.web.RefreshTokenRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.TenantRuntimeRequestAdmissionService;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class AuthServiceAuditAttributionTest {

  @Mock private JwtTokenService tokenService;
  @Mock private RefreshTokenService refreshTokenService;

  @Mock
  private com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository userAccountRepository;

  @Mock private CompanyRepository companyRepository;
  @Mock private JwtProperties properties;
  @Mock private MfaService mfaService;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private AuditService auditService;
  @Mock private TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthScopeService authScopeService;
  @Mock private IamCanonicalStorageService iamCanonicalStorageService;
  @Mock private AuthSessionService authSessionService;
  private AccountLockoutService accountLockoutService;

  private final PlatformTransactionManager transactionManager =
      new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
          return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {}

        @Override
        public void rollback(TransactionStatus status) {}
      };

  private AuthService authService;

  @BeforeEach
  void setup() {
    accountLockoutService =
        new AccountLockoutService(
            userAccountRepository,
            tokenBlacklistService,
            refreshTokenService,
            iamCanonicalStorageService,
            transactionManager);
    authService =
        new AuthService(
            tokenService,
            refreshTokenService,
            userAccountRepository,
            companyRepository,
            properties,
            mfaService,
            tokenBlacklistService,
            auditService,
            tenantRuntimeRequestAdmissionService,
            passwordEncoder,
            authScopeService,
            iamCanonicalStorageService,
            accountLockoutService,
            authSessionService);
  }

  @Test
  void loginAuthenticationFailure_usesTrimmedAuditIdentifiers() {
    LoginRequest request =
        new LoginRequest(" user@example.com ", "wrong-password", "  COMP-01  ", null, null);
    UserAccount user = new UserAccount("user@example.com", "COMP-01", "hash", "User");
    user.setEnabled(true);

    when(authScopeService.requireScopeCode("  COMP-01  ")).thenReturn("COMP-01");
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "user@example.com", "COMP-01"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Invalid credentials");

    verify(auditService)
        .logAuthFailure(
            eq(AuditEvent.LOGIN_FAILURE),
            eq("user@example.com"),
            eq("COMP-01"),
            argThat(
                (Map<String, String> map) ->
                    map != null && "Invalid credentials".equals(map.get("reason"))));
  }

  @Test
  void loginMfaFailure_registersFailure_andFallsBackAuditReasonWhenBlankMessage() {
    LoginRequest request = new LoginRequest("user@example.com", "Passw0rd!", "ACME", null, null);
    UserAccount user = userWithCompany("user@example.com", "ACME");

    when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
    when(authScopeService.isPlatformScope("ACME")).thenReturn(false);
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "user@example.com", "ACME"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
    doThrow(new InvalidMfaException("   "))
        .when(mfaService)
        .verifyDuringLogin(eq(user), any(), any());

    assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidMfaException.class);

    assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    verify(userAccountRepository).save(user);
    verify(tenantRuntimeRequestAdmissionService)
        .enforceAuthOperationAllowed("ACME", "user@example.com", "LOGIN");
    verify(auditService)
        .logAuthFailure(
            eq(AuditEvent.LOGIN_FAILURE),
            eq("user@example.com"),
            eq("ACME"),
            argThat(
                (Map<String, String> map) ->
                    map != null && "Login failed".equals(map.get("reason"))));
  }

  @Test
  void loginRuntimeFailureFromTenantEnforcement_doesNotRegisterLockFailure_andAuditsReason() {
    LoginRequest request = new LoginRequest("user@example.com", "Passw0rd!", "ACME", null, null);
    UserAccount user = userWithCompany("user@example.com", "ACME");

    when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
    when(authScopeService.isPlatformScope("ACME")).thenReturn(false);
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "user@example.com", "ACME"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
    doThrow(
            com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                "Tenant runtime hold"))
        .when(tenantRuntimeRequestAdmissionService)
        .enforceAuthOperationAllowed("ACME", "user@example.com", "LOGIN");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Tenant runtime hold");

    assertThat(user.getFailedLoginAttempts()).isZero();
    verify(userAccountRepository, never()).save(any());
    verify(auditService)
        .logAuthFailure(
            eq(AuditEvent.LOGIN_FAILURE),
            eq("user@example.com"),
            eq("ACME"),
            argThat(
                (Map<String, String> map) ->
                    map != null && "Tenant runtime hold".equals(map.get("reason"))));
  }

  @Test
  void loginAllowsPlatformSuperAdminOnPlatformScope() {
    LoginRequest request =
        new LoginRequest("super-admin@example.com", "Passw0rd!", "PLATFORM", null, null);
    UserAccount user =
        new UserAccount("super-admin@example.com", "PLATFORM", "hash", "Super Admin");
    user.setEnabled(true);
    user.addRole(role("ROLE_SUPER_ADMIN"));

    when(authScopeService.requireScopeCode("PLATFORM")).thenReturn("PLATFORM");
    when(authScopeService.isPlatformScope("PLATFORM")).thenReturn(true);
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "super-admin@example.com", "PLATFORM"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
    when(tokenService.generateAccessToken(
            eq(user.getPublicId().toString()), eq("PLATFORM"), any(Map.class), any(Instant.class)))
        .thenReturn("access-new");
    when(refreshTokenService.issueSession(
            eq(user.getPublicId()),
            eq("PLATFORM"),
            any(Instant.class),
            any(Instant.class),
            any(),
            any()))
        .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-new", UUID.randomUUID()));
    when(properties.getRefreshTokenTtlSeconds()).thenReturn(3600L);
    when(properties.getAccessTokenTtlSeconds()).thenReturn(900L);

    var response = authService.login(request);

    assertThat(response.companyCode()).isEqualTo("PLATFORM");
    assertThat(response.accessToken()).isEqualTo("access-new");
    assertThat(response.refreshToken()).isEqualTo("refresh-new");
    verify(companyRepository, never()).findByCodeIgnoreCase(any());
    verify(tenantRuntimeRequestAdmissionService, never())
        .enforceAuthOperationAllowed(any(), any(), any());
  }

  @Test
  void loginRejectsTenantUserWithoutCompanyMembership() {
    LoginRequest request =
        new LoginRequest("tenant-user@example.com", "Passw0rd!", "TARGET", null, null);
    UserAccount user = new UserAccount("tenant-user@example.com", "TARGET", "hash", "Tenant User");
    user.setEnabled(true);
    user.addRole(role("ROLE_ADMIN"));

    when(authScopeService.requireScopeCode("TARGET")).thenReturn("TARGET");
    when(authScopeService.isPlatformScope("TARGET")).thenReturn(false);
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "tenant-user@example.com", "TARGET"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void refreshSuccess_enforcesRuntimePolicy_andReturnsTokens() {
    RefreshTokenRequest request = new RefreshTokenRequest("refresh-old", "acme");
    Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
    UserAccount user = userWithCompany("user@example.com", "ACME");
    RefreshTokenService.TokenRecord record =
        new RefreshTokenService.TokenRecord(
            user.getPublicId(), "ACME", issuedAt, issuedAt.plus(1, ChronoUnit.DAYS), "digest-old");

    when(authScopeService.requireScopeCode("acme")).thenReturn("ACME");
    when(authScopeService.isPlatformScope("ACME")).thenReturn(false);
    when(refreshTokenService.inspect("refresh-old", "ACME")).thenReturn(Optional.of(record));
    when(refreshTokenService.consume("refresh-old", "ACME")).thenReturn(Optional.of(record));
    when(tokenBlacklistService.isUserTokenRevoked(user.getPublicId().toString(), issuedAt))
        .thenReturn(false);
    when(userAccountRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));
    when(tokenService.generateAccessToken(
            eq(user.getPublicId().toString()),
            eq("ACME"),
            argThat(
                (Map<String, Object> claims) ->
                    claims != null && user.getDisplayName().equals(claims.get("name"))),
            any(Instant.class)))
        .thenReturn("access-new");
    when(refreshTokenService.issueSession(
            eq(user.getPublicId()),
            eq("ACME"),
            any(Instant.class),
            any(Instant.class),
            any(),
            eq("digest-old")))
        .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-new", UUID.randomUUID()));
    when(properties.getRefreshTokenTtlSeconds()).thenReturn(3600L);
    when(properties.getAccessTokenTtlSeconds()).thenReturn(900L);

    var response = authService.refresh(request);

    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.accessToken()).isEqualTo("access-new");
    assertThat(response.refreshToken()).isEqualTo("refresh-new");
    assertThat(response.companyCode()).isEqualTo("ACME");
    assertThat(response.displayName()).isEqualTo(user.getDisplayName());
    verify(tenantRuntimeRequestAdmissionService)
        .enforceAuthOperationAllowed("ACME", "user@example.com", "REFRESH_TOKEN");
  }

  @Test
  void refreshRejectsWhenRefreshTokenAlreadyRevoked() {
    Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
    UUID userPublicId = UUID.randomUUID();
    RefreshTokenService.TokenRecord record =
        new RefreshTokenService.TokenRecord(
            userPublicId, "ACME", issuedAt, issuedAt.plus(1, ChronoUnit.DAYS), "digest-old");

    when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
    when(refreshTokenService.inspect("refresh-old", "ACME")).thenReturn(Optional.of(record));
    when(tokenBlacklistService.isUserTokenRevoked(userPublicId.toString(), issuedAt))
        .thenReturn(true);

    assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("refresh-old", "ACME")))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Refresh token revoked");
  }

  @Test
  void refreshRejectsWhenRequestedScopeDoesNotMatchStoredScope() {
    when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
    when(refreshTokenService.inspect("refresh-old", "ACME")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("refresh-old", "ACME")))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Invalid refresh token");
    verify(userAccountRepository, never()).findByPublicId(any());
  }

  @Test
  void logoutDerivesIdentityFromTokenSubject_whenRefreshingTokenIsAbsent() {
    Claims claims = org.mockito.Mockito.mock(Claims.class);
    Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");
    UUID userPublicId = UUID.randomUUID();
    when(tokenService.parse("access-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn(userPublicId.toString());
    when(claims.getId()).thenReturn("jti-logout");
    when(claims.getExpiration()).thenReturn(Date.from(expiresAt));

    authService.logout("   ", "access-token");

    verify(tokenBlacklistService, never()).revokeAllUserTokens(userPublicId.toString());
    verify(refreshTokenService, never()).revokeAllForUser(userPublicId);
    verify(tokenBlacklistService)
        .blacklistToken("jti-logout", expiresAt, userPublicId.toString(), "logout");
  }

  @Test
  void logoutLogsWarningWhenTokenBlacklistingFails() {
    Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logger.addAppender(logAppender);
    logAppender.start();

    Claims claims = org.mockito.Mockito.mock(Claims.class);
    Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");
    UUID userPublicId = UUID.randomUUID();
    when(tokenService.parse("access-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn(userPublicId.toString());
    when(claims.getId()).thenReturn("jti-logout");
    when(claims.getExpiration()).thenReturn(Date.from(expiresAt));
    doThrow(new RuntimeException("store unavailable"))
        .when(tokenBlacklistService)
        .blacklistToken("jti-logout", expiresAt, userPublicId.toString(), "logout");

    try {
      authService.logout("refresh-token", "access-token");
    } finally {
      logger.detachAppender(logAppender);
      logAppender.stop();
    }

    verify(tokenBlacklistService, never()).revokeAllUserTokens(userPublicId.toString());
    verify(refreshTokenService, never()).revokeAllForUser(userPublicId);
    assertThat(logAppender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.WARN);
              assertThat(event.getFormattedMessage())
                  .contains("Failed to blacklist access token during logout");
            });
  }

  @Test
  void logoutUsesNullIdentityWhenAccessTokenSubjectIsNotUuid() {
    Claims claims = org.mockito.Mockito.mock(Claims.class);
    Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");
    when(tokenService.parse("access-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("not-a-uuid");
    when(claims.getId()).thenReturn("jti-logout");
    when(claims.getExpiration()).thenReturn(Date.from(expiresAt));

    authService.logout("   ", "access-token");

    verify(tokenBlacklistService, never()).revokeAllUserTokens(anyString());
    verify(refreshTokenService, never()).revokeAllForUser(any());
    verify(tokenBlacklistService).blacklistToken("jti-logout", expiresAt, null, "logout");
  }

  @Test
  void loginLockoutThreshold_revokesExistingSessions() {
    LoginRequest request =
        new LoginRequest("user@example.com", "wrong-password", "ACME", null, null);
    UserAccount user = userWithCompany("user@example.com", "ACME");
    user.setFailedLoginAttempts(4);

    when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "user@example.com", "ACME"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Invalid credentials");

    assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    assertThat(user.getLockedUntil()).isNotNull();
    verify(userAccountRepository).save(user);
    verify(tokenBlacklistService).revokeAllUserTokens(user.getPublicId().toString());
    verify(refreshTokenService).revokeAllForUser(user.getPublicId());
  }

  @Test
  void loginNormalizesEmailWithLocaleRoot() {
    Locale originalLocale = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
      LoginRequest request =
          new LoginRequest(" IUSER@example.com ", "Passw0rd!", "ACME", null, null);
      UserAccount user = userWithCompany("iuser@example.com", "ACME");

      when(authScopeService.requireScopeCode("ACME")).thenReturn("ACME");
      when(authScopeService.isPlatformScope("ACME")).thenReturn(false);
      when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
              "iuser@example.com", "ACME"))
          .thenReturn(Optional.of(user));
      when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);
      when(tokenService.generateAccessToken(
              eq(user.getPublicId().toString()), eq("ACME"), any(Map.class), any(Instant.class)))
          .thenReturn("access-new");
      when(refreshTokenService.issueSession(
              eq(user.getPublicId()),
              eq("ACME"),
              any(Instant.class),
              any(Instant.class),
              any(),
              any()))
          .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-new", UUID.randomUUID()));
      when(properties.getRefreshTokenTtlSeconds()).thenReturn(3600L);
      when(properties.getAccessTokenTtlSeconds()).thenReturn(900L);

      var response = authService.login(request);

      assertThat(response.companyCode()).isEqualTo("ACME");
      verify(userAccountRepository)
          .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase("iuser@example.com", "ACME");
    } finally {
      Locale.setDefault(originalLocale);
    }
  }

  @Test
  void loginRejectsPlatformScopeForNonSuperAdmin() {
    LoginRequest request =
        new LoginRequest("tenant-user@example.com", "Passw0rd!", "PLATFORM", null, null);
    UserAccount user =
        new UserAccount("tenant-user@example.com", "PLATFORM", "hash", "Tenant User");
    user.setEnabled(true);
    user.addRole(role("ROLE_ADMIN"));

    when(authScopeService.requireScopeCode("PLATFORM")).thenReturn("PLATFORM");
    when(authScopeService.isPlatformScope("PLATFORM")).thenReturn(true);
    when(userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            "tenant-user@example.com", "PLATFORM"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Passw0rd!", "hash")).thenReturn(true);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void normalizeEmail_returnsNullForNullInput() {
    assertThat(
            (String)
                com.bigbrightpaints.erp.test.support.ReflectionFieldAccess.invokeMethod(
                    authService, "normalizeEmail", (Object) null))
        .isNull();
  }

  private UserAccount userWithCompany(String email, String companyCode) {
    UserAccount user = new UserAccount(email, companyCode, "hash", "User");
    user.setEnabled(true);
    user.setCompany(company(companyCode));
    return user;
  }

  private Company company(String code) {
    Company company = new Company();
    company.setCode(code);
    company.setName("Company " + code);
    company.setTimezone("UTC");
    return company;
  }

  private Role role(String name) {
    Role role = new Role();
    role.setName(name);
    return role;
  }
}
