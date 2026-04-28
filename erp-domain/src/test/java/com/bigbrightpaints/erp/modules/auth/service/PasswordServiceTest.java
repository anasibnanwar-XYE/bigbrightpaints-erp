package com.bigbrightpaints.erp.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.PasswordResetTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPasswordHistory;
import com.bigbrightpaints.erp.modules.auth.domain.UserPasswordHistoryRepository;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

  @Mock private UserAccountRepository userAccountRepository;

  @Mock private UserPasswordHistoryRepository passwordHistoryRepository;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private IamCanonicalStorageService iamCanonicalStorageService;

  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

  private PasswordEncoder passwordEncoder;
  private PasswordPolicy passwordPolicy;
  private PasswordService passwordService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    passwordPolicy = new PasswordPolicy();
    passwordService =
        new PasswordService(
            userAccountRepository,
            passwordHistoryRepository,
            passwordEncoder,
            passwordPolicy,
            tokenBlacklistService,
            refreshTokenService,
            iamCanonicalStorageService,
            passwordResetTokenRepository);
  }

  @Test
  void changePasswordRejectsHistoryReuse() {
    UserAccount user = userWithPassword("CurrentPass1!");
    UserPasswordHistory history =
        new UserPasswordHistory(user, passwordEncoder.encode("PriorPass1!"));
    when(passwordHistoryRepository.findTop5ByUserOrderByChangedAtDesc(user))
        .thenReturn(List.of(history));

    ChangePasswordRequest request =
        new ChangePasswordRequest("CurrentPass1!", "PriorPass1!", "PriorPass1!");

    assertThrows(ApplicationException.class, () -> passwordService.changePassword(user, request));
    verify(passwordHistoryRepository, never()).save(any());
    verifyNoInteractions(userAccountRepository);
    verifyNoInteractions(tokenBlacklistService, refreshTokenService);
  }

  @Test
  void changePasswordRejectsWeakPasswords() {
    UserAccount user = userWithPassword("CurrentPass1!");
    ChangePasswordRequest request = new ChangePasswordRequest("CurrentPass1!", "weak", "weak");

    assertThrows(ApplicationException.class, () -> passwordService.changePassword(user, request));
    verify(passwordHistoryRepository, never()).save(any());
    verifyNoInteractions(userAccountRepository);
    verifyNoInteractions(tokenBlacklistService, refreshTokenService);
  }

  @Test
  void changePasswordPersistsHistoryAndNewHash() {
    UserAccount user = userWithPassword("CurrentPass1!");
    when(passwordHistoryRepository.findTop5ByUserOrderByChangedAtDesc(user))
        .thenReturn(Collections.emptyList());
    when(passwordHistoryRepository.findByUserOrderByChangedAtDesc(user))
        .thenReturn(new ArrayList<>());

    ChangePasswordRequest request =
        new ChangePasswordRequest("CurrentPass1!", "NewPassword1!", "NewPassword1!");

    passwordService.changePassword(user, request);

    verify(passwordHistoryRepository)
        .save(
            argThat(
                entry ->
                    entry.getUser() == user
                        && passwordEncoder.matches("CurrentPass1!", entry.getPasswordHash())));
    verify(userAccountRepository).save(user);
    verify(iamCanonicalStorageService).syncUser(user);
    verify(passwordResetTokenRepository).deleteByUser(user);
    verify(tokenBlacklistService).revokeAllUserTokens(user.getPublicId().toString());
    verify(refreshTokenService).revokeAllForUser(user.getPublicId());
    assertTrue(passwordEncoder.matches("NewPassword1!", user.getPasswordHash()));
  }

  @Test
  void resetPasswordRejectsCurrentPasswordReuse() {
    UserAccount user = userWithPassword("CurrentPass1!");

    assertThrows(
        ApplicationException.class,
        () -> passwordService.resetPassword(user, "CurrentPass1!", "CurrentPass1!"));

    verify(passwordHistoryRepository, never()).save(any());
    verifyNoInteractions(userAccountRepository);
    verifyNoInteractions(tokenBlacklistService, refreshTokenService);
  }

  @Test
  void changePasswordNormalizesNewPasswordBeforeHashingAndHistoryChecks() {
    UserAccount user = userWithPassword("CurrentPass1!");
    when(passwordHistoryRepository.findTop5ByUserOrderByChangedAtDesc(user))
        .thenReturn(Collections.emptyList());
    when(passwordHistoryRepository.findByUserOrderByChangedAtDesc(user))
        .thenReturn(new ArrayList<>());

    ChangePasswordRequest request =
        new ChangePasswordRequest("CurrentPass1!", "Cafe\u0301Pass1!", "Caf\u00e9Pass1!");

    passwordService.changePassword(user, request);

    assertTrue(passwordEncoder.matches("Caf\u00e9Pass1!", user.getPasswordHash()));
    verify(userAccountRepository).save(user);
  }

  private UserAccount userWithPassword(String rawPassword) {
    return new UserAccount("user@bbp.dev", "BBP", passwordEncoder.encode(rawPassword), "User");
  }
}
