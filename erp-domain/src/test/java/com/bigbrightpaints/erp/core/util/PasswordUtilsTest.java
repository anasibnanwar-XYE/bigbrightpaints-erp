package com.bigbrightpaints.erp.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordUtilsTest {

    @Test
    void generateTemporaryPassword_lengthZero_isEmpty() {
        assertThat(PasswordUtils.generateTemporaryPassword(0)).isEmpty();
    }

    @Test
    void generateTemporaryPassword_lengthOne_isAllowedChar() {
        String value = PasswordUtils.generateTemporaryPassword(1);
        assertThat(value).hasSize(1);
        assertThat(isAllowed(value.charAt(0))).isTrue();
    }

    @Test
    void generateTemporaryPassword_length16_correctLength() {
        assertThat(PasswordUtils.generateTemporaryPassword(16)).hasSize(16);
    }

    @Test
    void generateTemporaryPassword_length64_correctLength() {
        assertThat(PasswordUtils.generateTemporaryPassword(64)).hasSize(64);
    }

    @Test
    void generateTemporaryPassword_onlyAllowedChars() {
        String value = PasswordUtils.generateTemporaryPassword(32);
        for (char c : value.toCharArray()) {
            assertThat(isAllowed(c)).isTrue();
        }
    }

    private boolean isAllowed(char c) {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*".indexOf(c) >= 0;
    }
}
