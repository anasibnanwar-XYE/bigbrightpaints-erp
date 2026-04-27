package com.bigbrightpaints.erp.modules.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.company.domain.Company;

class UserAccountTest {

  @Test
  void prePersist_populatesMissingIdentityFields() {
    UserAccount user = new UserAccount();

    user.prePersist();

    assertThat(user.getPublicId()).isNotNull();
    assertThat(user.getCreatedAt()).isNotNull();
  }

  @Test
  void prePersist_preservesExistingIdentityFields() {
    UserAccount user = new UserAccount("user@example.com", "MOCK", "hash", "User");
    UUID publicId = user.getPublicId();
    Instant createdAt = user.getCreatedAt();

    user.prePersist();

    assertThat(user.getPublicId()).isEqualTo(publicId);
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  void belongsToCompanyCode_requiresBoundCompanyAndCode() {
    UserAccount user = new UserAccount("user@example.com", "MOCK", "hash", "User");

    assertThat(user.belongsToCompanyCode(null)).isFalse();
    assertThat(user.belongsToCompanyCode(" ")).isFalse();

    Company company = new Company();
    company.setCode("ACME");
    user.setCompany(company);

    assertThat(user.belongsToCompanyCode("acme")).isTrue();

    company.setCode(null);
    assertThat(user.belongsToCompanyCode("acme")).isFalse();
  }

  @Test
  void setAuthScopeCode_normalizesWhitespaceAndCase() {
    UserAccount user = new UserAccount("user@example.com", "MOCK", "hash", "User");

    user.setAuthScopeCode(" acme ");

    assertThat(user.getAuthScopeCode()).isEqualTo("ACME");
  }
}
