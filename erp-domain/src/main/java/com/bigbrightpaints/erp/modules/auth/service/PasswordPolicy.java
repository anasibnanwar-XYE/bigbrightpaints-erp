package com.bigbrightpaints.erp.modules.auth.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

  private static final int MIN_CODE_POINTS = 10;
  private static final int MAX_CODE_POINTS = 128;

  public List<String> validate(String password) {
    List<String> violations = new ArrayList<>();
    if (password == null) {
      violations.add("Password is required");
      return violations;
    }
    String normalized = normalize(password);
    int codePointCount = normalized.codePointCount(0, normalized.length());
    if (codePointCount < MIN_CODE_POINTS) {
      violations.add("Must be at least 10 characters long");
    }
    if (codePointCount > MAX_CODE_POINTS) {
      violations.add("Must be at most 128 characters long");
    }
    if (normalized.codePoints().noneMatch(Character::isLowerCase)) {
      violations.add("Must include a lowercase letter");
    }
    if (normalized.codePoints().noneMatch(Character::isUpperCase)) {
      violations.add("Must include an uppercase letter");
    }
    if (normalized.codePoints().noneMatch(Character::isDigit)) {
      violations.add("Must include a digit");
    }
    if (normalized.codePoints().noneMatch(c -> !Character.isLetterOrDigit(c))) {
      violations.add("Must include a special character");
    }
    if (normalized.codePoints().anyMatch(Character::isWhitespace)) {
      violations.add("Must not contain whitespace");
    }
    return violations;
  }

  public String normalize(String password) {
    if (password == null) {
      return null;
    }
    return Normalizer.normalize(password, Normalizer.Form.NFC);
  }
}
