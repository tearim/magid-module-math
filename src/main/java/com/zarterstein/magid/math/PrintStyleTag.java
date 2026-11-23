package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.SemanticType;
import com.zarterstein.magid.api.tags.TagAttributeException;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagProcessorResult;

import java.util.Map;

/**
 * {@code <math-print-style/>}: how the math tags print from here on, in this session. Every attribute it leaves out
 * returns to its default, so {@code <math-print-style/>} alone restores the defaults.
 * <pre>{@code
 * <math-print-style digits="2"/>                       3.14, 2.50 → 2.5
 * <math-print-style digits="2" trim="false"/>          2.50
 * <math-print-style decimal=","/>                      2,5
 * <math-print-style notation="scientific"/>            1.5E3
 * }</pre>
 * <ul>
 *   <li>{@code digits}: decimals, rounded half away from zero; default all the digits the double has.</li>
 *   <li>{@code trim}: drop trailing zeros; default {@code true}.</li>
 *   <li>{@code notation}: {@code auto} (default: plain unless below 10<sup>-6</sup> or from 10<sup>15</sup>),
 *       {@code plain} or {@code scientific}.</li>
 *   <li>{@code decimal}: {@code .} (default) or {@code ,}. Only printing changes: formulas and receivers always
 *       use a dot.</li>
 * </ul>
 * The style is kept in {@value MathState#STYLE}, so it is saved with the session. A malformed attribute always
 * throws {@link TagAttributeException}, also with silent errors: it is a mistake in the story, not in a calculation.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = "print-style", types = {SemanticType.TEXT_GENERATOR})
public class PrintStyleTag extends MagidTag {

    @Override
    public Map<String, String> process(TagElement element, String context) {
        apply();
        return TagProcessorResult.mapWithTextOnly("");
    }

    @Override
    public Outcome execute() {
        apply();
        return Outcome.proceed();
    }

    private void apply() {
        MathStyle style = new MathStyle(digits(), bool("trim", MathStyle.DEFAULT.trim()), notation(), decimal());
        tagContext.symbols().set(MathState.STYLE, style.serialize());
    }

    private int digits() {
        if (!element.hasAttribute("digits")) return MathStyle.DEFAULT.digits();
        String text = getAttributeAsString("digits").strip();
        try {
            int digits = Integer.parseInt(text);
            if (digits >= 0 && digits <= MathStyle.MAX_DIGITS) return digits;
        } catch (NumberFormatException ignored) {
            // falls through to the exception below
        }
        throw invalid("digits", "'" + text + "' isn't a number of decimals from 0 to " + MathStyle.MAX_DIGITS);
    }

    private boolean bool(String attribute, boolean fallback) {
        if (!element.hasAttribute(attribute)) return fallback;
        String text = getAttributeAsString(attribute).strip();
        if (text.equalsIgnoreCase("true")) return true;
        if (text.equalsIgnoreCase("false")) return false;
        throw invalid(attribute, "'" + text + "' is neither true nor false");
    }

    private MathStyle.Notation notation() {
        if (!element.hasAttribute("notation")) return MathStyle.DEFAULT.notation();
        try {
            return MathStyle.Notation.of(getAttributeAsString("notation"));
        } catch (IllegalArgumentException e) {
            throw invalid("notation", e.getMessage());
        }
    }

    private String decimal() {
        if (!element.hasAttribute("decimal")) return MathStyle.DEFAULT.decimalSeparator();
        String text = getAttributeAsString("decimal").strip();
        if (text.equals(".") || text.equals(",")) return text;
        throw invalid("decimal", "the decimal separator is . or , not '" + text + "'");
    }

    private TagAttributeException invalid(String attribute, String message) {
        return new TagAttributeException(element.name() + ": " + message, element, attribute);
    }

    @Override
    public boolean newLine() {
        return false;
    }

    @Override
    public boolean zeroLength() {
        return true;
    }
}
