package com.bigbrightpaints.erp.truthsuite.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

@Tag("critical")
class IamCoreSchemaAndModelHardCutMigrationIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void canonicalIamTablesExistAfterFlywayV2Migration() {
    assertThat(existingTables())
        .contains(
            "iam_accounts",
            "iam_account_profiles",
            "iam_account_contacts",
            "iam_credentials",
            "iam_mfa_factors",
            "iam_sessions",
            "iam_devices",
            "iam_security_events",
            "mfa_recovery_codes");
  }

  @Test
  void verifierOnlyStorageRemovesRawTokenAndDelimitedRecoveryColumns() {
    assertThat(columns("refresh_tokens")).contains("token_digest").doesNotContain("token");
    assertThat(columns("password_reset_tokens")).contains("token_digest").doesNotContain("token");
    assertThat(columns("app_users")).doesNotContain("mfa_recovery_codes");
    assertThat(columns("iam_sessions"))
        .contains("refresh_token_digest", "revoked_at", "consumed_at")
        .doesNotContain("refresh_token", "token");
  }

  @Test
  void accountProfileContactCredentialAndMfaOwnershipColumnsAreSeparated() {
    assertThat(columns("iam_accounts"))
        .contains(
            "user_id",
            "public_id",
            "account_type",
            "auth_scope_code",
            "company_id",
            "status",
            "locked_until",
            "failed_login_attempts",
            "must_change_password")
        .doesNotContain("password_hash", "secondary_email", "phone_secondary");
    assertThat(columns("iam_account_profiles"))
        .contains("display_name", "preferred_name", "profile_picture_url", "job_title");
    assertThat(columns("iam_account_contacts"))
        .contains("primary_email", "secondary_email", "phone_secondary");
    assertThat(columns("iam_credentials")).contains("password_hash", "must_change_password");
    assertThat(columns("iam_mfa_factors")).contains("factor_type", "encrypted_secret", "status");
  }

  private List<String> existingTables() {
    return jdbcTemplate.queryForList(
        """
        select table_name
          from information_schema.tables
         where table_schema = 'public'
        """,
        String.class);
  }

  private List<String> columns(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select column_name
          from information_schema.columns
         where table_schema = 'public'
           and table_name = ?
        """,
        String.class,
        tableName);
  }
}
