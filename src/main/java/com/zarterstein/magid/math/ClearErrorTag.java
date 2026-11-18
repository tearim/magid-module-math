package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.SemanticType;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagProcessorResult;

import java.util.Map;

/**
 * {@code <math-clear-error/>}: forgets the last error a silent math tag swallowed ({@value MathState#LAST_ERROR}).
 * The pack never clears it by itself, because a scene's description renders again after every action; put this tag
 * before the tags whose errors a story wants to check, then {@code <math-last-error/>} or an {@code <if>}. Prints
 * nothing.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
@MagidXmlTag(target = "clear-error", types = {SemanticType.TEXT_GENERATOR})
public class ClearErrorTag extends MagidTag {

    @Override
    public Map<String, String> process(TagElement element, String context) {
        tagContext.symbols().set(MathState.LAST_ERROR, "");
        return TagProcessorResult.mapWithTextOnly("");
    }

    @Override
    public Outcome execute() {
        tagContext.symbols().set(MathState.LAST_ERROR, "");
        return Outcome.proceed();
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
