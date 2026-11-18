package com.zarterstein.magid.math;

import com.zarterstein.magid.api.reveries.Reverie;
import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.SemanticType;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagProcessorResult;

import java.util.Map;

/**
 * {@code <math-last-error/>}: prints the last error a silent math tag swallowed ({@code <math-errors
 * reaction="silent"/>}), e.g. {@code math-div: division by zero}; {@code none="…"} prints instead when there is none
 * (default: nothing). It reads {@value MathState#LAST_ERROR}, which only the story clears ({@link ClearErrorTag}).
 * <pre>{@code
 * <action id="share">
 *     <math-clear-error/>
 *     <math-div a="{player.gold}" b="$friends" receiver="$share"/>
 *     <math-last-error none="Shared."/>
 * </action>
 * }</pre>
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = "last-error", types = {SemanticType.TEXT_GENERATOR})
public class LastErrorTag extends MagidTag {

    /** The text printed when there is no error. */
    public static final String NONE = "none";

    @Override
    public Map<String, String> process(TagElement element, String context) {
        appendToLastText = true;
        return TagProcessorResult.mapWithTextOnly(text());
    }

    @Override
    public Outcome execute() {
        String text = text();
        if (!text.isEmpty()) narrate(text);
        return Outcome.proceed();
    }

    private String text() {
        String error = tagContext.symbols().recall(MathState.LAST_ERROR).map(Reverie::get).map(String::valueOf)
                .orElse("");
        return error.isEmpty() ? element.attribute(NONE) : error;
    }

    @Override
    public boolean newLine() {
        return false;
    }

    @Override
    public boolean zeroLength() {
        return false;
    }
}
