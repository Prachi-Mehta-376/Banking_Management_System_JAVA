package com.tiet.banking.model;

import com.tiet.banking.exception.ValidationException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public final class Money {
    private static final Locale INDIA_LOCALE = new Locale("en", "IN");

    private final long paise;

    private Money(long paise) {
        this.paise = paise;
    }

    public static Money parsePositive(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ValidationException("Amount is required.");
        }
        String normalized = rawInput.trim().replace(",", "");
        BigDecimal decimal;
        try {
            decimal = new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Enter a valid numeric amount.");
        }
        return fromBigDecimalPositive(decimal);
    }

    public static Money fromBigDecimalPositive(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("Amount is required.");
        }
        if (amount.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        // Reject more than two fractional digits instead of silently rounding user money input.
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > 2) {
            throw new ValidationException("Amount cannot have more than two decimal places.");
        }
        try {
            long paise = amount.movePointRight(2).longValueExact();
            if (paise <= 0) {
                throw new ValidationException("Amount must be greater than zero.");
            }
            return new Money(paise);
        } catch (ArithmeticException ex) {
            throw new ValidationException("Amount exceeds supported monetary range.");
        }
    }

    public static Money ofPaise(long paise) {
        return new Money(paise);
    }

    public static Money ofNonNegativePaise(long paise) {
        if (paise < 0) {
            throw new ValidationException("Monetary balance cannot be negative.");
        }
        return new Money(paise);
    }

    public long paise() {
        return paise;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other");
        try {
            return new Money(Math.addExact(this.paise, other.paise));
        } catch (ArithmeticException ex) {
            throw new ValidationException("Monetary addition overflows supported range.");
        }
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other");
        try {
            return new Money(Math.subtractExact(this.paise, other.paise));
        } catch (ArithmeticException ex) {
            throw new ValidationException("Monetary subtraction overflows supported range.");
        }
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(paise, 2);
    }

    public String toPlainString() {
        return toBigDecimal().toPlainString();
    }

    public static String formatPlain(long paise) {
        return BigDecimal.valueOf(paise, 2).toPlainString();
    }

    public static String formatInr(long paise) {
        NumberFormat fmt = NumberFormat.getNumberInstance(INDIA_LOCALE);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        return "₹" + fmt.format(BigDecimal.valueOf(paise, 2));
    }

    public static String formatSignedInr(long deltaPaise) {
        if (deltaPaise < 0) {
            long abs = deltaPaise == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(deltaPaise);
            return "-" + formatInr(abs);
        }
        return "+" + formatInr(deltaPaise);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Money other && this.paise == other.paise;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(paise);
    }

    @Override
    public String toString() {
        return formatInr(paise);
    }
}
