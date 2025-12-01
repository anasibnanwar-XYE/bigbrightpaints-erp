package com.bigbrightpaints.erp.modules.auth.service;

import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.core.security.JwtProperties;
import com.bigbrightpaints.erp.core.security.JwtTokenService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.web.AuthResponse;
import com.bigbrightpaints.erp.modules.auth.web.ForgotPasswordRequest;
import com.bigbrightpaints.erp.modules.auth.web.LoginRequest;
import com.bigbrightpaints.erp.modules.auth.web.RefreshTokenRequest;
import com.bigbrightpaints.erp.modules.auth.web.ResetPasswordRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserAccountRepository userAccountRepository;
    private final CompanyRepository companyRepository;
    private final JwtProperties properties;
    private final MfaService mfaService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService,
                       RefreshTokenService refreshTokenService,
                       UserAccountRepository userAccountRepository,
                       CompanyRepository companyRepository,
                       JwtProperties properties,
                       MfaService mfaService,
                       EmailService emailService,
                       TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.userAccountRepository = userAccountRepository;
        this.companyRepository = companyRepository;
        this.properties = properties;
        this.mfaService = mfaService;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        enforceLock(user);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Company company = resolveCompanyForUser(principal.getUser(), request.companyCode());
            mfaService.verifyDuringLogin(principal.getUser(), request.mfaCode(), request.recoveryCode());
            resetLock(user);
            Map<String, Object> claims = new HashMap<>();
            claims.put("name", principal.getUser().getDisplayName());
            String accessToken = tokenService.generateAccessToken(principal.getUsername(), company.getCode(), claims);
            String refreshToken = refreshTokenService.issue(principal.getUsername(),
                    Instant.now().plusSeconds(properties.getRefreshTokenTtlSeconds()));
            return new AuthResponse("Bearer", accessToken, refreshToken, properties.getAccessTokenTtlSeconds(),
                    company.getCode(), principal.getUser().getDisplayName());
        } catch (AuthenticationException ex) {
            registerFailure(user);
            throw ex;
        }
    }

    // Forgot Password
    public void forgotPassword(ForgotPasswordRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetExpiry(Instant.now().plus(Duration.ofHours(1)));
        userAccountRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), resetToken);
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        UserAccount user = userAccountRepository.findByResetToken(request.token())
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (user.getResetExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords don't match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetExpiry(null);
        userAccountRepository.save(user);
        tokenBlacklistService.revokeAllUserTokens(user.getEmail());
        emailService.sendPasswordResetConfirmation(user.getEmail(), user.getDisplayName());
        return Map.of("success", true, "message", "Password reset. Login now.");
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String userEmail = refreshTokenService.consume(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Company company = resolveCompanyForUser(user, request.companyCode());
        Map<String, Object> claims = Map.of("name", userEmail);
        String accessToken = tokenService.generateAccessToken(userEmail, company.getCode(), claims);
        String refreshToken = refreshTokenService.issue(userEmail,
                Instant.now().plusSeconds(properties.getRefreshTokenTtlSeconds()));
        return new AuthResponse("Bearer", accessToken, refreshToken, properties.getAccessTokenTtlSeconds(),
                company.getCode(), userEmail);
    }

    private Company resolveCompanyForUser(UserAccount user, String companyCode) {
        Company company = companyRepository.findByCodeIgnoreCase(companyCode)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyCode));
        boolean member = user.getCompanies().stream()
                .anyMatch(c -> c.getCode().equalsIgnoreCase(companyCode));
        if (!member) {
            throw new IllegalArgumentException("User not assigned to company: " + companyCode);
        }
        return company;
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    private void enforceLock(UserAccount user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account locked until " + user.getLockedUntil());
        }
    }

    private void resetLock(UserAccount user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userAccountRepository.save(user);
    }

    private void registerFailure(UserAccount user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        }
        userAccountRepository.save(user);
    }
}
