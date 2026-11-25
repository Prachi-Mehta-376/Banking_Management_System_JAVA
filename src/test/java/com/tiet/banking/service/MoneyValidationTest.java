package com.tiet.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyValidationTest {

    @Test
    void parsesValidPositiveAmountsIntoExactPaise() {
        assertEquals(50_000L, Money.parsePositive("500").paise());
        assertEquals(123_45L, Money.parsePositive("123.45").paise());
        assertEquals(100_050L, Money.parsePositive("1,000.50").paise());
        assertEquals(1L, Money.parsePositive("0.01").paise());
        assertEquals(new BigDecimal("123.45"), Money.parsePositive("123.45").toBigDecimal());
    }

    @Test
    void rejectsZeroNegativeMoreThanTwoDecimalsAndInvalidText() {
        assertThrows(ValidationException.class, () -> Money.parsePositive(null));
        assertThrows(ValidationException.class, () -> Money.parsePositive("  "));
        assertThrows(ValidationException.class, () -> Money.parsePositive("0"));
        assertThrows(ValidationException.class, () -> Money.parsePositive("0.00"));
        assertThrows(ValidationException.class, () -> Money.parsePositive("-50.00"));
        assertThrows(ValidationException.class, () -> Money.parsePositive("10.123"));
        assertThrows(ValidationException.class, () -> Money.parsePositive("abc"));
        assertThrows(ValidationException.class, () -> Money.fromBigDecimalPositive(new BigDecimal("-0.01")));
    }

    @Test
    void protectsAgainstLongArithmeticOverflowAndUnderflow() {
        Money max = Money.ofPaise(Long.MAX_VALUE);
        Money onePaise = Money.ofPaise(1L);
        assertThrows(ValidationException.class, () -> max.add(onePaise));
        assertThrows(ValidationException.class, () -> Money.ofPaise(Long.MIN_VALUE).subtract(onePaise));
        assertThrows(ValidationException.class, () -> Money.parsePositive("999999999999999999.99"));
        assertThrows(ValidationException.class, () -> Money.ofNonNegativePaise(-1L));
    }
}
