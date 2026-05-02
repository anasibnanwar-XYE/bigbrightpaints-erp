package com.bigbrightpaints.erp.modules.company.service;

import java.math.BigDecimal;

final class TenantBootstrapDefaults {

  private static final BigDecimal DEFAULT_BOOTSTRAP_GST_RATE = BigDecimal.valueOf(18);

  private TenantBootstrapDefaults() {}

  static BigDecimal resolveDefaultGstRate(BigDecimal requestedDefaultGstRate) {
    return requestedDefaultGstRate == null ? DEFAULT_BOOTSTRAP_GST_RATE : requestedDefaultGstRate;
  }

  static int runtimeLimitFromQuota(long configuredLimit) {
    if (configuredLimit <= 0L) {
      return 0;
    }
    return configuredLimit > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) configuredLimit;
  }
}
