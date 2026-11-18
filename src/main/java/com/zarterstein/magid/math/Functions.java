package com.zarterstein.magid.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every function of the pack, by name: what {@code <math-calc>} calls ({@code sqrt(2)}) and what the function tags
 * are ({@code <math-sqrt x="2"/>}). A function's parameters are its tag's attributes; an aggregate takes a list,
 * {@code values="1, 2, $x"}.
 * <p>
 * The results are those of {@link StrictMath} (the same bits on every machine) or of IEEE 754 arithmetic, which is
 * correctly rounded; a result that is NaN or infinite is an error, never a value.
 * </p>
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Functions {

    /** What a function computes from its arguments. */
    @FunctionalInterface
    interface Body {
        /**
         * @param args the arguments, as many as the function takes
         * @return the result, before the finiteness check
         */
        double apply(double[] args);
    }

    /**
     * A function.
     *
     * @param name      its name, also the local name of its tag
     * @param params    its parameters in order, also its tag's attributes; for an aggregate the single
     *                  {@value #VALUES}
     * @param required  how many of {@code params} must be given (the rest are optional)
     * @param aggregate whether it takes a list of any length
     * @param body      what it computes
     */
    record MathFunction(String name, List<String> params, int required, boolean aggregate, Body body) {

        /**
         * @param count a number of arguments
         * @return whether the function takes that many
         */
        boolean takes(int count) {
            return aggregate || (count >= required && count <= params.size());
        }

        /** @return how the function is called, e.g. {@code round(x[, decimals])} or {@code mean(values…)} */
        String signature() {
            if (aggregate) return name + "(values…)";
            StringBuilder text = new StringBuilder(name).append('(');
            for (int i = 0; i < params.size(); i++) {
                if (i == required) text.append('[');
                if (i > 0) text.append(", ");
                text.append(params.get(i));
            }
            if (params.size() > required) text.append(']');
            return text.append(')').toString();
        }
    }

    /** The attribute of an aggregate's tag: a comma-separated list. */
    static final String VALUES = "values";

    private static final Map<String, MathFunction> ALL = new LinkedHashMap<>();

    static {
        // constants
        constant("pi", Math.PI);
        constant("e", Math.E);
        constant("tau", 2 * Math.PI);   // exact doubling

        // arithmetic
        binary("add", (a, b) -> a + b);
        binary("sub", (a, b) -> a - b);
        binary("mul", (a, b) -> a * b);
        binary("div", (a, b) -> divide(a, b, "div"));
        binary("mod", Functions::floorMod);
        binary("rem", (a, b) -> {
            requireNonZero(b, "rem");
            return a % b;
        });
        binary("pow", StrictMath::pow);
        binary("hypot", StrictMath::hypot);
        unary("abs", Math::abs);
        unary("sign", Math::signum);

        // rounding
        unary("floor", Math::floor);
        unary("ceil", Math::ceil);
        unary("trunc", x -> x < 0 ? Math.ceil(x) : Math.floor(x));
        unary("rint", Math::rint);
        add("round", List.of("x", "decimals"), 1, false, args -> round(args[0], args.length > 1 ? args[1] : 0));
        add("clamp", List.of("x", "min", "max"), 3, false, args -> clamp(args[0], args[1], args[2]));

        // powers and logarithms
        unary("sqrt", x -> {
            if (x < 0) throw new MathException("sqrt(" + Numbers.canonical(x) + "): the root of a negative number");
            return StrictMath.sqrt(x);
        });
        unary("cbrt", StrictMath::cbrt);
        unary("exp", StrictMath::exp);
        unary("ln", x -> StrictMath.log(requirePositive(x, "ln")));
        unary("log10", x -> StrictMath.log10(requirePositive(x, "log10")));
        unary("log2", x -> log2(requirePositive(x, "log2")));
        add("log", List.of("x", "base"), 2, false, args -> log(args[0], args[1]));

        // trigonometry, radians
        unary("sin", StrictMath::sin);
        unary("cos", StrictMath::cos);
        unary("tan", StrictMath::tan);
        unary("asin", x -> StrictMath.asin(requireUnit(x, "asin")));
        unary("acos", x -> StrictMath.acos(requireUnit(x, "acos")));
        unary("atan", StrictMath::atan);
        add("atan2", List.of("y", "x"), 2, false, args -> StrictMath.atan2(args[0], args[1]));
        unary("sinh", StrictMath::sinh);
        unary("cosh", StrictMath::cosh);
        unary("tanh", StrictMath::tanh);

        // trigonometry, degrees (exact at multiples of 30° and 45°)
        unary("sind", Degrees::sin);
        unary("cosd", Degrees::cos);
        unary("tand", Degrees::tan);
        unary("asind", x -> Degrees.asin(requireUnit(x, "asind")));
        unary("acosd", x -> Degrees.acos(requireUnit(x, "acosd")));
        unary("atand", Degrees::atan);
        add("atan2d", List.of("y", "x"), 2, false, args -> Degrees.atan2(args[0], args[1]));
        unary("degrees", StrictMath::toDegrees);
        unary("radians", StrictMath::toRadians);

        // aggregates
        aggregate("sum", Stats::sum);
        aggregate("product", Stats::product);
        aggregate("count", values -> values.length);
        aggregate("mean", Stats::mean);
        aggregate("median", Stats::median);
        aggregate("min", Stats::min);
        aggregate("max", Stats::max);
        aggregate("varp", values -> Stats.variance(values, false));
        aggregate("vars", values -> Stats.variance(values, true));
        aggregate("stdevp", values -> StrictMath.sqrt(Stats.variance(values, false)));
        aggregate("stdevs", values -> StrictMath.sqrt(Stats.variance(values, true)));
    }

    private Functions() {
    }

    /**
     * @param name a function's name
     * @return the function; {@code null} if there is none of that name
     */
    static MathFunction get(String name) {
        return ALL.get(name);
    }

    /**
     * @return every function, in the order of the documentation
     */
    static Collection<MathFunction> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    /**
     * Calls a function and checks its result.
     *
     * @param function the function
     * @param args     its arguments; {@link MathFunction#takes} must accept their number
     * @return the result
     * @throws MathException if the function can't compute it, or the result isn't a finite number
     */
    static double call(MathFunction function, double[] args) {
        double result = function.body().apply(args);
        return Numbers.requireFinite(result, function.name() + "(" + Arrays.stream(args)
                .mapToObj(Numbers::canonical).collect(Collectors.joining(", ")) + ")");
    }

    // ---- registration ----

    private static void add(String name, List<String> params, int required, boolean aggregate, Body body) {
        ALL.put(name, new MathFunction(name, params, required, aggregate, body));
    }

    private static void constant(String name, double value) {
        add(name, List.of(), 0, false, args -> value);
    }

    private static void unary(String name, java.util.function.DoubleUnaryOperator operator) {
        add(name, List.of("x"), 1, false, args -> operator.applyAsDouble(args[0]));
    }

    private static void binary(String name, java.util.function.DoubleBinaryOperator operator) {
        add(name, List.of("a", "b"), 2, false, args -> operator.applyAsDouble(args[0], args[1]));
    }

    private static void aggregate(String name, Body body) {
        add(name, List.of(VALUES), 0, true, body);
    }

    // ---- the functions that need more than one line ----

    static double divide(double a, double b, String name) {
        requireNonZero(b, name);
        return a / b;
    }

    /** The remainder with the divisor's sign: {@code mod(-1, 3)} is 2 (where {@code rem} gives -1). */
    static double floorMod(double a, double b) {
        requireNonZero(b, "mod");
        double remainder = a % b;   // exact
        if (remainder != 0 && (remainder < 0) != (b < 0)) remainder += b;
        return remainder;
    }

    /**
     * Rounds half away from zero, on the decimal a story sees: {@code round(2.675, 2)} is {@code 2.68}, although the
     * double nearest 2.675 lies a little below it. {@code rint} rounds half to even instead.
     */
    static double round(double x, double decimals) {
        if (decimals != Math.rint(decimals) || Math.abs(decimals) > 20) {
            throw new MathException("round: decimals must be a whole number from -20 to 20, not "
                    + Numbers.canonical(decimals), "decimals");
        }
        return new BigDecimal(Double.toString(x)).setScale((int) decimals, RoundingMode.HALF_UP).doubleValue();
    }

    static double clamp(double x, double min, double max) {
        if (min > max) {
            throw new MathException("clamp: min " + Numbers.canonical(min) + " is above max " + Numbers.canonical(max));
        }
        return Math.max(min, Math.min(max, x));
    }

    /** The binary logarithm, exact for powers of two. */
    static double log2(double x) {
        long bits = Double.doubleToRawLongBits(x);
        boolean powerOfTwo = (bits & 0x000F_FFFF_FFFF_FFFFL) == 0 && Math.getExponent(x) >= Double.MIN_EXPONENT;
        return powerOfTwo ? Math.getExponent(x) : StrictMath.log(x) / StrictMath.log(2);
    }

    static double log(double x, double base) {
        requirePositive(x, "log");
        if (base <= 0 || base == 1) {
            throw new MathException("log: the base must be positive and not 1, not " + Numbers.canonical(base), "base");
        }
        if (base == 2) return log2(x);
        if (base == 10) return StrictMath.log10(x);   // exact for powers of ten
        return StrictMath.log(x) / StrictMath.log(base);
    }

    private static void requireNonZero(double divisor, String name) {
        if (divisor == 0) throw new MathException(("/".equals(name) ? "" : name + ": ") + "division by zero");
    }

    private static double requirePositive(double x, String name) {
        if (x <= 0) {
            throw new MathException(name + "(" + Numbers.canonical(x) + "): the logarithm of a number that isn't positive");
        }
        return x;
    }

    private static double requireUnit(double x, String name) {
        if (x < -1 || x > 1) throw new MathException(name + "(" + Numbers.canonical(x) + "): outside [-1, 1]");
        return x;
    }
}
