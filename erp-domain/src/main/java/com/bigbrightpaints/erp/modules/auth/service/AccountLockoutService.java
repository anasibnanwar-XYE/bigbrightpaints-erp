package com.bigbrightpaints.erp.modules.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;

@Service
public class AccountLockoutService {

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

  private final UserAccountRepository userAccountRepository;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final TransactionTemplate transactionTemplate;
  private final TransactionTemplate requiresNewTransactionTemplate;
  private final Clock clock;

  @Autowired
  public AccountLockoutService(
      UserAccountRepository userAccountRepository,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      IamCanonicalStorageService iamCanonicalStorageService,
      PlatformTransactionManager transactionManager) {
    this(
        userAccountRepository,
        tokenBlacklistService,
        refreshTokenService,
        iamCanonicalStorageService,
        transactionManager,
        Clock.systemUTC());
  }

  AccountLockoutService(
      UserAccountRepository userAccountRepository,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      IamCanonicalStorageService iamCanonicalStorageService,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.userAccountRepository = userAccountRepository;
    this.tokenBlacklistService = tokenBlacklistService;
    this.refreshTokenService = refreshTokenService;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  public void enforceUnlocked(UserAccount user) {
    if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now())) {
      throw new LockedException("Account locked until " + user.getLockedUntil());
    }
  }

  public void resetFailures(UserAccount user) {
    if (user == null) {
      return;
    }
    transactionTemplate.executeWithoutResult(
        status -> {
          UserAccount lockedUser = lockOrSelf(user);
          lockedUser.setFailedLoginAttempts(0);
          lockedUser.setLockedUntil(null);
          userAccountRepository.save(lockedUser);
          iamCanonicalStorageService.syncUser(lockedUser);
          user.setFailedLoginAttempts(0);
          user.setLockedUntil(null);
        });
  }

  public void recordFailure(UserAccount user) {
    if (user == null) {
      return;
    }
    requiresNewTransactionTemplate.executeWithoutResult(
        status -> {
          UserAccount lockedUser = lockOrSelf(user);
          int attempts = lockedUser.getFailedLoginAttempts() + 1;
          lockedUser.setFailedLoginAttempts(attempts);
          boolean locked = attempts >= MAX_FAILED_ATTEMPTS;
          if (locked) {
            lockedUser.setLockedUntil(now().plus(LOCKOUT_DURATION));
          }
          userAccountRepository.save(lockedUser);
          iamCanonicalStorageService.syncUser(lockedUser);
          user.setFailedLoginAttempts(lockedUser.getFailedLoginAttempts());
          user.setLockedUntil(lockedUser.getLockedUntil());
          if (locked) {
            revokeActiveSessions(lockedUser);
          }
        });
  }

  public void recordFailureOnLockedAccount(UserAccount lockedUser) {
    if (lockedUser == null) {
      return;
    }
    int attempts = lockedUser.getFailedLoginAttempts() + 1;
    lockedUser.setFailedLoginAttempts(attempts);
    boolean locked = attempts >= MAX_FAILED_ATTEMPTS;
    if (locked) {
      lockedUser.setLockedUntil(now().plus(LOCKOUT_DURATION));
    }
    userAccountRepository.save(lockedUser);
    iamCanonicalStorageService.syncUser(lockedUser);
    if (locked) {
      revokeActiveSessions(lockedUser);
    }
  }

  private void revokeActiveSessions(UserAccount user) {
    if (user == null || user.getPublicId() == null) {
      return;
    }
    tokenBlacklistService.revokeAllUserTokens(user.getPublicId().toString());
    refreshTokenService.revokeAllForUser(user.getPublicId());
  }

  private UserAccount lockOrSelf(UserAccount user) {
    if (user.getId() == null) {
      return user;
    }
    return userAccountRepository.lockById(user.getId()).orElse(user);
  }

  private Instant now() {
    return clock.instant();
  }
}
