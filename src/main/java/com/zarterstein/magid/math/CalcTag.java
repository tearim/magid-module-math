package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.SemanticType;

/**
 * {@code <math-calc>formula</math-calc>}: computes a formula in doubles ({@link Calc} has the grammar).
 * <pre>{@code
 * <math-calc>2 + 3 * 4</math-calc>                                  prints 14
 * <math-calc receiver="$hyp">sqrt($a^2 + $b^2)</math-calc>          stores, prints nothing
 * <math-calc receiver="$s" print="true" digits="2">sin(30deg) * $r</math-calc>    30deg, or 30°
 * <math-calc>mean($scores) + {player.gold}</math-calc>
 * }</pre>
 * {@code {…}} and {@code $(…)} in the text are expanded by the engine before the formula is read; {@code $x},
 * {@code #x}, {@code actor.key} and {@code actor::key} are read by the formula itself, without going through text.
 * Attributes: {@value #RECEIVER}, {@value #PRINT}, {@value #DIGITS} ({@link AbstractMathTag}).
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = "calc", types = {SemanticType.TEXT_GENERATOR, SemanticType.DYNAMIC_GENERATOR})
public class CalcTag extends AbstractMathTag {

    @Override
    protected double compute() {
        String formula = element.text();
        if (tagContext.evaluator().hasExpressions(formula)) formula = tagContext.evaluator().evaluate(formula);
        return Calc.parse(formula).eval(env());
    }
}
