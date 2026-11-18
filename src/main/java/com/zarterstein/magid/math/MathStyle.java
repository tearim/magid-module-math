package com.zarterstein.magid.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * How the math tags print a result: set by {@code <math-print-style/>}, kept in the story's symbol
 * {@value MathState#STYLE}, so it is saved with the session. It changes only what is printed; a receiver always
 * gets the full value ({@link Numbers#canonical}).
 *
 * @param digits           decimals to print, rounded half away from zero; {@code -1} for all that the double has
 *                         (its shortest exact text)
 * @param trim             whether trailing zeros after the decimal separator are dropped ({@code 2.50} → {@code 2.5})
 * @param notation         plain digits, scientific, or plain unless the number is very small or very large
 * @param decimalSeparator {@code .} or {@code ,}
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
record MathStyle(int digits, boolean trim, Notation notation, String decimalSeparator) {

    /** All the digits the double has, trailing zeros dropped, plain unless tiny or huge, a dot. */
    static final MathStyle DEFAULT = new MathStyle(-1, true, Notation.AUTO, ".");

    /** The most decimals a style may ask for: a double has at most 17 significant digits. */
    static final int MAX_DIGITS = 17;

    /** Plain or scientific notation. */
    enum Notation {
        /** Plain, unless the absolute value is below 10<sup>-6</sup> or at least 10<sup>15</sup>. */
        AUTO,
        /** Always plain digits, however many. */
        PLAIN,
        /** Always a mantissa between 1 and 10 and an exponent: {@code 1.5E3}. */
        SCIENTIFIC;

        /**
         * @param name {@code auto}, {@code plain} or {@code scientific}, any case
         * @return the notation
         * @throws IllegalArgumentException for any other name
         */
        static Notation of(String name) {
            try {
                return valueOf(name.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("'" + name + "' is not a notation (auto, plain, scientific)");
            }
        }
    }

    /**
     * @throws IllegalArgumentException if {@code digits} is below -1 or above {@value #MAX_DIGITS}, or the
     *                                  separator isn't {@code .} or {@code ,}
     */
    MathStyle {
        if (digits < -1 || digits > MAX_DIGITS) {
            throw new IllegalArgumentException("digits must be between 0 and " + MAX_DIGITS + ", not " + digits);
        }
        if (!".".equals(decimalSeparator) && !",".equals(decimalSeparator)) {
            throw new IllegalArgumentException("the decimal separator is . or , not '" + decimalSeparator + "'");
        }
    }

    /**
     * @param newDigits the decimals to print instead
     * @return this style with other digits (a tag's own {@code digits} attribute)
     */
    MathStyle withDigits(int newDigits) {
        return new MathStyle(newDigits, trim, notation, decimalSeparator);
    }

    /**
     * @param value a finite double
     * @return the text to print
     */
    String format(double value) {
        if (value == 0) value = 0.0;   // no "-0"
        BigDecimal exact = new BigDecimal(Double.toString(value));   // the double's shortest exact decimal
        double magnitude = Math.abs(value);
        boolean scientific = notation == Notation.SCIENTIFIC
                || (notation == Notation.AUTO && value != 0 && (magnitude < 1e-6 || magnitude >= 1e15));
        String text = scientific ? scientific(exact) : plain(exact);
        if (text.startsWith("-") && text.chars().noneMatch(c -> c >= '1' && c <= '9')) {
            text = text.substring(1);   // -0.00 after rounding a tiny negative number
        }
        return ".".equals(decimalSeparator) ? text : text.replace('.', decimalSeparator.charAt(0));
    }

    private String plain(BigDecimal exact) {
        BigDecimal shown = digits >= 0 ? exact.setScale(digits, RoundingMode.HALF_UP) : exact;
        if (digits < 0 || trim) shown = stripZeros(shown);
        return shown.toPlainString();
    }

    private String scientific(BigDecimal exact) {
        if (exact.signum() == 0) return plain(exact);
        BigDecimal stripped = exact.stripTrailingZeros();
        int precision = stripped.precision();
        int exponent = precision - 1 - stripped.scale();
        BigDecimal mantissa = new BigDecimal(stripped.unscaledValue(), precision - 1);
        if (digits >= 0) {
            mantissa = mantissa.setScale(digits, RoundingMode.HALF_UP);
            if (mantissa.abs().compareTo(BigDecimal.TEN) >= 0) {   // 9.96 with one decimal is 10.0
                mantissa = mantissa.movePointLeft(1).setScale(digits, RoundingMode.HALF_UP);
                exponent++;
            }
        }
        if (digits < 0 || trim) mantissa = stripZeros(mantissa);
        return mantissa.toPlainString() + "E" + exponent;
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /**
     * @return the style as the symbol {@value MathState#STYLE} holds it, e.g.
     *         {@code digits=2;trim=false;notation=auto;decimal=,}
     */
    String serialize() {
        return "digits=" + digits + ";trim=" + trim + ";notation=" + notation.name().toLowerCase(Locale.ROOT)
                + ";decimal=" + decimalSeparator;
    }

    /**
     * @param text what {@link #serialize()} wrote
     * @return the style; {@link #DEFAULT} for text that isn't one (a story overwrote the symbol)
     */
    static MathStyle parse(String text) {
        try {
            int digits = DEFAULT.digits;
            boolean trim = DEFAULT.trim;
            Notation notation = DEFAULT.notation;
            String decimal = DEFAULT.decimalSeparator;
            for (String part : text.split(";")) {
                int equals = part.indexOf('=');
                if (equals < 0) continue;
                String key = part.substring(0, equals).strip();
                String value = part.substring(equals + 1).strip();
                switch (key) {
                    case "digits" -> digits = Integer.parseInt(value);
                    case "trim" -> trim = Boolean.parseBoolean(value);
                    case "notation" -> notation = Notation.of(value);
                    case "decimal" -> decimal = value;
                    default -> { }
                }
            }
            return new MathStyle(digits, trim, notation, decimal);
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
