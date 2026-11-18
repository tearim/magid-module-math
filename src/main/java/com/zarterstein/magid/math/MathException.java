package com.zarterstein.magid.math;

/**
 * A calculation without an answer: a malformed number or formula, a division by zero, a result that isn't a finite
 * number. The tags turn it into the API's {@code TagAttributeException} or {@code TagException}, or swallow it when
 * the story said {@code <math-errors reaction="silent"/>}.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class MathException extends RuntimeException {

    private final String attribute;

    /**
     * @param message what went wrong, for the story's author
     */
    MathException(String message) {
        this(message, null);
    }

    /**
     * @param message   what went wrong, for the story's author
     * @param attribute the attribute that caused it; {@code null} if none (the tag's text, or no attribute at all)
     */
    MathException(String message, String attribute) {
        super(message);
        this.attribute = attribute;
    }

    /**
     * @return the attribute that caused the problem; {@code null} if none
     */
    String attribute() {
        return attribute;
    }

    /**
     * @param attribute the attribute being computed
     * @return this exception if it already names an attribute, otherwise the same message naming {@code attribute}
     */
    MathException in(String attribute) {
        return this.attribute != null ? this : new MathException(getMessage(), attribute);
    }
}
