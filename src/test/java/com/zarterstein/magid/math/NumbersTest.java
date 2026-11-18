package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Numbers}: reading numbers, and the canonical text a receiver holds.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class NumbersTest {

    @Test
    @DisplayName("An integer, a decimal, a leading dot and an exponent are numbers")
    void numberForms() {
        assertEquals(3, Numbers.parse("3"));
        assertEquals(-0.25, Numbers.parse("-0.25"));
        assertEquals(0.5, Numbers.parse(".5"));
        assertEquals(0.0015, Numbers.parse("1.5e-3"));
    }

    @Test
    @DisplayName("A comma is not a decimal separator in input")
    void commaIsNotDecimal() {
        assertThrows(MathException.class, () -> Numbers.parse("1,5"));
    }

    @Test
    @DisplayName("Text is not a number")
    void textIsNotNumber() {
        assertThrows(MathException.class, () -> Numbers.parse("three"));
    }

    @Test
    @DisplayName("A number beyond the double range is refused, not turned into infinity")
    void tooLarge() {
        MathException e = assertThrows(MathException.class, () -> Numbers.parse("1e400"));
        assertTrue(e.getMessage().contains("too large"), e.getMessage());
    }

    @Test
    @DisplayName("An integral double is written as an integer")
    void integerCanonical() {
        assertEquals("3", Numbers.canonical(3.0));
        assertEquals("-42", Numbers.canonical(-42.0));
    }

    @Test
    @DisplayName("Negative zero is written as 0")
    void negativeZero() {
        assertEquals("0", Numbers.canonical(-0.0));
    }

    @Test
    @DisplayName("0.1 + 0.2 is written with every digit it has, and reads back to the same bits")
    void canonicalRoundTrips() {
        double sum = 0.1 + 0.2;
        String text = Numbers.canonical(sum);

        assertEquals("0.30000000000000004", text);
        assertEquals(Double.doubleToLongBits(sum), Double.doubleToLongBits(Numbers.parse(text)));
    }

    @Test
    @DisplayName("Very small and very large values keep their exponent and read back exactly")
    void exponentRoundTrips() {
        for (double value : new double[]{1e-7, 1.2345678901234567e20, Double.MIN_VALUE, Double.MAX_VALUE}) {
            assertEquals(value, Numbers.parse(Numbers.canonical(value)), Numbers.canonical(value));
        }
    }

    @Test
    @DisplayName("requireFinite names NaN as not a real number")
    void nanIsNotReal() {
        MathException e = assertThrows(MathException.class, () -> Numbers.requireFinite(Double.NaN, "sqrt(-1)"));
        assertEquals("sqrt(-1) is not a real number", e.getMessage());
    }
}
