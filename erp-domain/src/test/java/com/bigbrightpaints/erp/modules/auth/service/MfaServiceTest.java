package com.bigbrightpaints.erp.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.security.CryptoService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.MfaRecoveryCode;
import com.bigbrightpaints.erp.modules.auth.domain.MfaRecoveryCodeRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.exception.InvalidMfaException;
import com.bigbrightpaints.erp.modules.auth.exception.MfaRequiredException;
import com.bigbrightpaints.erp.test.support.TotpTestUtils;

class MfaServiceTest {

  private UserAccountRepository repository;
  private MfaRecoveryCodeRepository mfaRecoveryCodeRepository;
  private PasswordEncoder passwordEncoder;
  private Clock clock;
  private CryptoService cryptoService;
  private TokenBlacklistService tokenBlacklistService;
  private RefreshTokenService refreshTokenService;
  private MfaService mfaService;

  @BeforeEach
  void setUp() {
    repository = mock(UserAccountRepository.class);
    mfaRecoveryCodeRepository = mock(MfaRecoveryCodeRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    cryptoService = mock(CryptoService.class);
    tokenBlacklistService = mock(TokenBlacklistService.class);
    refreshTokenService = mock(RefreshTokenService.class);
    clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    when(cryptoService.encrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    when(cryptoService.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    mfaService =
        new MfaService(
            repository,
            mfaRecoveryCodeRepository,
            passwordEncoder,
            cryptoService,
            tokenBlacklistService,
            refreshTokenService,
            "BigBright ERP",
            clock);
  }

  @Test
  void verifyDuringLoginSkipsWhenMfaDisabled() {
    UserAccount user = new UserAccount("user@bbp.dev", "hash", "User");
    user.setMfaEnabled(false);

    assertDoesNotThrow(() -> mfaService.verifyDuringLogin(user, null, null));
    verifyNoInteractions(repository);
  }

  @Test
  void verifyDuringLoginRequiresCodeWhenEnabled() {
    UserAccount user = userWithSecret();
    assertThrows(MfaRequiredException.class, () -> mfaService.verifyDuringLogin(user, null, null));
  }

  @Test
  void verifyDuringLoginAcceptsValidTotp() {
    UserAccount user = userWithSecret();
    String code = TotpTestUtils.generateCode(user.getMfaSecret(), clock.instant());

    assertDoesNotThrow(() -> mfaService.verifyDuringLogin(user, code, null));
    verifyNoInteractions(repository);
  }

  @Test
  void beginEnrollment_buildsScopeAwareQrLabel() {
    UserAccount user = new UserAccount("user@bbp.dev", "MOCK", "hash", "User");

    MfaService.MfaEnrollment enrollment = mfaService.beginEnrollment(user);

    assertThat(enrollment.qrUri()).contains("user");
    assertThat(enrollment.qrUri()).contains("MOCK");
  }

  @Test
  void disable_acceptsRecoveryCodeAndClearsEnrollment() {
    UserAccount user = userWithSecret();
    MfaRecoveryCode code = new MfaRecoveryCode(user, "hash-1");
    when(mfaRecoveryCodeRepository.findUnusedByUser(user)).thenReturn(java.util.List.of(code));
    when(passwordEncoder.matches("RECOVERY1", "hash-1")).thenReturn(true);

    mfaService.disable(user, null, "RECOVERY1");

    assertThat(user.isMfaEnabled()).isFalse();
    assertThat(user.getMfaSecret()).isNull();
    verify(mfaRecoveryCodeRepository).deleteAllByUser(user);
    verify(repository).save(user);
  }

  @Test
  void verifyDuringLogin_acceptsRecoveryCodeAndPersistsConsumption() {
    UserAccount user = userWithSecret();
    MfaRecoveryCode code = new MfaRecoveryCode(user, "hash-1");
    when(mfaRecoveryCodeRepository.findUnusedByUser(user)).thenReturn(java.util.List.of(code));
    when(passwordEncoder.matches("RECOVERY1", "hash-1")).thenReturn(true);

    assertDoesNotThrow(() -> mfaService.verifyDuringLogin(user, null, " RECOVERY1 "));

    verify(repository).save(user);
    verify(mfaRecoveryCodeRepository).save(code);
    assertThat(code.isUsed()).isTrue();
  }

  @Test
  void verifyDuringLogin_rejectsInvalidVerifier() {
    UserAccount user = userWithSecret();

    assertThrows(
        InvalidMfaException.class, () -> mfaService.verifyDuringLogin(user, "000000", null));
  }

  @Test
  void activate_requiresValidTotp() {
    UserAccount user = userWithSecret();

    assertThrows(ApplicationException.class, () -> mfaService.activate(user, "000000"));
  }

  private UserAccount userWithSecret() {
    UserAccount user = new UserAccount("user@bbp.dev", "hash", "User");
    user.setMfaSecret("JBSWY3DPEHPK3PXP");
    user.setMfaEnabled(true);
    return user;
  }
}
