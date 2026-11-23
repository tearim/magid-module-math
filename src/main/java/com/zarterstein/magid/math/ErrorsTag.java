package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.SemanticType;
import com.zarterstein.magid.api.tags.TagAttributeException;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagProcessorResult;

import java.util.Locale;
import java.util.Map;

/**
 * {@code <math-errors reaction="silent"/>}: what a math tag does when it has no answer (a division by zero, a formula
 * that doesn't parse, a result that isn't a finite number), from here on, in this session.
 * <ul>
 *   <li>{@code throw} (the default): the tag throws, and the engine reports the error.</li>
 *   <li>{@code silent}: the tag prints nothing and leaves its receiver alone; the message goes into the symbol
 *       {@value MathState#LAST_ERROR} (and to the log). Only the story clears it, so a story can show it, or
 *       test it in an {@code <if>}, also after the scene has rendered again.</li>
 * </ul>
 * Kept in {@value MathState#ERRORS}, so it is saved with the session. A missing or unknown {@code reaction} always
 * throws {@link TagAttributeException}.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = "errors", types = {SemanticType.TEXT_GENERATOR})
public class ErrorsTag extends MagidTag {

    /** The attribute that names the reaction. */
    public static final String REACTION = "reaction";

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
        String reaction = getAttributeAsString(REACTION).strip().toLowerCase(Locale.ROOT);
        if (!reaction.equals(MathState.THROW) && !reaction.equals(MathState.SILENT)) {
            throw new TagAttributeException(element.name() + ": reaction is " + MathState.THROW + " or "
                    + MathState.SILENT + ", not '" + reaction + "'", element, REACTION);
        }
        tagContext.symbols().set(MathState.ERRORS, reaction);
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
