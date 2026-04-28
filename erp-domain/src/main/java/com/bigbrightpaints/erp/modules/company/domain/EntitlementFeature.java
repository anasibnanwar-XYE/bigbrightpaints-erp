package com.bigbrightpaints.erp.modules.company.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.StringUtils;

public enum EntitlementFeature {
  CUSTOM_PLAN("CUSTOM_PLAN", null, false, false, false),
  ACCOUNTING("ACCOUNTING", CompanyModule.ACCOUNTING, false, false, true),
  SALES("SALES", CompanyModule.SALES, false, false, true),
  INVENTORY("INVENTORY", CompanyModule.INVENTORY, false, false, true),
  PRODUCTION("PRODUCTION", CompanyModule.MANUFACTURING, true, true, false),
  HR("HR", CompanyModule.HR_PAYROLL, true, true, false),
  PURCHASING("PURCHASING", CompanyModule.PURCHASING, true, true, false),
  PORTAL("PORTAL", CompanyModule.PORTAL, true, true, false),
  REPORTS("REPORTS", CompanyModule.REPORTS_ADVANCED, true, true, false);

  private final String key;
  private final CompanyModule module;
  private final boolean runtimeGatable;
  private final boolean mutable;
  private final boolean alwaysOn;

  EntitlementFeature(
      String key,
      CompanyModule module,
      boolean runtimeGatable,
      boolean mutable,
      boolean alwaysOn) {
    this.key = key;
    this.module = module;
    this.runtimeGatable = runtimeGatable;
    this.mutable = mutable;
    this.alwaysOn = alwaysOn;
  }

  public String key() {
    return key;
  }

  public Optional<CompanyModule> module() {
    return Optional.ofNullable(module);
  }

  public boolean runtimeGatable() {
    return runtimeGatable;
  }

  public boolean mutable() {
    return mutable;
  }

  public boolean alwaysOn() {
    return alwaysOn;
  }

  public static Set<String> canonicalKeys() {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    Arrays.stream(values()).map(EntitlementFeature::key).forEach(keys::add);
    return keys;
  }

  public static String keyForModule(CompanyModule module) {
    return Arrays.stream(values())
        .filter(feature -> feature.module == module)
        .map(EntitlementFeature::key)
        .findFirst()
        .orElse(module.name());
  }

  public static EntitlementFeature require(String rawValue, InvalidFeature invalidFeature) {
    String normalized = normalize(rawValue);
    return Arrays.stream(values())
        .filter(feature -> feature.key.equals(normalized))
        .findFirst()
        .orElseThrow(() -> invalidFeature.invalid("Unsupported entitlement feature: " + rawValue));
  }

  public static String normalizeKey(String rawValue, InvalidFeature invalidFeature) {
    return require(rawValue, invalidFeature).key();
  }

  private static String normalize(String rawValue) {
    if (!StringUtils.hasText(rawValue)) {
      return "";
    }
    return rawValue.trim().toUpperCase(Locale.ROOT);
  }

  @FunctionalInterface
  public interface InvalidFeature {
    RuntimeException invalid(String message);
  }
}
