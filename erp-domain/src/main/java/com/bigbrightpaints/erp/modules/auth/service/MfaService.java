package com.bigbrightpaints.erp.modules.auth.service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.CryptoService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.MfaRecoveryCode;
import com.bigbrightpaints.erp.modules.auth.domain.MfaRecoveryCodeRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.exception.InvalidMfaException;
import com.bigbrightpaints.erp.modules.auth.exception.MfaRequiredException;

import jakarta.transaction.Transactional;

@Service
public class MfaService {

  private static final int RECOVERY_CODE_COUNT = 8;
  private static final int RECOVERY_CODE_LENGTH = 10;
  private static final char[] RECOVERY_CODE_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private static final int TOTP_DIGITS = 6;
  private static final int TOTP_MODULUS = (int) Math.pow(10, TOTP_DIGITS);
  private static final int TIME_STEP_SECONDS = 30;
  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private static final int[] BASE32_LOOKUP = buildLookup();

  private final UserAccountRepository userAccountRepository;
  private final MfaRecoveryCodeRepository mfaRecoveryCodeRepository;
  private final PasswordEncoder passwordEncoder;
  private final CryptoService cryptoService;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final AccountLockoutService accountLockoutService;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock;
  private final String issuer;

  @Autowired
  public MfaService(
      UserAccountRepository userAccountRepository,
      MfaRecoveryCodeRepository mfaRecoveryCodeRepository,
      PasswordEncoder passwordEncoder,
      CryptoService cryptoService,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      IamCanonicalStorageService iamCanonicalStorageService,
      AccountLockoutService accountLockoutService,
      @Value("${security.mfa.issuer:BigBright ERP}") String issuer) {
    this(
        userAccountRepository,
        mfaRecoveryCodeRepository,
        passwordEncoder,
        cryptoService,
        tokenBlacklistService,
        refreshTokenService,
        iamCanonicalStorageService,
        accountLockoutService,
        issuer,
        Clock.systemUTC());
  }

  MfaService(
      UserAccountRepository userAccountRepository,
      MfaRecoveryCodeRepository mfaRecoveryCodeRepository,
      PasswordEncoder passwordEncoder,
      CryptoService cryptoService,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      IamCanonicalStorageService iamCanonicalStorageService,
      AccountLockoutService accountLockoutService,
      String issuer,
      Clock clock) {
    this.userAccountRepository = userAccountRepository;
    this.mfaRecoveryCodeRepository = mfaRecoveryCodeRepository;
    this.passwordEncoder = passwordEncoder;
    this.cryptoService = cryptoService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.refreshTokenService = refreshTokenService;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.accountLockoutService = accountLockoutService;
    this.issuer = issuer;
    this.clock = clock;
  }

  @Transactional
  public MfaEnrollment beginEnrollment(UserAccount user) {
    requireUser(user);
    accountLockoutService.enforceUnlocked(user);
    if (user.isMfaEnabled()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "MFA is already enabled; use a verified MFA profile-change flow");
    }
    String secret = generateSecret();
    List<String> recoveryCodes = generateRecoveryCodes();
    user.setMfaSecret(cryptoService.encrypt(secret));
    user.setMfaEnabled(false);
    userAccountRepository.save(user);
    replaceRecoveryCodes(user, recoveryCodes);
    iamCanonicalStorageService.syncUser(user);
    return new MfaEnrollment(secret, buildOtpAuthUri(user, secret), recoveryCodes);
  }

  @Transactional
  public void activate(UserAccount user, String code) {
    requireUser(user);
    accountLockoutService.enforceUnlocked(user);
    String decryptedSecret = requireActiveSecret(user);
    if (!isValidTotp(decryptedSecret, code)) {
      accountLockoutService.recordFailure(user);
      throw new ApplicationException(ErrorCode.AUTH_MFA_INVALID, "Invalid MFA code");
    }
    accountLockoutService.resetFailures(user);
    user.setMfaEnabled(true);
    userAccountRepository.save(user);
    iamCanonicalStorageService.syncUser(user);
    revokeActiveSessions(user);
  }

  @Transactional
  public void disable(UserAccount user, String totpCode, String recoveryCode) {
    requireUser(user);
    accountLockoutService.enforceUnlocked(user);
    if (!user.isMfaEnabled()) {
      return;
    }
    boolean cleared = false;
    String decryptedSecret = requireActiveSecret(user);
    if (isValidTotp(decryptedSecret, totpCode)) {
      cleared = true;
    } else if (consumeRecoveryCode(user, normalizeCode(recoveryCode))) {
      cleared = true;
    }
    if (!cleared) {
      accountLockoutService.recordFailure(user);
      throw new ApplicationException(ErrorCode.AUTH_MFA_INVALID, "Invalid MFA verification data");
    }
    accountLockoutService.resetFailures(user);
    clearMfa(user);
    userAccountRepository.save(user);
    iamCanonicalStorageService.syncUser(user);
    revokeActiveSessions(user);
  }

  @Transactional(dontRollbackOn = ApplicationException.class)
  public List<String> regenerateRecoveryCodes(
      UserAccount user, String totpCode, String recoveryCode) {
    return regenerateRecoveryCodes(user, totpCode, recoveryCode, null);
  }

  @Transactional(dontRollbackOn = ApplicationException.class)
  public List<String> regenerateRecoveryCodes(
      UserAccount user, String totpCode, String recoveryCode, Instant authenticatedTokenIssuedAt) {
    UserAccount lockedUser = lockUserForRecoveryRegeneration(user);
    accountLockoutService.enforceUnlocked(lockedUser);
    enforceTokenStillActive(lockedUser, authenticatedTokenIssuedAt);
    if (!lockedUser.isMfaEnabled()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "MFA must be enabled to regenerate recovery codes");
    }
    String decryptedSecret = requireActiveSecret(lockedUser);
    boolean verified = false;
    String normalizedTotp = normalizeCode(totpCode);
    String normalizedRecovery = normalizeCode(recoveryCode);
    if (StringUtils.hasText(normalizedTotp) && isValidTotp(decryptedSecret, normalizedTotp)) {
      verified = true;
    } else if (StringUtils.hasText(normalizedRecovery)
        && consumeRecoveryCode(lockedUser, normalizedRecovery)) {
      verified = true;
    }
    if (!verified) {
      accountLockoutService.recordFailureOnLockedAccount(lockedUser);
      throw new ApplicationException(ErrorCode.AUTH_MFA_INVALID, "Invalid MFA verification data");
    }
    accountLockoutService.resetFailures(lockedUser);
    List<String> recoveryCodes = generateRecoveryCodes();
    replaceRecoveryCodes(lockedUser, recoveryCodes);
    userAccountRepository.save(lockedUser);
    iamCanonicalStorageService.syncUser(lockedUser);
    revokeActiveSessions(lockedUser);
    return recoveryCodes;
  }

  @Transactional
  public void verifyDuringLogin(UserAccount user, String totpCode, String recoveryCode) {
    requireUser(user);
    if (!user.isMfaEnabled()) {
      return;
    }
    String normalizedTotp = normalizeCode(totpCode);
    String normalizedRecovery = normalizeCode(recoveryCode);
    String decryptedSecret = requireActiveSecret(user);
    if (StringUtils.hasText(normalizedTotp) && isValidTotp(decryptedSecret, normalizedTotp)) {
      return;
    }
    if (StringUtils.hasText(normalizedRecovery) && consumeRecoveryCode(user, normalizedRecovery)) {
      userAccountRepository.save(user);
      return;
    }
    if (!StringUtils.hasText(normalizedTotp) && !StringUtils.hasText(normalizedRecovery)) {
      throw new MfaRequiredException("Multi-factor authentication required");
    }
    throw new InvalidMfaException("Invalid MFA verifier");
  }

  private String requireActiveSecret(UserAccount user) {
    if (!StringUtils.hasText(user.getMfaSecret())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "MFA enrollment is inactive for this user");
    }
    return cryptoService.decrypt(user.getMfaSecret());
  }

  private void requireUser(UserAccount user) {
    if (user == null) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "User is required");
    }
  }

  private String normalizeCode(String code) {
    if (!StringUtils.hasText(code)) {
      return null;
    }
    return code.replaceAll("\\s+", "").trim();
  }

  private String generateSecret() {
    byte[] buffer = new byte[20];
    secureRandom.nextBytes(buffer);
    return encodeBase32(buffer);
  }

  private List<String> generateRecoveryCodes() {
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
      codes.add(randomRecoveryCode());
    }
    return codes;
  }

  private String randomRecoveryCode() {
    StringBuilder builder = new StringBuilder(RECOVERY_CODE_LENGTH);
    for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
      int index = secureRandom.nextInt(RECOVERY_CODE_ALPHABET.length);
      builder.append(RECOVERY_CODE_ALPHABET[index]);
    }
    return builder.toString();
  }

  private boolean consumeRecoveryCode(UserAccount user, String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return false;
    }
    List<MfaRecoveryCode> codes = mfaRecoveryCodeRepository.findUnusedByUserForUpdate(user);
    for (MfaRecoveryCode code : codes) {
      if (passwordEncoder.matches(candidate, code.getCodeHash())) {
        code.markAsUsed();
        mfaRecoveryCodeRepository.save(code);
        return true;
      }
    }
    return false;
  }

  private UserAccount lockUserForRecoveryRegeneration(UserAccount user) {
    if (user == null || user.getId() == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "MFA must be enabled to regenerate recovery codes");
    }
    return userAccountRepository
        .lockById(user.getId())
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.VALIDATION_INVALID_INPUT,
                    "MFA must be enabled to regenerate recovery codes"));
  }

  private void enforceTokenStillActive(UserAccount user, Instant authenticatedTokenIssuedAt) {
    if (user == null || user.getPublicId() == null || authenticatedTokenIssuedAt == null) {
      return;
    }
    if (tokenBlacklistService.isUserTokenRevoked(
        user.getPublicId().toString(), authenticatedTokenIssuedAt)) {
      throw new ApplicationException(
          ErrorCode.AUTH_SESSION_EXPIRED, ErrorCode.AUTH_SESSION_EXPIRED.getDefaultMessage());
    }
  }

  private void replaceRecoveryCodes(UserAccount user, List<String> recoveryCodes) {
    mfaRecoveryCodeRepository.findUnusedByUserForUpdate(user);
    mfaRecoveryCodeRepository.deleteAllByUser(user);
    mfaRecoveryCodeRepository.flush();
    if (recoveryCodes == null || recoveryCodes.isEmpty()) {
      return;
    }
    List<MfaRecoveryCode> hashedCodes =
        recoveryCodes.stream()
            .map(code -> new MfaRecoveryCode(user, passwordEncoder.encode(code)))
            .toList();
    mfaRecoveryCodeRepository.saveAll(hashedCodes);
    mfaRecoveryCodeRepository.flush();
  }

  private void revokeActiveSessions(UserAccount user) {
    if (user == null || user.getPublicId() == null) {
      return;
    }
    tokenBlacklistService.revokeAllUserTokens(user.getPublicId().toString());
    refreshTokenService.revokeAllForUser(user.getPublicId());
  }

  private boolean isValidTotp(String secret, String code) {
    if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
      return false;
    }
    if (!code.matches("\\d{" + TOTP_DIGITS + "}")) {
      return false;
    }
    byte[] decodedSecret = decodeBase32(secret);
    if (decodedSecret.length == 0) {
      return false;
    }
    long currentBucket = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
    for (int offset = -1; offset <= 1; offset++) {
      long counter = currentBucket + offset;
      try {
        int expected = generateTotp(decodedSecret, counter);
        if (formatCode(expected).equals(code)) {
          return true;
        }
      } catch (GeneralSecurityException e) {
        return false;
      }
    }
    return false;
  }

  private String formatCode(int value) {
    return String.format("%0" + TOTP_DIGITS + "d", value);
  }

  private int generateTotp(byte[] secretKey, long counter) throws GeneralSecurityException {
    byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
    Mac mac = Mac.getInstance(HMAC_ALGORITHM);
    mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
    byte[] hash = mac.doFinal(counterBytes);
    int offset = hash[hash.length - 1] & 0x0F;
    int binary =
        ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);
    return binary % TOTP_MODULUS;
  }

  private String buildOtpAuthUri(UserAccount user, String secret) {
    String label =
        UriUtils.encodePathSegment(issuer + ":" + scopedAccountLabel(user), StandardCharsets.UTF_8);
    String encodedIssuer = UriUtils.encode(issuer, StandardCharsets.UTF_8);
    return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encodedIssuer;
  }

  private String scopedAccountLabel(UserAccount user) {
    String email = user.getEmail();
    String authScopeCode = normalizeCode(user.getAuthScopeCode());
    if (!StringUtils.hasText(authScopeCode)) {
      return email;
    }
    return email + " (" + authScopeCode + ")";
  }

  private void clearMfa(UserAccount user) {
    user.setMfaEnabled(false);
    user.setMfaSecret(null);
    mfaRecoveryCodeRepository.deleteAllByUser(user);
  }

  public record MfaEnrollment(String secret, String qrUri, List<String> recoveryCodes) {}

  private static int[] buildLookup() {
    int[] lookup = new int[128];
    Arrays.fill(lookup, -1);
    for (int i = 0; i < BASE32_ALPHABET.length; i++) {
      lookup[BASE32_ALPHABET[i]] = i;
    }
    return lookup;
  }

  private String encodeBase32(byte[] data) {
    StringBuilder output = new StringBuilder((data.length * 8 + 4) / 5);
    int buffer = 0;
    int bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        int index = (buffer >> (bitsLeft - 5)) & 0x1F;
        output.append(BASE32_ALPHABET[index]);
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      int index = (buffer << (5 - bitsLeft)) & 0x1F;
      output.append(BASE32_ALPHABET[index]);
    }
    return output.toString().toUpperCase(Locale.ROOT);
  }

  private byte[] decodeBase32(String value) {
    if (!StringUtils.hasText(value)) {
      return new byte[0];
    }
    String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int buffer = 0;
    int bitsLeft = 0;
    for (char c : normalized.toCharArray()) {
      if (c >= BASE32_LOOKUP.length) {
        continue;
      }
      int val = BASE32_LOOKUP[c];
      if (val < 0) {
        continue;
      }
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        out.write((buffer >> (bitsLeft - 8)) & 0xFF);
        bitsLeft -= 8;
      }
    }
    return out.toByteArray();
  }
}
