package com.bigbrightpaints.erp.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesSecurityTest {

    @Test
    void validate_failsWhenSecretMissingInNonSafeRuntimeProfiles() {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles("prod"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be provided");
    }

    @Test
    void validate_failsWhenUnsafeStaticSecretIsUsedInNonSafeRuntimeProfiles() {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles("prod"));
        properties.setSecret("mock-super-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe static placeholder");
    }

    @Test
    void validate_failsWhenSafeAndNonSafeProfilesAreMixed() {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles("dev", "prod"));
        properties.setSecret("dev-only-jwt-secret-please-override-32");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe static placeholder");
    }

    @Test
    void validate_generatesEphemeralSecretForSafeProfilesWhenMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles("dev"));
        properties.setSecret("   ");

        properties.validate();

        assertThat(properties.getSecret()).isNotBlank();
        assertThat(properties.getSecret().getBytes(StandardCharsets.UTF_8).length).isGreaterThanOrEqualTo(32);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "mock", "openapi", "benchmark"})
    void validate_rejectsStaticPlaceholdersOutsideTestProfiles(String profile) {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles(profile));
        properties.setSecret("test-secret-should-be-at-least-32-bytes-long-1234");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe static placeholder");
    }

    @Test
    void validate_allowsStaticPlaceholderOnlyInTestProfile() {
        JwtProperties properties = new JwtProperties();
        properties.setEnvironment(environmentWithProfiles("test"));
        properties.setSecret("test-secret-should-be-at-least-32-bytes-long-1234");

        properties.validate();

        assertThat(properties.getSecret()).isEqualTo("test-secret-should-be-at-least-32-bytes-long-1234");
    }

    private MockEnvironment environmentWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }
}
