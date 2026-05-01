package com.bigbrightpaints.erp.core.security;

/**
 * Canonical retired tenant-admin hosts that intentionally fall through to dispatcher 404.
 */
public final class RetiredTenantAdminHostPaths {

  public static final String ADMIN_SETTINGS = "/api/v1/admin/settings";
  public static final String ADMIN_SETTINGS_WILDCARD = "/api/v1/admin/settings/**";
  public static final String ADMIN_ROLES = "/api/v1/admin/roles";
  public static final String ADMIN_ROLES_WILDCARD = "/api/v1/admin/roles/**";
  public static final String ADMIN_NOTIFY = "/api/v1/admin/notify";
  public static final String ADMIN_NOTIFY_WILDCARD = "/api/v1/admin/notify/**";

  private static final String[] REQUEST_MATCHERS = {
    ADMIN_SETTINGS,
    ADMIN_SETTINGS_WILDCARD,
    ADMIN_ROLES,
    ADMIN_ROLES_WILDCARD,
    ADMIN_NOTIFY,
    ADMIN_NOTIFY_WILDCARD
  };

  private RetiredTenantAdminHostPaths() {}

  public static boolean matchesNormalizedPath(String normalizedPath) {
    if (normalizedPath == null || normalizedPath.isBlank()) {
      return false;
    }
    return normalizedPath.equals(ADMIN_SETTINGS)
        || normalizedPath.startsWith(ADMIN_SETTINGS + "/")
        || normalizedPath.equals(ADMIN_ROLES)
        || normalizedPath.startsWith(ADMIN_ROLES + "/")
        || normalizedPath.equals(ADMIN_NOTIFY)
        || normalizedPath.startsWith(ADMIN_NOTIFY + "/");
  }

  public static boolean matchesNormalizedPath(String normalizedPath, String method) {
    if (matchesNormalizedPath(normalizedPath)) {
      return true;
    }
    if (normalizedPath == null || normalizedPath.isBlank() || method == null) {
      return false;
    }
    String normalizedMethod = method.trim().toUpperCase(java.util.Locale.ROOT);
    if ("PATCH".equals(normalizedMethod)
        && (isAdminUserChildAlias(normalizedPath, "suspend")
            || isAdminUserChildAlias(normalizedPath, "unsuspend"))) {
      return true;
    }
    return "DELETE".equals(normalizedMethod) && isAdminUserDetailPath(normalizedPath);
  }

  private static boolean isAdminUserChildAlias(String normalizedPath, String alias) {
    String prefix = "/api/v1/admin/users/";
    return normalizedPath.startsWith(prefix)
        && normalizedPath.endsWith("/" + alias)
        && normalizedPath
                .substring(prefix.length(), normalizedPath.length() - alias.length() - 1)
                .indexOf('/')
            < 0;
  }

  private static boolean isAdminUserDetailPath(String normalizedPath) {
    String prefix = "/api/v1/admin/users/";
    if (!normalizedPath.startsWith(prefix)) {
      return false;
    }
    String remainder = normalizedPath.substring(prefix.length());
    return !remainder.isBlank() && remainder.indexOf('/') < 0;
  }

  public static String[] requestMatchers() {
    return REQUEST_MATCHERS.clone();
  }
}
