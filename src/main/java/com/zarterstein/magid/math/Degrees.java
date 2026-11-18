package com.zarterstein.magid.math;

/**
 * Trigonometry in degrees, exact where a story expects it: {@code sind(30)} is {@code 0.5}, {@code sind(180)} is
 * {@code 0}, {@code cosd(60)} is {@code 0.5} and {@code tand(45)} is {@code 1}. Converting to radians first can't do
 * that ({@code sin(toRadians(30))} is {@code 0.49999999999999994}), because π has no exact double.
 * <p>
 * The angle is reduced exactly: {@code deg % 360} and the steps to a quarter are exact in binary floating point. On
 * a quarter, multiples of 30° and 45° take their exact values ({@code 0}, {@code 0.5}, the correctly rounded √½ and
 * √3/2, {@code 1}); every other angle goes through {@link StrictMath} in radians. The inverse functions return exact
 * degrees for those same values.
 * </p>
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Degrees {

    /** √½, correctly rounded: sin 45°. */
    static final double SQRT_HALF = StrictMath.sqrt(0.5);
    /** √3/2, correctly rounded (the halving is exact): sin 60°. */
    static final double SQRT3_HALF = StrictMath.sqrt(3) / 2;

    private Degrees() {
    }

    /**
     * @param degrees an angle in degrees, finite
     * @return its sine
     */
    static double sin(double degrees) {
        double angle = reduce(degrees);
        int quarter = (int) (angle / 90);
        double rest = angle - 90 * quarter;   // exact (Sterbenz)
        double sin = switch (quarter) {
            case 0 -> sinQuarter(rest);
            case 1 -> sinQuarter(90 - rest);
            case 2 -> -sinQuarter(rest);
            default -> -sinQuarter(90 - rest);
        };
        return sin + 0.0;   // sin 180° is 0, not -0
    }

    /**
     * @param degrees an angle in degrees, finite
     * @return its cosine
     */
    static double cos(double degrees) {
        double angle = reduce(degrees);
        int quarter = (int) (angle / 90);
        double rest = angle - 90 * quarter;
        double cos = switch (quarter) {
            case 0 -> sinQuarter(90 - rest);
            case 1 -> -sinQuarter(rest);
            case 2 -> -sinQuarter(90 - rest);
            default -> sinQuarter(rest);
        };
        return cos + 0.0;   // cos 90° is 0, not -0
    }

    /**
     * @param degrees an angle in degrees, finite
     * @return its tangent
     * @throws MathException at 90°, 270° and their repeats, where the tangent is infinite
     */
    static double tan(double degrees) {
        double cos = cos(degrees);
        if (cos == 0) throw new MathException("tand(" + Numbers.canonical(degrees) + ") is infinite");
        double sin = sin(degrees);
        if (Math.abs(sin) == Math.abs(cos)) return Math.signum(sin) * Math.signum(cos);   // exactly ±1
        return sin / cos;
    }

    /**
     * @param x a sine, in [-1, 1]
     * @return its angle in degrees, in [-90, 90]
     */
    static double asin(double x) {
        double exact = asinExact(Math.abs(x));
        if (!Double.isNaN(exact)) return Math.copySign(exact, x);
        return StrictMath.toDegrees(StrictMath.asin(x));
    }

    /**
     * @param x a cosine, in [-1, 1]
     * @return its angle in degrees, in [0, 180]
     */
    static double acos(double x) {
        double exact = asinExact(Math.abs(x));
        if (!Double.isNaN(exact)) return x >= 0 ? 90 - exact : 90 + exact;   // acos = 90 - asin
        return StrictMath.toDegrees(StrictMath.acos(x));
    }

    /**
     * @param x a tangent
     * @return its angle in degrees, in (-90, 90)
     */
    static double atan(double x) {
        if (x == 0) return x;
        if (Math.abs(x) == 1) return Math.copySign(45, x);
        return StrictMath.toDegrees(StrictMath.atan(x));
    }

    /**
     * @param y the y coordinate
     * @param x the x coordinate
     * @return the angle of the point (x, y) in degrees, in (-180, 180]
     */
    static double atan2(double y, double x) {
        if (y == 0 && x == 0) return 0;
        if (y == 0) return x > 0 ? 0 : 180;
        if (x == 0) return y > 0 ? 90 : -90;
        if (Math.abs(y) == Math.abs(x)) return x > 0 ? Math.copySign(45, y) : Math.copySign(135, y);
        return StrictMath.toDegrees(StrictMath.atan2(y, x));
    }

    /** The angle in [0, 360), exactly. */
    private static double reduce(double degrees) {
        double angle = degrees % 360;   // exact
        if (angle < 0) angle += 360;
        return angle >= 360 ? 0 : angle;   // -1e-20 + 360 rounds to 360
    }

    /** The sine of an angle in [0, 90]. */
    private static double sinQuarter(double degrees) {
        if (degrees == 0) return 0;
        if (degrees == 30) return 0.5;
        if (degrees == 45) return SQRT_HALF;
        if (degrees == 60) return SQRT3_HALF;
        if (degrees == 90) return 1;
        return StrictMath.sin(StrictMath.toRadians(degrees));
    }

    /** The exact angle of a sine in {0, ½, √½, √3/2, 1}; NaN for any other. */
    private static double asinExact(double x) {
        if (x == 0) return 0;
        if (x == 0.5) return 30;
        if (x == SQRT_HALF) return 45;
        if (x == SQRT3_HALF) return 60;
        if (x == 1) return 90;
        return Double.NaN;
    }
}
