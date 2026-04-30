package com.bigbrightpaints.erp.core.validationharness;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@RestController
@Profile("validation-harness")
@RequestMapping("/api/v1/validation/harness")
public class ValidationHarnessController {

  private final ValidationFaultInjectionService faultInjectionService;
  private final ValidationTimeControlService timeControlService;
  private final ValidationSecurityAlertTriggerService securityAlertTriggerService;
  private final Environment environment;

  public ValidationHarnessController(
      ValidationFaultInjectionService faultInjectionService,
      ValidationTimeControlService timeControlService,
      ValidationSecurityAlertTriggerService securityAlertTriggerService,
      Environment environment) {
    this.faultInjectionService = faultInjectionService;
    this.timeControlService = timeControlService;
    this.securityAlertTriggerService = securityAlertTriggerService;
    this.environment = environment;
  }

  @GetMapping("/status")
  public ApiResponse<HarnessStatus> status(
      @RequestParam(name = "runMarker", required = false) String runMarker) {
    String safeMarker =
        runMarker == null || runMarker.isBlank()
            ? null
            : ValidationRunNamespace.requireSafeRunMarker(runMarker);
    return ApiResponse.success(
        new HarnessStatus(
            true,
            safeMarker,
            Arrays.asList(environment.getActiveProfiles()),
            timeControlService.state(),
            faultInjectionService.states(),
            "validation-harness"));
  }

  @PutMapping("/time")
  public ApiResponse<ValidationTimeControlService.TimeState> freezeTime(
      @RequestBody TimeControlRequest request) {
    return ApiResponse.success(timeControlService.freeze(request.runMarker(), request.instant()));
  }

  @DeleteMapping("/time")
  public ApiResponse<ValidationTimeControlService.TimeState> clearTime(
      @RequestParam("runMarker") String runMarker) {
    return ApiResponse.success(timeControlService.clear(runMarker));
  }

  @GetMapping("/faults")
  public ApiResponse<List<ValidationFaultInjectionService.FaultState>> faults() {
    return ApiResponse.success(faultInjectionService.states());
  }

  @PutMapping("/faults/{fault}")
  public ApiResponse<ValidationFaultInjectionService.FaultState> setFault(
      @PathVariable("fault") String fault, @RequestBody FaultRequest request) {
    ValidationFaultKind kind = ValidationFaultKind.fromCode(fault);
    return ApiResponse.success(
        faultInjectionService.setFault(
            kind, request.enabled(), request.runMarker(), request.reason()));
  }

  @PostMapping("/security-alert")
  public ApiResponse<ValidationSecurityAlertTriggerService.TriggerResult> triggerSecurityAlert(
      @RequestBody SecurityAlertRequest request) {
    return ApiResponse.success(
        "Validation security alert triggered",
        securityAlertTriggerService.trigger(
            request.runMarker(), request.alertType(), request.reasonCode()));
  }

  public record TimeControlRequest(String runMarker, Instant instant) {}

  public record FaultRequest(String runMarker, boolean enabled, String reason) {}

  public record SecurityAlertRequest(String runMarker, String alertType, String reasonCode) {}

  public record HarnessStatus(
      boolean validationOnly,
      String runMarker,
      List<String> activeProfiles,
      ValidationTimeControlService.TimeState time,
      List<ValidationFaultInjectionService.FaultState> faults,
      String requiredProfile) {
    public Map<String, Object> evidenceSummary() {
      return Map.of(
          "validationOnly",
          validationOnly,
          "requiredProfile",
          requiredProfile,
          "timeControl",
          time.enabled(),
          "faultCount",
          faults.size());
    }
  }
}
