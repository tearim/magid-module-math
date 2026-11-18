package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Degrees}: trigonometry in degrees, exact at multiples of 30° and 45°.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class DegreesTest {

    @Test
    @DisplayName("sin 30° is exactly 0.5")
    void sin30() {
        assertEquals(0.5, Degrees.sin(30));
    }

    @Test
    @DisplayName("sin 150° is 0.5, sin 210° is -0.5, sin -30° is -0.5")
    void sinQuarters() {
        assertEquals(0.5, Degrees.sin(150));
        assertEquals(-0.5, Degrees.sin(210));
        assertEquals(-0.5, Degrees.sin(-30));
    }

    @Test
    @DisplayName("sin 180° is exactly 0, and sin 390° is sin 30°")
    void sinPeriod() {
        assertEquals(0, Degrees.sin(180));
        assertEquals(0.5, Degrees.sin(390));
    }

    @Test
    @DisplayName("sin 45° is √½ correctly rounded")
    void sin45() {
        assertEquals(StrictMath.sqrt(0.5), Degrees.sin(45));
    }

    @Test
    @DisplayName("cos 60° is exactly 0.5, cos 90° exactly 0, cos 180° exactly -1")
    void cosExact() {
        assertEquals(0.5, Degrees.cos(60));
        assertEquals(0, Degrees.cos(90));
        assertEquals(-1, Degrees.cos(180));
    }

    @Test
    @DisplayName("An angle off the table goes through StrictMath: sin 10°")
    void sinOther() {
        assertEquals(StrictMath.sin(StrictMath.toRadians(10)), Degrees.sin(10));
    }

    @Test
    @DisplayName("tan 45° is exactly 1, tan 135° exactly -1")
    void tan45() {
        assertEquals(1, Degrees.tan(45));
        assertEquals(-1, Degrees.tan(135));
    }

    @Test
    @DisplayName("tan 90° is an error, not infinity")
    void tan90() {
        assertThrows(MathException.class, () -> Degrees.tan(90));
    }

    @Test
    @DisplayName("asin 0.5 is exactly 30°, acos 0.5 exactly 60°, acos -1 exactly 180°")
    void inverseExact() {
        assertEquals(30, Degrees.asin(0.5));
        assertEquals(60, Degrees.acos(0.5));
        assertEquals(180, Degrees.acos(-1));
    }

    @Test
    @DisplayName("atan 1 is exactly 45°; atan2(1, -1) is exactly 135°")
    void atanExact() {
        assertEquals(45, Degrees.atan(1));
        assertEquals(135, Degrees.atan2(1, -1));
    }
}
