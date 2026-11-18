package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Calc}: the formula language.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class CalcTest {

    private final Map<String, Object> symbols = new HashMap<>();
    private final Calc.Env env = name -> Optional.ofNullable(symbols.get(name));

    private double calc(String formula) {
        return Calc.parse(formula).eval(env);
    }

    @Test
    @DisplayName("* binds tighter than +: 2 + 3 * 4 is 14")
    void precedence() {
        assertEquals(14, calc("2 + 3 * 4"));
    }

    @Test
    @DisplayName("Parentheses group: (2 + 3) * 4 is 20")
    void parentheses() {
        assertEquals(20, calc("(2 + 3) * 4"));
    }

    @Test
    @DisplayName("^ binds tighter than unary minus: -2^2 is -4")
    void powerOverNegation() {
        assertEquals(-4, calc("-2^2"));
    }

    @Test
    @DisplayName("^ is right-associative: 2^3^2 is 512")
    void powerRightAssociative() {
        assertEquals(512, calc("2^3^2"));
    }

    @Test
    @DisplayName("A negative exponent: 2^-1 is 0.5")
    void negativeExponent() {
        assertEquals(0.5, calc("2^-1"));
    }

    @Test
    @DisplayName("× ÷ − and · are accepted")
    void unicodeOperators() {
        assertEquals(6, calc("2 × 6 ÷ 2"));
        assertEquals(1, calc("3 − 2"));
        assertEquals(6, calc("2 · 3"));
    }

    @Test
    @DisplayName("% is mod: -1 % 3 is 2")
    void percentIsMod() {
        assertEquals(2, calc("-1 % 3"));
    }

    @Test
    @DisplayName("sin(30°) is exactly 0.5, as sind(30)")
    void sinDegrees() {
        assertEquals(0.5, calc("sin(30°)"));
    }

    @Test
    @DisplayName("deg is the same as °: sin(30deg) is exactly 0.5")
    void sinDeg() {
        assertEquals(0.5, calc("sin(30deg)"));
    }

    @Test
    @DisplayName("deg may stand after a space: cos(60 deg) is exactly 0.5")
    void degAfterSpace() {
        assertEquals(0.5, calc("cos(60 deg)"));
    }

    @Test
    @DisplayName("deg after a parenthesis: tan((40 + 5)deg) is exactly 1")
    void degAfterParenthesis() {
        assertEquals(1, calc("tan((40 + 5)deg)"));
    }

    @Test
    @DisplayName("180deg alone is π")
    void degToRadians() {
        assertEquals(Math.PI, calc("180deg"));
    }

    @Test
    @DisplayName("deg is a whole word: degrees(pi) is still the function, 180")
    void degreesFunctionUntouched() {
        assertEquals(180, calc("degrees(pi)"));
    }

    @Test
    @DisplayName("180° alone is π")
    void degreesToRadians() {
        assertEquals(Math.PI, calc("180°"));
    }

    @Test
    @DisplayName("Constants without parentheses: pi, e")
    void constants() {
        assertEquals(Math.PI, calc("pi"));
        assertEquals(Math.E, calc("e"));
    }

    @Test
    @DisplayName("A function with arguments: hypot(3, 4) is 5")
    void functionCall() {
        assertEquals(5, calc("hypot(3, 4)"));
    }

    @Test
    @DisplayName("An aggregate takes any number of arguments: mean(1, 2, 3, 4) is 2.5")
    void aggregateCall() {
        assertEquals(2.5, calc("mean(1, 2, 3, 4)"));
    }

    @Test
    @DisplayName("A variable holding an integer, a constant holding text, an actor's memory")
    void symbols() {
        symbols.put("$x", 7);
        symbols.put("#rate", "0.5");
        symbols.put("player.gold", 10);

        assertEquals(13.5, calc("$x * 1 + #rate + player.gold - 4"));
    }

    @Test
    @DisplayName("A float symbol reads as the story shows it: 0.1f is 0.1, not 0.10000000149011612")
    void floatSymbol() {
        symbols.put("$f", 0.1f);

        assertEquals(0.1, calc("$f"));
    }

    @Test
    @DisplayName("A stored canonical result reads back with every bit")
    void canonicalSymbol() {
        symbols.put("$sum", Numbers.canonical(0.1 + 0.2));

        assertEquals(0.1 + 0.2, calc("$sum"));
    }

    @Test
    @DisplayName("A symbol holding a list spreads into an aggregate: mean($scores) with '2, 4, 9'")
    void listSymbol() {
        symbols.put("$scores", "2, 4, 9");

        assertEquals(5, calc("mean($scores)"));
        assertEquals(6.2, calc("mean(1, $scores, 15)"));
    }

    @Test
    @DisplayName("A symbol holding a list is an error where one number goes")
    void listWhereNumberGoes() {
        symbols.put("$scores", "2, 4, 9");

        MathException e = assertThrows(MathException.class, () -> calc("$scores + 1"));
        assertTrue(e.getMessage().contains("holds a list"), e.getMessage());
    }

    @Test
    @DisplayName("An unset symbol is an error naming it")
    void unsetSymbol() {
        MathException e = assertThrows(MathException.class, () -> calc("$missing + 1"));
        assertTrue(e.getMessage().contains("$missing"), e.getMessage());
    }

    @Test
    @DisplayName("An unknown name is an error naming it")
    void unknownName() {
        MathException e = assertThrows(MathException.class, () -> calc("foo(2)"));
        assertTrue(e.getMessage().contains("foo"), e.getMessage());
    }

    @Test
    @DisplayName("A missing parenthesis is an error")
    void missingParenthesis() {
        assertThrows(MathException.class, () -> calc("(2 + 3"));
    }

    @Test
    @DisplayName("Too many arguments is an error showing the signature")
    void wrongArity() {
        MathException e = assertThrows(MathException.class, () -> calc("sqrt(2, 3)"));
        assertTrue(e.getMessage().contains("sqrt(x)"), e.getMessage());
    }

    @Test
    @DisplayName("No implicit multiplication: 2pi is an error")
    void noImplicitMultiplication() {
        assertThrows(MathException.class, () -> calc("2pi"));
    }

    @Test
    @DisplayName("An empty formula is an error")
    void emptyFormula() {
        assertThrows(MathException.class, () -> calc("  "));
    }

    @Test
    @DisplayName("A list: 1, 3, $x")
    void list() {
        symbols.put("$x", 5);

        assertArrayEquals(new double[]{1, 3, 5}, Calc.values(Calc.parseList("1, 3, $x"), env));
    }
}
