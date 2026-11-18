package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Functions}: the functions whose results a story relies on.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class FunctionsTest {

    private static double call(String name, double... args) {
        return Functions.call(Functions.get(name), args);
    }

    @Test
    @DisplayName("mod takes the divisor's sign: mod(-1, 3) is 2")
    void modFloor() {
        assertEquals(2, call("mod", -1, 3));
    }

    @Test
    @DisplayName("rem takes the dividend's sign: rem(-1, 3) is -1")
    void remTruncated() {
        assertEquals(-1, call("rem", -1, 3));
    }

    @Test
    @DisplayName("Division by zero is an error")
    void divisionByZero() {
        MathException e = assertThrows(MathException.class, () -> call("div", 1, 0));
        assertTrue(e.getMessage().contains("division by zero"), e.getMessage());
    }

    @Test
    @DisplayName("round(2.675, 2) is 2.68: half away from zero on the decimal the story wrote")
    void roundDecimals() {
        assertEquals(2.68, call("round", 2.675, 2));
    }

    @Test
    @DisplayName("round(-2.5) is -3 (half away from zero); rint(2.5) is 2 (half to even)")
    void roundAndRint() {
        assertEquals(-3, call("round", -2.5));
        assertEquals(2, call("rint", 2.5));
    }

    @Test
    @DisplayName("round with fractional decimals is an error naming decimals")
    void roundBadDecimals() {
        MathException e = assertThrows(MathException.class, () -> call("round", 1, 1.5));
        assertEquals("decimals", e.attribute());
    }

    @Test
    @DisplayName("trunc cuts toward zero: trunc(-2.7) is -2")
    void trunc() {
        assertEquals(-2, call("trunc", -2.7));
    }

    @Test
    @DisplayName("log2(8) is exactly 3; log(8, 2) too; log(1000, 10) is exactly 3")
    void exactLogarithms() {
        assertEquals(3, call("log2", 8));
        assertEquals(3, call("log", 8, 2));
        assertEquals(3, call("log", 1000, 10));
    }

    @Test
    @DisplayName("ln(0) is an error")
    void lnOfZero() {
        assertThrows(MathException.class, () -> call("ln", 0));
    }

    @Test
    @DisplayName("sqrt(-1) is an error")
    void sqrtNegative() {
        assertThrows(MathException.class, () -> call("sqrt", -1));
    }

    @Test
    @DisplayName("A result that overflows is an error: pow(10, 400)")
    void overflow() {
        MathException e = assertThrows(MathException.class, () -> call("pow", 10, 400));
        assertTrue(e.getMessage().contains("too large"), e.getMessage());
    }

    @Test
    @DisplayName("asin(2) is an error")
    void asinOutOfRange() {
        assertThrows(MathException.class, () -> call("asin", 2));
    }

    @Test
    @DisplayName("clamp keeps a value within min and max; min above max is an error")
    void clamp() {
        assertEquals(10, call("clamp", 12, 0, 10));
        assertThrows(MathException.class, () -> call("clamp", 1, 5, 2));
    }

    @Test
    @DisplayName("tau is exactly twice pi")
    void tau() {
        assertEquals(2 * Math.PI, call("tau"));
    }

    @Test
    @DisplayName("A signature shows optional parameters in brackets")
    void signature() {
        assertEquals("round(x[, decimals])", Functions.get("round").signature());
        assertEquals("mean(values…)", Functions.get("mean").signature());
    }
}
