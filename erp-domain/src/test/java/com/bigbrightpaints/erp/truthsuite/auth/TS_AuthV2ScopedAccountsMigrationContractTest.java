package com.bigbrightpaints.erp.truthsuite.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;

@Tag("critical")
class TS_AuthV2ScopedAccountsMigrationContractTest {

  private static final String V2_MIGRATION =
      "src/main/resources/db/migration_v2/V167__auth_v2_scoped_accounts.sql";

  @Test
  void v2MigrationNormalizesEmailsBeforeScopedUniqueness() {
    TruthSuiteFileAssert.assertContains(
        V2_MIGRATION,
        "SET email = LOWER(TRIM(email))",
        "GROUP BY LOWER(TRIM(email)), auth_scope_code",
        "Resolve duplicate emails before rerunning migration.");
    TruthSuiteFileAssert.assertContainsInOrder(
        V2_MIGRATION,
        "DROP CONSTRAINT IF EXISTS app_users_email_key;",
        "SET email = LOWER(TRIM(email));",
        "GROUP BY LOWER(TRIM(email)), auth_scope_code",
        "ADD CONSTRAINT uq_app_users_email_scope UNIQUE (email, auth_scope_code);");
  }
}
