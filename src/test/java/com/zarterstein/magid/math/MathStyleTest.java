package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MathStyle}: how results print.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class MathStyleTest {

    private static MathStyle digits(int digits) {
        return MathStyle.DEFAULT.withDigits(digits);
    }

    @Test
    @DisplayName("By default an integral result prints without a fraction: 3, not 3.0")
    void integerPrintsPlain() {
        assertEquals("3", MathStyle.DEFAULT.format(3.0));
    }

    @Test
    @DisplayName("By default every digit of the double prints: 0.1 + 0.2 is 0.30000000000000004")
    void allDigits() {
        assertEquals("0.30000000000000004", MathStyle.DEFAULT.format(0.1 + 0.2));
    }

    @Test
    @DisplayName("digits rounds half away from zero on the decimal the story sees: 2.675 is 2.68")
    void digitsRoundHalfUp() {
        assertEquals("2.68", digits(2).format(2.675));
    }

    @Test
    @DisplayName("digits with trim drops trailing zeros: 2.5 prints 2.5")
    void digitsTrimmed() {
        assertEquals("2.5", digits(2).format(2.5));
    }

    @Test
    @DisplayName("digits without trim keeps them: 2.5 prints 2.50")
    void digitsNotTrimmed() {
        assertEquals("2.50", new MathStyle(2, false, MathStyle.Notation.AUTO, ".").format(2.5));
    }

    @Test
    @DisplayName("A tiny negative number rounded to zero prints 0, not -0")
    void noNegativeZero() {
        assertEquals("0", digits(2).format(-0.001));
    }

    @Test
    @DisplayName("A comma as decimal separator changes only the printed text")
    void decimalComma() {
        assertEquals("2,5", new MathStyle(-1, true, MathStyle.Notation.AUTO, ",").format(2.5));
    }

    @Test
    @DisplayName("Scientific notation: 1500 prints 1.5E3")
    void scientific() {
        assertEquals("1.5E3", new MathStyle(-1, true, MathStyle.Notation.SCIENTIFIC, ".").format(1500));
    }

    @Test
    @DisplayName("Scientific with digits rounds the mantissa and carries into the exponent: 9.96 with one decimal is 1E1")
    void scientificCarry() {
        assertEquals("1E1", new MathStyle(1, true, MathStyle.Notation.SCIENTIFIC, ".").format(9.96));
    }

    @Test
    @DisplayName("Auto notation turns scientific from 1e15 and below 1e-6")
    void autoNotation() {
        assertEquals("1E15", MathStyle.DEFAULT.format(1e15));
        assertEquals("1E-7", MathStyle.DEFAULT.format(1e-7));
        assertEquals("123456789012345", MathStyle.DEFAULT.format(123456789012345.0));
    }

    @Test
    @DisplayName("Plain notation writes every digit of a huge number")
    void plainHuge() {
        assertEquals("1000000000000000000000", new MathStyle(-1, true, MathStyle.Notation.PLAIN, ".").format(1e21));
    }

    @Test
    @DisplayName("A style reads back from the symbol it was stored in")
    void serializeRoundTrips() {
        MathStyle style = new MathStyle(3, false, MathStyle.Notation.PLAIN, ",");

        assertEquals(style, MathStyle.parse(style.serialize()));
    }

    @Test
    @DisplayName("A symbol that doesn't hold a style gives the default")
    void garbageIsDefault() {
        assertEquals(MathStyle.DEFAULT, MathStyle.parse("digits=many"));
    }
}
