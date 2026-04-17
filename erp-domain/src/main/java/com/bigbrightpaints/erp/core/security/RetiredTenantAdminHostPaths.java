package com.bigbrightpaints.erp.core.security;

/**
 * Canonical retired tenant-admin and company-control hosts that intentionally fall through to
 * dispatcher 404.
 */
public final class RetiredTenantAdminHostPaths {

  public static final String ADMIN_SETTINGS = "/api/v1/admin/settings";
  public static final String ADMIN_SETTINGS_WILDCARD = "/api/v1/admin/settings/**";
  public static final String ADMIN_ROLES = "/api/v1/admin/roles";
  public static final String ADMIN_ROLES_WILDCARD = "/api/v1/admin/roles/**";
  public static final String ADMIN_NOTIFY = "/api/v1/admin/notify";
  public static final String ADMIN_NOTIFY_WILDCARD = "/api/v1/admin/notify/**";

  public static final String COMPANIES_SUPERADMIN = "/api/v1/companies/superadmin";
  public static final String COMPANIES_SUPERADMIN_WILDCARD = "/api/v1/companies/superadmin/**";
  public static final String COMPANIES_COMPANY_ROOT = "/api/v1/companies/*";
  public static final String COMPANIES_COMPANY_SUPPORT_WILDCARD = "/api/v1/companies/*/support/**";
  public static final String COMPANIES_COMPANY_TENANT_METRICS =
      "/api/v1/companies/*/tenant-metrics";
  public static final String COMPANIES_COMPANY_TENANT_RUNTIME_WILDCARD =
      "/api/v1/companies/*/tenant-runtime/**";
  public static final String COMPANIES_COMPANY_LIFECYCLE_STATE =
      "/api/v1/companies/*/lifecycle-state";

  private static final String COMPANIES_PREFIX = "/api/v1/companies/";

  private static final String[] REQUEST_MATCHERS = {
    ADMIN_SETTINGS,
    ADMIN_SETTINGS_WILDCARD,
    ADMIN_ROLES,
    ADMIN_ROLES_WILDCARD,
    ADMIN_NOTIFY,
    ADMIN_NOTIFY_WILDCARD,
    COMPANIES_SUPERADMIN,
    COMPANIES_SUPERADMIN_WILDCARD,
    COMPANIES_COMPANY_ROOT,
    COMPANIES_COMPANY_SUPPORT_WILDCARD,
    COMPANIES_COMPANY_TENANT_METRICS,
    COMPANIES_COMPANY_TENANT_RUNTIME_WILDCARD,
    COMPANIES_COMPANY_LIFECYCLE_STATE
  };

  private RetiredTenantAdminHostPaths() {}

  public static boolean matchesNormalizedPath(String normalizedPath) {
    if (normalizedPath == null || normalizedPath.isBlank()) {
      return false;
    }
    return isRetiredAdminHost(normalizedPath) || isRetiredCompanyAliasHost(normalizedPath);
  }

  private static boolean isRetiredAdminHost(String normalizedPath) {
    return normalizedPath.equals(ADMIN_SETTINGS)
        || normalizedPath.startsWith(ADMIN_SETTINGS + "/")
        || normalizedPath.equals(ADMIN_ROLES)
        || normalizedPath.startsWith(ADMIN_ROLES + "/")
        || normalizedPath.equals(ADMIN_NOTIFY)
        || normalizedPath.startsWith(ADMIN_NOTIFY + "/");
  }

  private static boolean isRetiredCompanyAliasHost(String normalizedPath) {
    if (normalizedPath.equals(COMPANIES_SUPERADMIN)
        || normalizedPath.startsWith(COMPANIES_SUPERADMIN + "/")) {
      return true;
    }
    if (!normalizedPath.startsWith(COMPANIES_PREFIX)) {
      return false;
    }
    String companySegmentAndSuffix = normalizedPath.substring(COMPANIES_PREFIX.length());
    if (companySegmentAndSuffix.isBlank()) {
      return false;
    }
    int separator = companySegmentAndSuffix.indexOf('/');
    if (separator < 0) {
      // `/api/v1/companies/{id}` delete alias is retired from the live contract.
      return true;
    }
    String companySegment = companySegmentAndSuffix.substring(0, separator);
    if (companySegment.isBlank() || "superadmin".equalsIgnoreCase(companySegment)) {
      return false;
    }
    String suffix = companySegmentAndSuffix.substring(separator);
    return suffix.equals("/lifecycle-state")
        || suffix.equals("/tenant-metrics")
        || suffix.equals("/tenant-runtime")
        || suffix.startsWith("/tenant-runtime/")
        || suffix.equals("/support")
        || suffix.startsWith("/support/");
  }

  public static String[] requestMatchers() {
    return REQUEST_MATCHERS.clone();
  }
}
