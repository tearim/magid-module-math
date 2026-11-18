package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.SemanticType;

/**
 * Every {@linkplain Functions function} as a tag of its own, its parameters as attributes:
 * <pre>{@code
 * <math-sqrt x="2"/>                          1.4142135623730951
 * <math-round x="2.675" decimals="2"/>        2.68
 * <math-div a="{player.gold}" b="3" receiver="$share"/>
 * <math-sind x="30"/>                         0.5
 * <math-mean values="1, 3, {x}, $scores"/>
 * <math-pi digits="4"/>                       3.1416
 * }</pre>
 * Each attribute is a formula ({@link Calc}): {@code x="$angle * 2"} works. An aggregate takes
 * {@value Functions#VALUES}, a comma-separated list, where a symbol holding a list spreads out. Also
 * {@value #RECEIVER}, {@value #PRINT} and {@value #DIGITS} ({@link AbstractMathTag}).
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = FunctionTag.NAMES, types = {SemanticType.TEXT_GENERATOR, SemanticType.DYNAMIC_GENERATOR})
public class FunctionTag extends AbstractMathTag {

    /** The tag names: every function's name ({@code FunctionTagTest} keeps the two lists equal). */
    static final String NAMES = "pi, e, tau, add, sub, mul, div, mod, rem, pow, hypot, abs, sign, floor, ceil, trunc, "
            + "rint, round, clamp, sqrt, cbrt, exp, ln, log10, log2, log, sin, cos, tan, asin, acos, atan, atan2, "
            + "sinh, cosh, tanh, sind, cosd, tand, asind, acosd, atand, atan2d, degrees, radians, sum, product, "
            + "count, mean, median, min, max, varp, vars, stdevp, stdevs";

    @Override
    protected double compute() {
        Functions.MathFunction function = Functions.get(functionName());
        if (function == null) throw new MathException("no such function: " + element.name());
        if (function.aggregate()) return call(function, numbers(Functions.VALUES));
        int count = 0;
        while (count < function.params().size() && element.hasAttribute(function.params().get(count))) count++;
        for (int i = 0; i < function.params().size(); i++) {
            String param = function.params().get(i);
            if (i >= count && element.hasAttribute(param)) {
                throw new MathException(param + " needs " + function.params().get(count) + " too: "
                        + function.signature(), function.params().get(count));
            }
            if (i < function.required() && !element.hasAttribute(param)) {
                throw new MathException("the attribute " + param + " is missing: " + function.signature(), param);
            }
        }
        double[] args = new double[count];
        for (int i = 0; i < count; i++) args[i] = number(function.params().get(i));
        return call(function, args);
    }

    /** Calls the function; its message drops the function's name, which the tag's name already says. */
    private static double call(Functions.MathFunction function, double[] args) {
        try {
            return Functions.call(function, args);
        } catch (MathException e) {
            String prefix = function.name() + ": ";
            if (!e.getMessage().startsWith(prefix)) throw e;
            throw new MathException(e.getMessage().substring(prefix.length()), e.attribute());
        }
    }

    /**
     * The function's name: the tag's local name. Function names have no hyphen, so the part after the last hyphen
     * is the local name whatever prefix the story uses ({@code math-sqrt}, {@code m-sqrt}, or {@code sqrt} with
     * {@code as="none"}), also where the engine's {@code localName()} still returns the written name.
     */
    private String functionName() {
        String name = element.localName();
        return name.substring(name.lastIndexOf('-') + 1);
    }
}
