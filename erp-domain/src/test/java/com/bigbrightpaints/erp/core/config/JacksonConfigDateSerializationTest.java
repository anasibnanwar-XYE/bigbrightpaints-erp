package com.bigbrightpaints.erp.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Tag("critical")
class JacksonConfigDateSerializationTest {

  @Test
  void objectMapper_serializesJavaTimeValuesAsIso8601Strings() throws Exception {
    ObjectMapper mapper = new JacksonConfig().objectMapper();

    String payload =
        mapper.writeValueAsString(
            Map.of(
                "today", LocalDate.of(2026, 4, 5),
                "now", Instant.parse("2026-04-05T10:30:00Z"),
                "period", YearMonth.of(2026, 4)));

    assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    assertThat(payload).contains("\"today\":\"2026-04-05\"");
    assertThat(payload).contains("\"now\":\"2026-04-05T10:30:00Z\"");
    assertThat(payload).contains("\"period\":\"2026-04\"");
  }
}
