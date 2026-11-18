package com.zarterstein.magid.math;

import com.zarterstein.magid.api.reveries.Reverie;

import java.util.regex.Pattern;

/**
 * Reading and writing doubles as text without losing a bit.
 * <p>
 * The <b>canonical</b> text of a double is what a receiver symbol holds ({@link #stored}): an integer below 10<sup>15</sup> as an
 * integer ({@code 3}, not {@code 3.0}), anything else as {@link Double#toString(double)}, the shortest text that
 * reads back to the same double ({@code 0.1}, {@code 1.0E-7}). {@code Double.parseDouble} of the canonical text is
 * always the original value, so a result stored by one math tag and read by another keeps every bit. What a story
 * does with it outside the pack (JEXL, other packs) may round it to a float: the promise holds inside the pack.
 * </p>
 * Input is always written with a dot: the {@code decimal} of {@link MathStyle} changes only what is printed.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Numbers {

    /** A number as a story writes it: optional sign, digits with an optional dot, optional exponent. */
    static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    /** Integers below this print without a fraction or exponent; above it {@code Double.toString} is clearer. */
    private static final double PLAIN_INTEGER_LIMIT = 1e15;

    private Numbers() {
    }

    /**
     * @param text a number as a story writes it, e.g. {@code 3}, {@code -0.25}, {@code 1.5e-3}
     * @return the double nearest to it (correctly rounded)
     * @throws MathException if the text isn't a number, or too large for a double
     */
    static double parse(String text) {
        String stripped = text == null ? "" : text.strip();
        if (!NUMBER.matcher(stripped).matches()) {
            throw new MathException("'" + stripped + "' is not a number");
        }
        double value = Double.parseDouble(stripped);
        if (Double.isInfinite(value)) {
            throw new MathException("'" + stripped + "' is too large for a double");
        }
        return value;
    }

    /**
     * @param value a finite double
     * @return its canonical text (see the class description); {@code -0.0} becomes {@code 0}
     */
    static String canonical(double value) {
        if (value == 0) return "0";
        if (value == Math.rint(value) && Math.abs(value) < PLAIN_INTEGER_LIMIT) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * What a receiver symbol gets. The engine turns numeric text written into a symbol into a float, so the full
     * value goes in as a ready-made {@link Reverie}, which the engine keeps as it is: an integer that fits an
     * {@code int} as an integer reverie (exact, and the type a story expects), any other value as a text reverie
     * holding its {@linkplain #canonical canonical text}.
     *
     * @param value a finite double
     * @return the reverie to store
     */
    static Reverie<?> stored(double value) {
        if (value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return Reverie.of((int) value);
        }
        return Reverie.of(canonical(value));
    }

    /**
     * @param value a result
     * @param what  how to name it in the message, e.g. {@code sqrt(-1)}
     * @return {@code value}
     * @throws MathException if {@code value} is NaN or infinite
     */
    static double requireFinite(double value, String what) {
        if (Double.isNaN(value)) throw new MathException(what + " is not a real number");
        if (Double.isInfinite(value)) throw new MathException(what + " is too large for a double");
        return value;
    }
}
