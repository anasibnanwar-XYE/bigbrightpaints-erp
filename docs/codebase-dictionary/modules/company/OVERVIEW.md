# Company DTOs

## Location
`modules/company/dto/`

## Core DTOs

### CompanyDto
```java
record CompanyDto(
    Long id,
    UUID publicId,
    String name,
    String code,
    String timezone,
    String stateCode,
    BigDecimal defaultGstRate
)
```

### CompanyRequest
```java
record CompanyRequest(
    @NotBlank @Size(max=255) String name,
    @NotBlank @Size(max=64) String code,
    @NotBlank @Size(max=64) String timezone,
    @Size(min=2, max=2) String stateCode,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal defaultGstRate,
    @Min(0) Long quotaMaxActiveUsers,
    @Min(0) Long quotaMaxApiRequests,
    @Min(0) Long quotaMaxStorageBytes,
    @Min(0) Long quotaMaxConcurrentRequests,
    Boolean quotaSoftLimitEnabled,
    Boolean quotaHardLimitEnabled,
    @Email @Size(max=255) String firstAdminEmail,
    @Size(max=255) String firstAdminDisplayName,
    Set<@NotBlank @Size(max=64) String> enabledModules
)
```

---

## Lifecycle DTOs

### CompanyLifecycleStateDto
```java
record CompanyLifecycleStateDto(
    Long companyId,
    String companyCode,
    String previousLifecycleState,
    String lifecycleState,
    String reason
)
```

### CompanyLifecycleStateRequest
```java
record CompanyLifecycleStateRequest(
    @NotBlank String state,
    @NotBlank String reason
)
```

---

## Module DTOs

### CompanyEnabledModulesDto
```java
record CompanyEnabledModulesDto(
    Long companyId,
    String companyCode,
    Set<String> enabledModules  // Unmodifiable
)
```

---

## Metrics DTOs

### CompanyTenantMetricsDto
```java
record CompanyTenantMetricsDto(
    Long companyId,
    String companyCode,
    String lifecycleState,
    String lifecycleReason,
    Long quotaMaxActiveUsers,
    Long quotaMaxApiRequests,
    Long quotaMaxStorageBytes,
    Long quotaMaxConcurrentRequests,
    boolean quotaSoftLimitEnabled,
    boolean quotaHardLimitEnabled,
    long activeUserCount,
    long apiActivityCount,
    long apiErrorCount,
    long apiErrorRateInBasisPoints,
    long currentConcurrentRequests,
    long auditStorageBytes
)
```

---

## Super-Admin Dashboard DTOs

### CompanySuperAdminDashboardDto
```java
record CompanySuperAdminDashboardDto(
    long totalTenants,
    long activeTenants,
    long suspendedTenants,
    long deactivatedTenants,
    long totalActiveUsers,
    long totalActiveUserQuota,
    long totalAuditStorageBytes,
    long totalStorageQuotaBytes,
    long totalCurrentConcurrentRequests,
    long totalConcurrentRequestQuota,
    List<TenantOverview> tenants
) {
    record TenantOverview(
        Long companyId,
        String companyCode,
        String companyName,
        String timezone,
        String lifecycleState,
        String lifecycleReason,
        long activeUsers,
        long quotaMaxActiveUsers,
        long auditStorageBytes,
        long quotaMaxStorageBytes,
        long currentConcurrentRequests,
        long quotaMaxConcurrentRequests,
        long apiActivityCount,
        long quotaMaxApiRequests,
        long apiErrorCount,
        long apiErrorRateInBasisPoints,
        boolean quotaSoftLimitEnabled,
        boolean quotaHardLimitEnabled,
        long userUtilizationBasisPoints,
        long storageUtilizationBasisPoints,
        long concurrencyUtilizationBasisPoints
    )
}
```

---

## Super-Admin Tenant DTOs

### SuperAdminTenantSummaryDto
```java
record SuperAdminTenantSummaryDto(
    Long companyId,
    String companyCode,
    String companyName,
    String timezone,
    String lifecycleState,
    String lifecycleReason,
    long activeUserCount,
    long quotaMaxActiveUsers,
    long apiActivityCount,
    long quotaMaxApiRequests,
    long auditStorageBytes,
    long quotaMaxStorageBytes,
    long currentConcurrentRequests,
    long quotaMaxConcurrentRequests,
    Set<String> enabledModules,
    MainAdminSummaryDto mainAdmin,
    Instant lastActivityAt
)
```

### SuperAdminTenantDetailDto
```java
record SuperAdminTenantDetailDto(
    Long companyId,
    String companyCode,
    String companyName,
    String timezone,
    String stateCode,
    String lifecycleState,
    String lifecycleReason,
    Set<String> enabledModules,
    Onboarding onboarding,
    MainAdminSummaryDto mainAdmin,
    Limits limits,
    Usage usage,
    SupportContext supportContext,
    List<SupportTimelineEvent> supportTimeline,
    AvailableActions availableActions
) {
    record Onboarding(
        String templateCode,
        String adminEmail,
        Long adminUserId,
        boolean tenantAdminProvisioned,
        boolean credentialsEmailSent,
        Instant credentialsEmailedAt,
        Instant completedAt
    )
    
    record Limits(
        long quotaMaxActiveUsers,
        long quotaMaxApiRequests,
        long quotaMaxStorageBytes,
        long quotaMaxConcurrentRequests,
        boolean quotaSoftLimitEnabled,
        boolean quotaHardLimitEnabled
    )
    
    record Usage(
        long activeUserCount,
        long apiActivityCount,
        long apiErrorCount,
        long apiErrorRateInBasisPoints,
        long auditStorageBytes,
        long currentConcurrentRequests,
        Instant lastActivityAt
    )
    
    record SupportContext(String supportNotes, Set<String> supportTags)
    
    record SupportTimelineEvent(String category, String title, String message, String actor, Instant occurredAt)
    
    record AvailableActions(
        boolean canUpdateLifecycle,
        boolean canUpdateLimits,
        boolean canUpdateModules,
        boolean canIssueWarnings,
        boolean canResetAdminPassword,
        boolean canForceLogout,
        boolean canReplaceMainAdmin,
        boolean canRequestAdminEmailChange
    )
}
```

### SuperAdminTenantLimitsDto
```java
record SuperAdminTenantLimitsDto(
    Long companyId,
    String companyCode,
    Long quotaMaxActiveUsers,
    Long quotaMaxApiRequests,
    Long quotaMaxStorageBytes,
    Long quotaMaxConcurrentRequests,
    boolean quotaSoftLimitEnabled,
    boolean quotaHardLimitEnabled
)
```

### SuperAdminTenantSupportContextDto
```java
record SuperAdminTenantSupportContextDto(
    Long companyId,
    String companyCode,
    String supportNotes,
    Set<String> supportTags
)
```

### SuperAdminTenantForceLogoutDto
```java
record SuperAdminTenantForceLogoutDto(
    Long companyId,
    String companyCode,
    int revokedUserCount,
    String reason,
    String actor,
    Instant occurredAt
)
```

---

## Admin Management DTOs

### MainAdminSummaryDto
```java
record MainAdminSummaryDto(
    Long userId,
    String email,
    String displayName,
    boolean enabled,
    boolean exists
)
```

### CompanyAdminCredentialResetDto
```java
record CompanyAdminCredentialResetDto(
    Long companyId,
    String companyCode,
    String resetEmail,
    String status  // "credentials-emailed"
)
```

### SuperAdminTenantAdminEmailChangeRequestDto
```java
record SuperAdminTenantAdminEmailChangeRequestDto(
    Long requestId,
    Long companyId,
    String companyCode,
    Long adminUserId,
    String currentEmail,
    String requestedEmail,
    Instant verificationSentAt,
    Instant expiresAt
)
```

### SuperAdminTenantAdminEmailChangeConfirmationDto
```java
record SuperAdminTenantAdminEmailChangeConfirmationDto(
    Long requestId,
    Long companyId,
    String companyCode,
    Long adminUserId,
    String updatedEmail,
    Instant verifiedAt,
    Instant confirmedAt
)
```

---

## Support DTOs

### CompanySupportWarningDto
```java
record CompanySupportWarningDto(
    Long companyId,
    String companyCode,
    String warningId,
    String warningCategory,
    String message,
    String requestedLifecycleState,
    Integer gracePeriodHours,
    String issuedBy,
    Instant issuedAt
)
```

---

## Onboarding DTOs

### TenantOnboardingRequest
```java
record TenantOnboardingRequest(
    @NotBlank @Size(max=255) String name,
    @NotBlank @Size(max=64) String code,
    @NotBlank @Size(max=64) String timezone,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal defaultGstRate,
    @Min(0) Long maxActiveUsers,
    @Min(0) Long maxApiRequests,
    @Min(0) Long maxStorageBytes,
    @Min(0) Long maxConcurrentRequests,
    Boolean softLimitEnabled,
    Boolean hardLimitEnabled,
    @Email @NotBlank String firstAdminEmail,
    @Size(max=255) String firstAdminDisplayName,
    @NotBlank @Size(max=64) String coaTemplateCode
)
```

### TenantOnboardingResponse
```java
record TenantOnboardingResponse(
    Long companyId,
    String companyCode,
    String templateCode,
    String bootstrapMode,
    boolean seededChartOfAccounts,
    Integer accountsCreated,
    Long accountingPeriodId,
    boolean defaultAccountingPeriodCreated,
    String adminEmail,
    Long mainAdminUserId,
    boolean tenantAdminProvisioned,
    boolean credentialsEmailSent,
    Instant credentialsEmailedAt,
    Instant onboardingCompletedAt,
    boolean systemSettingsInitialized
)
```

### CoATemplateDto
```java
record CoATemplateDto(
    String code,
    String name,
    String description,
    Integer accountCount
)
```
