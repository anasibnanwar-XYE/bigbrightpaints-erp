package com.bigbrightpaints.erp.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ConfigurationSecretsHardeningTest {

  @Test
  void defaultAndDevProfilesDoNotContainCommittedSmtpSecrets() throws IOException {
    String application = read("src/main/resources/application.yml");
    String applicationDev = read("src/main/resources/application-dev.yml");

    assertThat(application).contains("password: ${SPRING_MAIL_PASSWORD:}");
    assertThat(applicationDev).contains("password: ${SPRING_MAIL_PASSWORD:}");

    assertThat(application).doesNotContain("njqUYK2FytD5ZVhA");
    assertThat(applicationDev).doesNotContain("xsmtpsib-");
  }

  @Test
  void prodProfileRequiresMailCredentialsFromEnvironment() throws IOException {
    String applicationProd = read("src/main/resources/application-prod.yml");

    assertThat(applicationProd).contains("host: ${SPRING_MAIL_HOST}");
    assertThat(applicationProd).contains("username: ${SPRING_MAIL_USERNAME}");
    assertThat(applicationProd).contains("password: ${SPRING_MAIL_PASSWORD}");
  }

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path));
  }
}
