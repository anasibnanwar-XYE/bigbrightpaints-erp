package com.bigbrightpaints.erp.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.bigbrightpaints.erp.core.config.EmailProperties;

class EmailServiceSecurityTest {

  @Test
  void passwordResetLinkUsesConfiguredSafeBaseUrlAndEncodesToken() {
    EmailProperties properties = new EmailProperties();
    properties.setEnabled(true);
    properties.setSendPasswordReset(true);
    properties.setBaseUrl("https://app.bigbrightpaints.com/");
    JavaMailSender mailSender = mock(JavaMailSender.class);
    SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);

    String resetUrl =
        new EmailService(mailSender, properties, templateEngine)
            .buildPasswordResetLink("raw token+/= should encode");
    assertThat(resetUrl).startsWith("https://app.bigbrightpaints.com/reset-password?token=");
    assertThat(resetUrl).doesNotContain("raw token+/=");
    assertThat(resetUrl).contains("raw%20token%2B%2F%3D%20should%20encode");
    assertThat(resetUrl).doesNotContain("evil.test");
  }
}
