package com.bigbrightpaints.erp.core.config;

import java.util.List;
import java.util.Locale;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
public class OpenApiTaggingConfig {
  @Bean
  public OpenApiCustomizer moduleTagCustomizer() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }
      openApi
          .getPaths()
          .forEach(
              (path, item) -> {
                String tag = resolveTag(path);
                if (tag == null) {
                  return;
                }
                item.readOperations()
                    .forEach(
                        operation -> {
                          applyTag(operation, tag);
                          applyControlPlaneErrors(path, operation);
                        });
              });
    };
  }

  private void applyTag(Operation operation, String tag) {
    if (operation.getTags() == null || operation.getTags().isEmpty()) {
      operation.setTags(List.of(tag));
      return;
    }
    if (!operation.getTags().contains(tag)) {
      operation.addTagsItem(tag);
    }
  }

  private String resolveTag(String path) {
    String normalized = path.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("/api/v1/superadmin")) {
      return "SUPER_ADMIN";
    }
    if (normalized.startsWith("/api/v1/admin")
        || normalized.startsWith("/api/v1/auth")
        || normalized.startsWith("/api/v1/companies")
        || normalized.startsWith("/api/v1/orchestrator")
        || normalized.startsWith("/api/v1/portal")
        || normalized.startsWith("/api/v1/demo")
        || normalized.startsWith("/api/integration")) {
      return "ADMIN";
    }
    if (normalized.startsWith("/api/v1/accounting")
        || normalized.startsWith("/api/v1/reports")
        || normalized.startsWith("/api/v1/purchasing")
        || normalized.startsWith("/api/v1/inventory")
        || normalized.startsWith("/api/v1/dispatch")
        || normalized.startsWith("/api/v1/raw-materials")
        || normalized.startsWith("/api/v1/finished-goods")
        || normalized.startsWith("/api/v1/packaging")
        || normalized.startsWith("/api/v1/hr")
        || normalized.startsWith("/api/v1/payroll")
        || normalized.startsWith("/api/v1/suppliers")) {
      return "ACCOUNTING";
    }
    if (normalized.startsWith("/api/v1/factory") || normalized.startsWith("/api/v1/production")) {
      return "FACTORY_PRODUCTION";
    }
    if (normalized.startsWith("/api/v1/sales")
        || normalized.startsWith("/api/v1/invoices")
        || normalized.startsWith("/api/v1/credit/override-requests")
        || normalized.startsWith("/api/v1/credit/limit-requests")) {
      return "SALES";
    }
    if (normalized.startsWith("/api/v1/dealers")
        || normalized.startsWith("/api/v1/dealer-portal")) {
      return "DEALERS";
    }
    return null;
  }

  private void applyControlPlaneErrors(String path, Operation operation) {
    String normalized = path.toLowerCase(Locale.ROOT);
    if (!normalized.startsWith("/api/v1/superadmin") && !normalized.startsWith("/api/v1/auth")) {
      return;
    }
    if (operation.getResponses() == null) {
      return;
    }
    addErrorResponse(operation, "400", "Validation, parser, oversized query/header, or bad input");
    addErrorResponse(operation, "401", "Authentication failed or token missing");
    addErrorResponse(operation, "403", "Authenticated actor is not allowed");
    addErrorResponse(operation, "404", "Requested resource was not found");
    addErrorResponse(operation, "405", "HTTP method is not supported for this endpoint");
    addErrorResponse(operation, "406", "Requested response media type is not available");
    addErrorResponse(operation, "409", "Conflict or duplicate input");
    addErrorResponse(operation, "413", "Request body is too large");
    addErrorResponse(operation, "415", "Unsupported request media type");
    addErrorResponse(operation, "429", "Rate limit exceeded");
  }

  private void addErrorResponse(Operation operation, String status, String description) {
    if (operation.getResponses().containsKey(status)) {
      return;
    }
    operation
        .getResponses()
        .addApiResponse(
            status,
            new ApiResponse()
                .description(description + "; response uses ApiResponse metadata.traceId")
                .content(
                    new Content()
                        .addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(errorEnvelopeSchema()))));
  }

  private Schema<?> errorEnvelopeSchema() {
    return new Schema<>().$ref("#/components/schemas/ApiResponseMapStringObject");
  }
}
