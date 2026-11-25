package com.tiet.banking.util;

import com.tiet.banking.exception.ValidationException;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private InputValidator() {
    }

    public static String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        return value.trim();
    }

    public static String normalizeEmail(String rawEmail) {
        String email = requireNonBlank(rawEmail, "Email").toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        return email;
    }

    public static void requireValidPassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
    }

    public static String normalizeAccountNumber(String rawAccountNumber) {
        return requireNonBlank(rawAccountNumber, "Account number").toUpperCase();
    }

    public static int requireValidLimit(int limit) {
        if (limit <= 0 || limit > 500) {
            throw new ValidationException("Limit must be between 1 and 500.");
        }
        return limit;
    }
}
