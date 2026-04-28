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
  private static final String AUTH_TOKEN_DIGESTS =
      "src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthTokenDigests.java";
  private static final String ACTIVATION_ENTITY =
      "src/main/java/com/bigbrightpaints/erp/modules/company/domain/TenantActivationToken.java";
  private static final String ACTIVATION_REPOSITORY =
      "src/main/java/com/bigbrightpaints/erp/modules/company/domain/TenantActivationTokenRepository.java";
  private static final String ACTIVATION_SERVICE =
      "src/main/java/com/bigbrightpaints/erp/modules/company/service/SuperAdminTenantControlPlaneService.java";
  private static final String RESET_DIGEST_MIGRATION =
      "src/main/resources/db/migration_v2/V190__iam_core_schema_and_model_hard_cut.sql";
  private static final String DIGEST_METADATA_MIGRATION =
      "src/main/resources/db/migration_v2/V192__token_digest_metadata.sql";
  private static final String ACTIVATION_MIGRATION =
      "src/main/resources/db/migration_v2/V191__super_admin_add_client_activation.sql";

  @Test
  void passwordResetTokensHaveNoRawTokenPersistenceColumnOrEntityField() {
    String entity = TruthSuiteFileAssert.read(PASSWORD_RESET_ENTITY);
    assertThat(entity)
        .contains("@Column(name = \"token_digest\", nullable = false, length = 64)")
        .contains("@Column(name = \"digest_algorithm\", nullable = false, length = 32)")
        .contains("@Column(name = \"digest_version\", nullable = false)")
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
        .contains("@Column(name = \"digest_algorithm\", nullable = false, length = 32)")
        .contains("@Column(name = \"digest_version\", nullable = false)")
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
  void tokenDigestMetadataIsBackfilledAndEnforcedWithoutRawTokenColumns() {
    String metadataMigration = TruthSuiteFileAssert.read(DIGEST_METADATA_MIGRATION);
    TruthSuiteFileAssert.assertContainsInOrder(
        DIGEST_METADATA_MIGRATION,
        "ALTER TABLE password_reset_tokens",
        "ADD COLUMN IF NOT EXISTS digest_algorithm VARCHAR(32)",
        "ADD COLUMN IF NOT EXISTS digest_version INTEGER",
        "UPDATE password_reset_tokens",
        "digest_algorithm = 'SHA-256'",
        "UPDATE password_reset_tokens",
        "digest_version = 1",
        "ALTER COLUMN digest_algorithm SET NOT NULL",
        "ALTER COLUMN digest_version SET NOT NULL",
        "chk_password_reset_tokens_digest_algorithm",
        "chk_password_reset_tokens_digest_version");
    TruthSuiteFileAssert.assertContainsInOrder(
        DIGEST_METADATA_MIGRATION,
        "ALTER TABLE tenant_activation_tokens",
        "ADD COLUMN IF NOT EXISTS digest_algorithm VARCHAR(32)",
        "ADD COLUMN IF NOT EXISTS digest_version INTEGER",
        "UPDATE tenant_activation_tokens",
        "digest_algorithm = 'SHA-256'",
        "UPDATE tenant_activation_tokens",
        "digest_version = 1",
        "ALTER COLUMN digest_algorithm SET NOT NULL",
        "ALTER COLUMN digest_version SET NOT NULL",
        "chk_tenant_activation_tokens_digest_algorithm",
        "chk_tenant_activation_tokens_digest_version");
    assertThat(metadataMigration.replace("digest_version", "").replace("digest_algorithm", ""))
        .doesNotContain(" raw_token ")
        .doesNotContain(" activation_link ")
        .doesNotContain(" activation_url ")
        .doesNotContain(" reset_link ");
  }

  @Test
  void tokenLookupsHashInputBeforeRepositoryLookup() {
    assertThat(TruthSuiteFileAssert.read(AUTH_TOKEN_DIGESTS))
        .contains("public static final String DIGEST_ALGORITHM = \"SHA-256\";")
        .contains("public static final int DIGEST_VERSION = 1;")
        .contains("public static String passwordResetTokenDigest(String token)")
        .contains("public static String tenantActivationTokenDigest(String token)");
    TruthSuiteFileAssert.assertContainsInOrder(
        PASSWORD_RESET_SERVICE,
        "String tokenDigest = AuthTokenDigests.passwordResetTokenDigest(tokenValue);",
        ".findByTokenDigest(tokenDigest)");
    TruthSuiteFileAssert.assertContainsInOrder(
        PASSWORD_RESET_SERVICE,
        "String token = generateToken();",
        "PasswordResetToken.digestOnly(",
        "AuthTokenDigests.passwordResetTokenDigest(token)",
        "AuthTokenDigests.DIGEST_ALGORITHM",
        "AuthTokenDigests.DIGEST_VERSION");
    TruthSuiteFileAssert.assertContains(PASSWORD_RESET_SERVICE, "sendPasswordResetEmailRequired");
    TruthSuiteFileAssert.assertContains(
        PASSWORD_RESET_REPOSITORY, "findByTokenDigest", "deleteByTokenDigest");

    TruthSuiteFileAssert.assertContainsInOrder(
        ACTIVATION_SERVICE,
        "String rawToken = newActivationToken();",
        "TenantActivationToken.digestOnly(",
        "AuthTokenDigests.tenantActivationTokenDigest(rawToken)",
        "AuthTokenDigests.DIGEST_ALGORITHM",
        "AuthTokenDigests.DIGEST_VERSION");
    TruthSuiteFileAssert.assertContainsInOrder(
        ACTIVATION_SERVICE,
        ".findByTokenDigest(AuthTokenDigests.tenantActivationTokenDigest(tokenValue.trim()))");
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
