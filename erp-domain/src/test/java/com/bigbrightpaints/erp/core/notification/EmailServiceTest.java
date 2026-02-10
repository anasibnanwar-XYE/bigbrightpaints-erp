package com.bigbrightpaints.erp.core.notification;

import com.bigbrightpaints.erp.core.config.EmailProperties;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private EmailProperties emailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setFromAddress("noreply@bigbrightpaints.com");
        emailProperties.setBaseUrl("https://example.com");
        emailService = new EmailService(mailSender, emailProperties, templateEngine);
    }

    @Test
    void sendSimpleEmailThrowsWhenSmtpSendFails() {
        doThrow(new MailSendException("smtp-failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendSimpleEmail("user@example.com", "subject", "body"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> {
                    ApplicationException appEx = (ApplicationException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR);
                });
    }

    @Test
    void sendInvoiceEmailThrowsWhenSmtpSendFails() {
        doThrow(new MailSendException("smtp-failed"))
                .when(mailSender).send(any(MimeMessagePreparator.class));

        assertThatThrownBy(() -> emailService.sendInvoiceEmail(
                "dealer@example.com",
                "Dealer",
                "INV-001",
                "10 Feb 2026",
                "20 Feb 2026",
                "₹2000.00",
                "Big Bright Paints",
                "pdf".getBytes()
        ))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> {
                    ApplicationException appEx = (ApplicationException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR);
                });
    }

    @Test
    void sendSimpleEmailThrowsWhenMailDisabled() {
        emailProperties.setEnabled(false);

        assertThatThrownBy(() -> emailService.sendSimpleEmail("user@example.com", "subject", "body"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> {
                    ApplicationException appEx = (ApplicationException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_CONFIGURATION_ERROR);
                });
    }

    @Test
    void sendInvoiceEmailThrowsWhenRecipientBlank() {
        assertThatThrownBy(() -> emailService.sendInvoiceEmail(
                " ",
                "Dealer",
                "INV-001",
                "10 Feb 2026",
                "20 Feb 2026",
                "₹2000.00",
                "Big Bright Paints",
                "pdf".getBytes()
        ))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> {
                    ApplicationException appEx = (ApplicationException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
                });
    }
}
