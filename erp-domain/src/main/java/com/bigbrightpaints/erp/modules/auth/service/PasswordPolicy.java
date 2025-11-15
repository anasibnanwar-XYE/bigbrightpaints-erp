package com.bigbrightpaints.erp.modules.auth.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordPolicy {

    // Basic policy: length >= 10, includes upper, lower, digit, symbol, no spaces
    public List<String> validate(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null) {
            violations.add("Password is required");
            return violations;
        }
        if (password.length() < 10) {
            violations.add("Must be at least 10 characters long");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            violations.add("Must include a lowercase letter");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            violations.add("Must include an uppercase letter");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            violations.add("Must include a digit");
        }
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
            violations.add("Must include a special character");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            violations.add("Must not contain whitespace");
        }
        return violations;
    }
}

