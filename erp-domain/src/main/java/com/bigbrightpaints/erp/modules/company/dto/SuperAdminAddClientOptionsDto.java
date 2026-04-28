package com.bigbrightpaints.erp.modules.company.dto;

import java.util.List;

public record SuperAdminAddClientOptionsDto(
    Section company,
    Section owner,
    Section commercial,
    Section quotas,
    Section modules,
    Section support,
    List<CreateModeOption> createModes,
    SeedPolicy seedPolicy) {

  public record Section(String key, String title, List<Field> fields) {}

  public record Field(
      String key,
      String type,
      boolean required,
      Object defaultValue,
      List<String> enumValues,
      List<String> dependencies,
      String validationHint) {}

  public record CreateModeOption(
      String value, String label, String description, ActivationEffect activationEffect) {}

  public record ActivationEffect(
      String initialStatus, String activationStatus, boolean sendsEmail) {}

  public record SeedPolicy(
      String version,
      boolean requiredBeforeActivation,
      List<SeedCategory> categories,
      String activationRule) {}

  public record SeedCategory(
      String key, String label, String status, boolean requiredBeforeActivation, String detail) {}
}
