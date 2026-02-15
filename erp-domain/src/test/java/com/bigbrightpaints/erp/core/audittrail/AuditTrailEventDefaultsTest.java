package com.bigbrightpaints.erp.core.audittrail;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditTrailEventDefaultsTest {

    @Test
    void auditActionEvent_onCreate_setsDefaults() {
        AuditActionEvent event = new AuditActionEvent();
        event.onCreate();

        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(AuditActionEventStatus.SUCCESS);
        assertThat(event.getSource()).isEqualTo(AuditActionEventSource.SYSTEM);
    }

    @Test
    void auditActionEvent_onCreate_keepsExplicitValues() {
        Instant occurredAt = Instant.parse("2026-02-15T10:00:00Z");
        Instant createdAt = Instant.parse("2026-02-15T10:05:00Z");
        AuditActionEvent event = new AuditActionEvent();
        event.setOccurredAt(occurredAt);
        setField(event, "createdAt", createdAt);
        event.setStatus(AuditActionEventStatus.FAILURE);
        event.setSource(AuditActionEventSource.BACKEND);

        event.onCreate();

        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getStatus()).isEqualTo(AuditActionEventStatus.FAILURE);
        assertThat(event.getSource()).isEqualTo(AuditActionEventSource.BACKEND);
    }

    @Test
    void mlInteractionEvent_onCreate_setsDefaults() {
        MlInteractionEvent event = new MlInteractionEvent();
        event.onCreate();

        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(AuditActionEventStatus.SUCCESS);
    }

    @Test
    void mlInteractionEvent_onCreate_keepsExplicitValues() {
        Instant occurredAt = Instant.parse("2026-02-15T11:00:00Z");
        Instant createdAt = Instant.parse("2026-02-15T11:05:00Z");
        MlInteractionEvent event = new MlInteractionEvent();
        event.setOccurredAt(occurredAt);
        setField(event, "createdAt", createdAt);
        event.setStatus(AuditActionEventStatus.INFO);

        event.onCreate();

        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getStatus()).isEqualTo(AuditActionEventStatus.INFO);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
