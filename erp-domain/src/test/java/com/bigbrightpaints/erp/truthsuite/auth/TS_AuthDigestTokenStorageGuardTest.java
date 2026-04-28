package com.bigbrightpaints.erp.truthsuite.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
class TS_AuthDigestTokenStorageGuardTest {

  private static final String PASSWORD_RESET_ENTITY =
      "src/main/java/com/bigbrightpaints/erp/modules/auth/domain/PasswordResetToken.java";
  private static final String PASSWORD_RESET_REPOSITORY =
      "src/main/java/com/bigbrightpaints/erp/modules/auth/domain/PasswordResetTokenRepository.java";
  private static final String PASSWORD_RESET_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetService.java";
  private static final String ACTIVATION_ENTITY =
      "src/main/java/com/bigbrightpaints/erp/modules/company/domain/TenantActivationToken.java";
  private static final String ACTIVATION_REPOSITORY =
      "src/main/java/com/bigbrightpaints/erp/modules/company/domain/TenantActivationTokenRepository.java";
  private static final String ACTIVATION_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/company/service/SuperAdminTenantControlPlaneService.java";
  private static final String RESET_DIGEST_MIGRATION =
      "src/main/resources/db/migration_v2/V190__iam_core_schema_and_model_hard_cut.sql";
  private static final String ACTIVATION_MIGRATION =
      "src/main/resources/db/migration_v2/V191__super_admin_add_client_activation.sql";

  @Test
  void passwordResetTokensHaveNoRawTokenPersistenceColumnOrEntityField() {
    String entity = TruthSuiteFileAssert.read(PASSWORD_RESET_ENTITY);
    assertThat(entity)
        .contains("@Column(name = \"token_digest\", nullable = false, length = 64)")
        .contains("static PasswordResetToken digestOnly(")
        .doesNotContain("@Column(name = \"token\"")
        .doesNotContain("private String token;")
        .doesNotContain("getToken()")
        .doesNotContain("migrateToDigest");

    TruthSuiteFileAssert.assertContainsInOrder(
        RESET_DIGEST_MIGRATION,
        "DROP CONSTRAINT IF EXISTS password_reset_tokens_token_key;",
        "DROP COLUMN IF EXISTS token;",
        "ALTER COLUMN token_digest SET NOT NULL",
        "chk_password_reset_tokens_token_digest_length");
  }

  @Test
  void activationTokensHaveDigestOnlySchemaAndEntityFields() {
    String entity = TruthSuiteFileAssert.read(ACTIVATION_ENTITY);
    assertThat(entity)
        .contains("@Column(name = \"token_digest\", nullable = false, length = 64)")
        .contains("TenantActivationToken digestOnly(")
        .doesNotContain("@Column(name = \"token\"")
        .doesNotContain("activationUrl")
        .doesNotContain("activationLink");

    String migration = TruthSuiteFileAssert.read(ACTIVATION_MIGRATION).toLowerCase();
    assertThat(migration).contains("token_digest varchar(64) not null");
    assertThat(migration.replace("token_digest", ""))
        .doesNotContain(" activation_token ")
        .doesNotContain(" activation_url ")
        .doesNotContain(" activation_link ")
        .doesNotContain(" token ");
    TruthSuiteFileAssert.assertContains(
        ACTIVATION_MIGRATION, "chk_tenant_activation_tokens_token_digest_hex");
  }

  @Test
  void tokenLookupsHashInputBeforeRepositoryLookup() {
    TruthSuiteFileAssert.assertContainsInOrder(
        PASSWORD_RESET_SERVICE,
        "String tokenDigest = AuthTokenDigests.passwordResetTokenDigest(tokenValue);",
        ".findByTokenDigest(tokenDigest)");
    TruthSuiteFileAssert.assertContainsInOrder(
        PASSWORD_RESET_SERVICE,
        "String token = generateToken();",
        "PasswordResetToken.digestOnly(",
        "AuthTokenDigests.passwordResetTokenDigest(token)");
    TruthSuiteFileAssert.assertContains(PASSWORD_RESET_SERVICE, "sendPasswordResetEmailRequired");
    TruthSuiteFileAssert.assertContains(
        PASSWORD_RESET_REPOSITORY, "findByTokenDigest", "deleteByTokenDigest");

    TruthSuiteFileAssert.assertContainsInOrder(
        ACTIVATION_SERVICE,
        "String rawToken = newActivationToken();",
        "TenantActivationToken.digestOnly(",
        "activationTokenDigest(rawToken)");
    TruthSuiteFileAssert.assertContainsInOrder(
        ACTIVATION_SERVICE, ".findByTokenDigest(activationTokenDigest(tokenValue.trim()))");
    TruthSuiteFileAssert.assertContains(ACTIVATION_REPOSITORY, "findByTokenDigest");
  }

  @Test
  void auditAndLogMetadataStayTokenMetadataOnly() {
    String activationAuditMethod =
        region(
            TruthSuiteFileAssert.read(ACTIVATION_SERVICE),
            "private Map<String, String> activationAuditMetadata",
            "private List<String> activationRedactedFields");
    assertThat(activationAuditMethod)
        .contains("activationTokenId")
        .contains("activationExpiresAt")
        .contains("redactedFields")
        .doesNotContain("rawToken")
        .doesNotContain("activationUrl")
        .doesNotContain("activationLink");

    String resetAuditMethods =
        region(
            TruthSuiteFileAssert.read(PASSWORD_RESET_SERVICE),
            "private java.util.Map<String, String> resetAuditMetadata",
            "private void logTenantContextIgnoredIfPresent");
    assertThat(resetAuditMethods)
        .contains("resetAuditMetadata")
        .doesNotContain("rawToken")
        .doesNotContain("resetUrl")
        .doesNotContain("tokenValue");
    assertThat(TruthSuiteFileAssert.read(PASSWORD_RESET_SERVICE))
        .doesNotContain("System.out", "printStackTrace");
    assertThat(TruthSuiteFileAssert.read(ACTIVATION_SERVICE))
        .doesNotContain("System.out", "printStackTrace");
  }

  private String region(String content, String startNeedle, String endNeedle) {
    int start = content.indexOf(startNeedle);
    assertThat(start).as("region start should exist: %s", startNeedle).isGreaterThanOrEqualTo(0);
    int end = content.indexOf(endNeedle, start + startNeedle.length());
    assertThat(end).as("region end should exist: %s", endNeedle).isGreaterThan(start);
    return content.substring(start, end);
  }
}
