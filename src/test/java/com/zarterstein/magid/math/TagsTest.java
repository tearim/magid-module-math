package com.zarterstein.magid.math;

import com.zarterstein.magid.api.tags.MagidXmlTag;
import com.zarterstein.magid.api.tags.TagAttributeException;
import com.zarterstein.magid.api.tags.TagException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tags as a story uses them: printing, receivers, the print style and the error reaction.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class TagsTest {

    private final Fakes.Story story = new Fakes.Story();

    // ---- calc ----

    @Test
    @DisplayName("<math-calc> prints its result")
    void calcPrints() {
        assertEquals("14", story.renderWithText(new CalcTag(), "math-calc", "2 + 3 * 4"));
    }

    @Test
    @DisplayName("With a receiver, <math-calc> stores the full value and prints nothing")
    void calcStores() {
        String printed = story.renderWithText(new CalcTag(), "math-calc", "0.1 + 0.2", "receiver", "$x");

        assertEquals("", printed);
        assertEquals("0.30000000000000004", story.symbols.text("$x"));
    }

    @Test
    @DisplayName("With a receiver and print=\"true\", it stores and prints")
    void calcStoresAndPrints() {
        String printed = story.renderWithText(new CalcTag(), "math-calc", "1 / 4", "receiver", "$x", "print", "true");

        assertEquals("0.25", printed);
        assertEquals("0.25", story.symbols.text("$x"));
    }

    @Test
    @DisplayName("An integral result is stored as an integer")
    void integerStored() {
        story.renderWithText(new CalcTag(), "math-calc", "6 * 7", "receiver", "$x");

        assertEquals(42, story.symbols.recall("$x").orElseThrow().get());
    }

    @Test
    @DisplayName("A fractional result is stored as text, so the engine doesn't round it to a float")
    void fractionStoredAsText() {
        story.renderWithText(new CalcTag(), "math-calc", "0.1 + 0.2", "receiver", "$x");

        assertEquals("0.30000000000000004", story.symbols.recall("$x").orElseThrow().get());
    }

    @Test
    @DisplayName("A result stored by one tag is read by the next with every bit")
    void precisionAcrossTags() {
        story.renderWithText(new CalcTag(), "math-calc", "0.1 + 0.2", "receiver", "$x");

        assertEquals("0.6000000000000001", story.renderWithText(new CalcTag(), "math-calc", "$x * 2"));
    }

    @Test
    @DisplayName("digits on the tag prints rounded; the receiver still gets every digit")
    void digitsPrintOnly() {
        String printed = story.renderWithText(new CalcTag(), "math-calc", "2 / 3",
                "receiver", "$x", "print", "true", "digits", "2");

        assertEquals("0.67", printed);
        assertEquals("0.6666666666666666", story.symbols.text("$x"));
    }

    @Test
    @DisplayName("In an action, the result is narrated")
    void calcNarrates() {
        story.execute(new CalcTag(), "math-calc", "6 * 7");

        assertEquals(List.of("42"), story.narrated);
    }

    @Test
    @DisplayName("In an action with only a receiver, nothing is narrated")
    void calcActionStoresSilently() {
        story.execute(new CalcTag(), "math-calc", "6 * 7", "receiver", "$x");

        assertEquals(List.of(), story.narrated);
        assertEquals("42", story.symbols.text("$x"));
    }

    // ---- function tags ----

    @Test
    @DisplayName("<math-sqrt x=\"2\"/> prints the correctly rounded root")
    void functionTag() {
        assertEquals("1.4142135623730951", story.render(new FunctionTag(), "math-sqrt", "x", "2"));
    }

    @Test
    @DisplayName("A function tag under another prefix finds its function: <m-sind x=\"30\"/> prints 0.5")
    void otherPrefix() {
        assertEquals("0.5", story.render(new FunctionTag(), "m-sind", "x", "30"));
    }

    @Test
    @DisplayName("A function tag without a prefix (as=\"none\") finds its function: <round x=\"2.675\" decimals=\"2\"/>")
    void noPrefix() {
        assertEquals("2.68", story.render(new FunctionTag(), "round", "x", "2.675", "decimals", "2"));
    }

    @Test
    @DisplayName("Attributes are formulas: <math-pow a=\"$x + 1\" b=\"2\"/>")
    void attributeFormula() {
        story.symbols.set("$x", 2);

        assertEquals("9", story.render(new FunctionTag(), "math-pow", "a", "$x + 1", "b", "2"));
    }

    @Test
    @DisplayName("An aggregate takes values, a symbol's list spread among them")
    void aggregateTag() {
        story.symbols.set("$scores", "2, 4, 9");

        assertEquals("4", story.render(new FunctionTag(), "math-mean", "values", "1, $scores"));
    }

    @Test
    @DisplayName("A constant tag: <math-pi digits=\"4\"/> prints 3.1416")
    void constantTag() {
        assertEquals("3.1416", story.render(new FunctionTag(), "math-pi", "digits", "4"));
    }

    @Test
    @DisplayName("A missing attribute throws TagAttributeException naming it")
    void missingAttribute() {
        TagAttributeException e = assertThrows(TagAttributeException.class,
                () -> story.render(new FunctionTag(), "math-div", "a", "1"));
        assertEquals("b", e.attribute());
    }

    @Test
    @DisplayName("An optional attribute without the one before it is an error: decimals needs x")
    void optionalWithoutRequired() {
        assertThrows(TagAttributeException.class, () -> story.render(new FunctionTag(), "math-round", "decimals", "2"));
    }

    @Test
    @DisplayName("FunctionTag answers to exactly the functions there are")
    void namesMatchFunctions() {
        List<String> targets = Arrays.stream(FunctionTag.class.getAnnotation(MagidXmlTag.class).target().split(","))
                .map(String::strip).toList();
        List<String> functions = Functions.all().stream().map(Functions.MathFunction::name).toList();

        assertEquals(functions, targets);
    }

    // ---- errors ----

    @Test
    @DisplayName("A malformed attribute throws TagAttributeException naming the tag and the attribute")
    void attributeError() {
        TagAttributeException e = assertThrows(TagAttributeException.class,
                () -> story.render(new FunctionTag(), "math-div", "a", "1", "b", "three"));
        assertEquals("b", e.attribute());
        assertTrue(e.getMessage().startsWith("math-div: "), e.getMessage());
    }

    @Test
    @DisplayName("A calculation without an answer throws TagException: <math-div a=\"1\" b=\"0\"/>")
    void calculationError() {
        TagException e = assertThrows(TagException.class,
                () -> story.render(new FunctionTag(), "math-div", "a", "1", "b", "0"));
        assertEquals("math-div: division by zero", e.getMessage());
    }

    @Test
    @DisplayName("An error in a formula in the text throws TagException")
    void formulaError() {
        TagException e = assertThrows(TagException.class,
                () -> story.renderWithText(new CalcTag(), "math-calc", "1 / 0"));
        assertTrue(e.getMessage().contains("division by zero"), e.getMessage());
    }

    @Test
    @DisplayName("With silent errors, a failing tag prints nothing, leaves its receiver, and keeps the message")
    void silentError() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.symbols.set("$x", "5");

        String printed = story.renderWithText(new CalcTag(), "math-calc", "1 / 0", "receiver", "$x", "print", "true");

        assertEquals("", printed);
        assertEquals("5", story.symbols.text("$x"));
        assertTrue(story.symbols.text(MathState.LAST_ERROR).contains("division by zero"),
                story.symbols.text(MathState.LAST_ERROR));
    }

    @Test
    @DisplayName("A tag that succeeds leaves the last error for the story to read")
    void successKeepsError() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.renderWithText(new CalcTag(), "math-calc", "1 / 0");

        story.renderWithText(new CalcTag(), "math-calc", "1 / 2");

        assertEquals("math-calc: division by zero", story.symbols.text(MathState.LAST_ERROR));
    }

    @Test
    @DisplayName("<math-last-error/> prints the swallowed error")
    void lastErrorPrints() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.render(new FunctionTag(), "math-div", "a", "1", "b", "0");

        assertEquals("math-div: division by zero", story.render(new LastErrorTag(), "math-last-error"));
    }

    @Test
    @DisplayName("<math-last-error none=\"…\"/> prints its none text when there is no error")
    void lastErrorNone() {
        assertEquals("all fine", story.render(new LastErrorTag(), "math-last-error", "none", "all fine"));
    }

    @Test
    @DisplayName("<math-last-error/> without an error and without none prints nothing")
    void lastErrorEmpty() {
        assertEquals("", story.render(new LastErrorTag(), "math-last-error"));
    }

    @Test
    @DisplayName("<math-clear-error/> forgets the error")
    void clearError() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.render(new FunctionTag(), "math-div", "a", "1", "b", "0");

        story.render(new ClearErrorTag(), "math-clear-error");

        assertEquals("", story.symbols.text(MathState.LAST_ERROR));
        assertEquals("none", story.render(new LastErrorTag(), "math-last-error", "none", "none"));
    }

    @Test
    @DisplayName("In an action, <math-last-error/> narrates the error")
    void lastErrorNarrates() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.render(new FunctionTag(), "math-sqrt", "x", "-1");

        story.execute(new LastErrorTag(), "math-last-error", "");

        assertEquals(1, story.narrated.size());
        assertTrue(story.narrated.getFirst().toString().startsWith("math-sqrt: "), story.narrated.toString());
    }

    @Test
    @DisplayName("reaction=\"throw\" restores throwing")
    void throwAgain() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");
        story.render(new ErrorsTag(), "math-errors", "reaction", "throw");

        assertThrows(TagException.class, () -> story.renderWithText(new CalcTag(), "math-calc", "1 / 0"));
    }

    @Test
    @DisplayName("An unknown reaction throws TagAttributeException")
    void unknownReaction() {
        assertThrows(TagAttributeException.class, () -> story.render(new ErrorsTag(), "math-errors", "reaction", "loud"));
    }

    // ---- print style ----

    @Test
    @DisplayName("<math-print-style digits=\"2\"/> rounds what the next tags print")
    void printStyleDigits() {
        story.render(new PrintStyleTag(), "math-print-style", "digits", "2");

        assertEquals("0.67", story.renderWithText(new CalcTag(), "math-calc", "2 / 3"));
    }

    @Test
    @DisplayName("<math-print-style decimal=\",\"/> prints a decimal comma; formulas still use a dot")
    void printStyleComma() {
        story.render(new PrintStyleTag(), "math-print-style", "decimal", ",");

        assertEquals("2,5", story.renderWithText(new CalcTag(), "math-calc", "2.5"));
    }

    @Test
    @DisplayName("<math-print-style/> alone restores the defaults")
    void printStyleReset() {
        story.render(new PrintStyleTag(), "math-print-style", "digits", "2");
        story.render(new PrintStyleTag(), "math-print-style");

        assertEquals("0.6666666666666666", story.renderWithText(new CalcTag(), "math-calc", "2 / 3"));
    }

    @Test
    @DisplayName("A malformed style throws, even with silent errors")
    void printStyleInvalid() {
        story.render(new ErrorsTag(), "math-errors", "reaction", "silent");

        TagAttributeException e = assertThrows(TagAttributeException.class,
                () -> story.render(new PrintStyleTag(), "math-print-style", "digits", "many"));
        assertEquals("digits", e.attribute());
    }

    @Test
    @DisplayName("The style lives in the story's symbols, so a save keeps it")
    void styleIsSymbol() {
        story.render(new PrintStyleTag(), "math-print-style", "digits", "3", "decimal", ",");

        assertEquals("digits=3;trim=true;notation=auto;decimal=,", story.symbols.text(MathState.STYLE));
    }
}
