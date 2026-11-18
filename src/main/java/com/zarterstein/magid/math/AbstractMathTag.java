package com.zarterstein.magid.math;

import com.zarterstein.magid.api.reveries.Reverie;
import com.zarterstein.magid.api.symbols.SymbolPinax;
import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.TagAttributeException;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagException;
import com.zarterstein.magid.api.tags.TagProcessorResult;

import java.util.Map;
import java.util.Optional;

/**
 * What every computing tag of the pack does: compute a double, store it in the {@value #RECEIVER} symbol if there
 * is one, and print it in the story's {@linkplain MathStyle print style} if there is no receiver or
 * {@value #PRINT}{@code ="true"}.
 * <ul>
 *   <li>In a narration block ({@code <description>} …) the result joins the text around it.</li>
 *   <li>In an action it is narrated.</li>
 *   <li>A receiver always gets the full value ({@link Numbers#stored}), never the printed one.</li>
 *   <li>{@value #DIGITS}{@code ="2"} prints this result with two decimals, whatever the story's style.</li>
 *   <li>An error throws {@link TagAttributeException} (or {@link TagException} for a formula in the tag's text),
 *       unless the story set {@code <math-errors reaction="silent"/>}: then the tag prints nothing, leaves its
 *       receiver alone, and the message goes into {@value MathState#LAST_ERROR}, which only the story clears.</li>
 * </ul>
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
public abstract class AbstractMathTag extends MagidTag {

    /** The symbol that receives the result, e.g. {@code $area} or {@code player.gold}. */
    public static final String RECEIVER = "receiver";
    /** With a receiver: print the result as well. */
    public static final String PRINT = "print";
    /** Decimals to print for this tag only. */
    public static final String DIGITS = "digits";

    /**
     * @return the result
     * @throws MathException if there is none
     */
    protected abstract double compute();

    @Override
    public Map<String, String> process(TagElement element, String context) {
        appendToLastText = true;   // inline: the result joins the text around it
        return TagProcessorResult.mapWithTextOnly(run());
    }

    @Override
    public Outcome execute() {
        String text = run();
        if (!text.isEmpty()) narrate(text);
        return Outcome.proceed();
    }

    @Override
    public boolean newLine() {
        return false;
    }

    @Override
    public boolean zeroLength() {
        return !printing();
    }

    /** @return the text to print; {@code ""} if nothing is printed */
    private String run() {
        SymbolPinax symbols = tagContext.symbols();
        try {
            double result = Numbers.requireFinite(compute(), "the result");
            String receiver = receiver();
            if (receiver != null) symbols.set(receiver, Numbers.stored(result));
            return printing() ? style(symbols).format(result) : "";
        } catch (MathException e) {
            String message = element.name() + ": " + e.getMessage();
            if (!MathState.silent(symbols)) throw toTagException(message, e.attribute());
            tagContext.logger().fine(message);
            MathState.rememberError(symbols, message);
            return "";
        }
    }

    private boolean printing() {
        return receiver() == null || element.isTrue(PRINT, false);
    }

    private String receiver() {
        String receiver = element.attribute(RECEIVER);
        return receiver == null || receiver.isBlank() ? null : receiver.strip();
    }

    private MathStyle style(SymbolPinax symbols) {
        MathStyle style = MathState.style(symbols);
        if (!element.hasAttribute(DIGITS)) return style;
        double digits = Numbers.parse(getAttributeAsString(DIGITS));
        if (digits != Math.rint(digits) || digits < 0 || digits > MathStyle.MAX_DIGITS) {
            throw new MathException("digits must be a whole number from 0 to " + MathStyle.MAX_DIGITS, DIGITS);
        }
        return style.withDigits((int) digits);
    }

    private TagException toTagException(String message, String attribute) {
        return attribute == null
                ? new TagException(message, element.position())
                : new TagAttributeException(message, attribute, element.position(attribute));
    }

    // ---- for the subclasses ----

    /**
     * @param attribute an attribute holding a formula, e.g. {@code x="$angle * 2"}
     * @return its value; {@code {…}} and {@code $(…)} are expanded by the engine first
     * @throws MathException naming the attribute, if it doesn't compute
     */
    protected double number(String attribute) {
        try {
            return Calc.parse(getAttributeAsString(attribute)).eval(env());
        } catch (MathException e) {
            throw e.in(attribute);
        }
    }

    /**
     * @param attribute an attribute holding a list, e.g. {@code values="1, 3, {x}, $scores"}
     * @return its values, a symbol's list spread out
     * @throws MathException naming the attribute, if it doesn't compute
     */
    protected double[] numbers(String attribute) {
        try {
            return Calc.values(Calc.parseList(getAttributeAsString(attribute)), env());
        } catch (MathException e) {
            throw e.in(attribute);
        }
    }

    /**
     * @return the story's symbols, as a formula reads them
     */
    protected Calc.Env env() {
        SymbolPinax symbols = tagContext.symbols();
        return name -> {
            try {
                Optional<Reverie<?>> value = symbols.recall(name);
                return value.map(reverie -> (Object) reverie.get());
            } catch (RuntimeException e) {
                throw new MathException(name + " can't be read: " + e.getMessage());
            }
        };
    }
}
