package com.bigbrightpaints.erp.truthsuite.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
class TS_IamCoreSchemaAndModelHardCutMigrationContractTest {

  private static final String V2_MIGRATION =
      "src/main/resources/db/migration_v2/V190__iam_core_schema_and_model_hard_cut.sql";

  @Test
  void v2MigrationAddsForwardOnlyCanonicalIamSchema() {
    TruthSuiteFileAssert.assertContains(
        V2_MIGRATION,
        "CREATE TABLE IF NOT EXISTS iam_accounts",
        "CREATE TABLE IF NOT EXISTS iam_account_profiles",
        "CREATE TABLE IF NOT EXISTS iam_account_contacts",
        "CREATE TABLE IF NOT EXISTS iam_credentials",
        "CREATE TABLE IF NOT EXISTS iam_mfa_factors",
        "CREATE TABLE IF NOT EXISTS iam_sessions",
        "CREATE TABLE IF NOT EXISTS iam_devices",
        "CREATE TABLE IF NOT EXISTS iam_security_events");
  }

  @Test
  void v2MigrationRemovesRawVerifierStorage() {
    TruthSuiteFileAssert.assertContainsInOrder(
        V2_MIGRATION,
        "INSERT INTO mfa_recovery_codes",
        "ALTER TABLE refresh_tokens",
        "DROP COLUMN IF EXISTS token",
        "ALTER TABLE password_reset_tokens",
        "DROP COLUMN IF EXISTS token",
        "ALTER TABLE app_users",
        "DROP COLUMN IF EXISTS mfa_recovery_codes");
    TruthSuiteFileAssert.assertContains(
        V2_MIGRATION,
        "CONSTRAINT chk_iam_sessions_digest_length CHECK (length(refresh_token_digest) = 64)",
        "CONSTRAINT chk_refresh_tokens_token_digest_length CHECK (length(token_digest) = 64)",
        "CONSTRAINT chk_password_reset_tokens_token_digest_length CHECK (length(token_digest) ="
            + " 64)");
  }

  @Test
  void v2MigrationEncodesIdentityOwnershipBoundaries() {
    TruthSuiteFileAssert.assertContains(
        V2_MIGRATION,
        "Identity-owned profile fields only",
        "Identity-owned contact fields only",
        "CONSTRAINT chk_iam_accounts_company_boundary",
        "CONSTRAINT chk_iam_account_contacts_primary_email_normalized",
        "CONSTRAINT chk_iam_mfa_factors_type CHECK (factor_type = 'TOTP')");
  }
}
