package com.bigbrightpaints.erp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class IdentityRouteInventoryContractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void val_route_001_openapi_identity_inventory_exposes_canonical_routes() throws IOException {
    JsonNode root = readSnapshot();

    assertOperations(
        root,
        List.of(
            op("POST", "/api/v1/auth/login"),
            op("POST", "/api/v1/auth/refresh-token"),
            op("POST", "/api/v1/auth/logout"),
            op("GET", "/api/v1/auth/me"),
            op("PATCH", "/api/v1/auth/me/profile"),
            op("PATCH", "/api/v1/auth/me/contact"),
            op("GET", "/api/v1/auth/me/security"),
            op("GET", "/api/v1/auth/me/security-events"),
            op("POST", "/api/v1/auth/password/change"),
            op("POST", "/api/v1/auth/password/forgot"),
            op("POST", "/api/v1/auth/password/reset"),
            op("GET", "/api/v1/auth/mfa"),
            op("POST", "/api/v1/auth/mfa/setup"),
            op("POST", "/api/v1/auth/mfa/activate"),
            op("POST", "/api/v1/auth/mfa/disable"),
            op("POST", "/api/v1/auth/mfa/recovery-codes/regenerate"),
            op("GET", "/api/v1/auth/sessions"),
            op("DELETE", "/api/v1/auth/sessions/{sessionId}"),
            op("DELETE", "/api/v1/auth/sessions/current"),
            op("DELETE", "/api/v1/auth/sessions"),
            op("GET", "/api/v1/admin/users"),
            op("POST", "/api/v1/admin/users"),
            op("GET", "/api/v1/admin/users/{userId}"),
            op("PUT", "/api/v1/admin/users/{userId}"),
            op("PUT", "/api/v1/admin/users/{userId}/status"),
            op("POST", "/api/v1/admin/users/{userId}/lock"),
            op("POST", "/api/v1/admin/users/{userId}/unlock"),
            op("POST", "/api/v1/admin/users/{userId}/force-reset-password"),
            op("PATCH", "/api/v1/admin/users/{id}/mfa/disable"),
            op("DELETE", "/api/v1/admin/users/{userId}/sessions"),
            op("GET", "/api/v1/admin/users/{userId}/security-events"),
            op("GET", "/api/v1/admin/users/assignable-roles"),
            op("PUT", "/api/v1/superadmin/tenants/{id}/lifecycle"),
            op("PUT", "/api/v1/superadmin/tenants/{id}/limits")));
  }

  @Test
  void val_route_001_retired_duplicate_identity_routes_are_absent_from_openapi_inventory()
      throws IOException {
    JsonNode root = readSnapshot();

    assertMissingOperations(
        root,
        List.of(
            op("GET", "/api/v1/auth/profile"),
            op("POST", "/api/v1/auth/profile"),
            op("PUT", "/api/v1/auth/profile"),
            op("PATCH", "/api/v1/auth/profile"),
            op("DELETE", "/api/v1/auth/profile"),
            op("POST", "/api/v1/auth/password/forgot/superadmin"),
            op("PATCH", "/api/v1/admin/users/{userId}/suspend"),
            op("PATCH", "/api/v1/admin/users/{userId}/unsuspend"),
            op("DELETE", "/api/v1/admin/users/{userId}")));
  }

  @Test
  void identity_response_schemas_do_not_expose_credential_or_token_storage_fields()
      throws IOException {
    JsonNode schemas = readSnapshot().path("components").path("schemas");

    assertSchemaPropertiesExcludeSecrets(
        schemas,
        "AuthResponse",
        List.of("passwordHash", "resetToken", "mfaSecret", "recoveryCodeHashes", "tokenDigest"));
    assertSchemaPropertiesExcludeSecrets(
        schemas,
        "MeResponse",
        List.of(
            "passwordHash",
            "accessToken",
            "refreshToken",
            "resetToken",
            "mfaSecret",
            "recoveryCodes",
            "recoveryCodeHashes",
            "tokenDigest",
            "companyId"));
    assertSchemaPropertiesExcludeSecrets(
        schemas,
        "UserDto",
        List.of(
            "passwordHash",
            "accessToken",
            "refreshToken",
            "resetToken",
            "mfaSecret",
            "recoveryCodes",
            "recoveryCodeHashes",
            "tokenDigest"));
  }

  private JsonNode readSnapshot() throws IOException {
    return OBJECT_MAPPER.readTree(Files.readString(resolveRepoRoot().resolve("openapi.json")));
  }

  private static Path resolveRepoRoot() {
    Path moduleRoot = Path.of("").toAbsolutePath().normalize();
    if (moduleRoot.getFileName() != null
        && "erp-domain".equals(moduleRoot.getFileName().toString())) {
      return moduleRoot.getParent();
    }
    return moduleRoot;
  }

  private static Operation op(String method, String path) {
    return new Operation(method.toLowerCase(Locale.ROOT), path);
  }

  private static void assertOperations(JsonNode root, List<Operation> expectedOperations) {
    List<String> missing = new ArrayList<>();
    for (Operation operation : expectedOperations) {
      if (!hasOperation(root, operation)) {
        missing.add(operation.method().toUpperCase(Locale.ROOT) + " " + operation.path());
      }
    }
    assertThat(missing).as("expected current identity route inventory").isEmpty();
  }

  private static void assertMissingOperations(JsonNode root, List<Operation> expectedMissing) {
    List<String> present = new ArrayList<>();
    for (Operation operation : expectedMissing) {
      if (hasOperation(root, operation)) {
        present.add(operation.method().toUpperCase(Locale.ROOT) + " " + operation.path());
      }
    }
    assertThat(present).as("expected absent identity route inventory").isEmpty();
  }

  private static boolean hasOperation(JsonNode root, Operation operation) {
    String expectedPath = normalizePathTemplate(operation.path());
    Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").fields();
    while (paths.hasNext()) {
      Map.Entry<String, JsonNode> path = paths.next();
      if (expectedPath.equals(normalizePathTemplate(path.getKey()))
          && !path.getValue().path(operation.method()).isMissingNode()) {
        return true;
      }
    }
    return false;
  }

  private static String normalizePathTemplate(String path) {
    return path.replaceAll("\\{[^/}]+}", "{}");
  }

  private static void assertSchemaPropertiesExcludeSecrets(
      JsonNode schemas, String schemaName, List<String> disallowedProperties) {
    JsonNode properties = schemas.path(schemaName).path("properties");
    assertThat(properties.isMissingNode())
        .withFailMessage("Missing OpenAPI schema properties for %s", schemaName)
        .isFalse();
    for (String disallowedProperty : disallowedProperties) {
      assertThat(properties.has(disallowedProperty))
          .withFailMessage(
              "Schema %s must not expose secret/storage field %s", schemaName, disallowedProperty)
          .isFalse();
    }
  }

  private record Operation(String method, String path) {}
}
