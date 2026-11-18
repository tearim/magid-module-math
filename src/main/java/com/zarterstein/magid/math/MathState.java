package com.zarterstein.magid.math;

import com.zarterstein.magid.api.reveries.Reverie;
import com.zarterstein.magid.api.symbols.SymbolPinax;

import java.util.Optional;

/**
 * What the pack remembers in a session: the print style and the error reaction, in the story's symbols, so they are
 * saved and restored with the session. The names carry the pack's prefix ({@code math}), whatever prefix a story
 * gives the pack, and need no declaration.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class MathState {

    /** The print style ({@link MathStyle#serialize()}), set by {@code <math-print-style/>}. */
    static final String STYLE = "$math_print_style";
    /** The error reaction, {@value #THROW} or {@value #SILENT}, set by {@code <math-errors/>}. */
    static final String ERRORS = "$math_errors";
    /**
     * The last error a silent tag swallowed. The pack never clears it: the scene's description renders again after
     * every action, and its tags would wipe the message before the story could show it. A story clears it itself
     * ({@code <set id="$math_last_error" value=""/>}) before the tags it wants to check.
     */
    static final String LAST_ERROR = "$math_last_error";

    /** Errors stop the story with an exception (the default). */
    static final String THROW = "throw";
    /** Errors are logged and kept in {@value #LAST_ERROR}; the tag prints nothing and leaves its receiver alone. */
    static final String SILENT = "silent";

    private MathState() {
    }

    /**
     * @param symbols the story's symbols
     * @return the story's print style; {@link MathStyle#DEFAULT} if it set none
     */
    static MathStyle style(SymbolPinax symbols) {
        return read(symbols, STYLE).map(MathStyle::parse).orElse(MathStyle.DEFAULT);
    }

    /**
     * @param symbols the story's symbols
     * @return whether the story asked for silent errors
     */
    static boolean silent(SymbolPinax symbols) {
        return read(symbols, ERRORS).map(SILENT::equals).orElse(false);
    }

    /**
     * Keeps the message of a swallowed error.
     *
     * @param symbols the story's symbols
     * @param message the error's message
     */
    static void rememberError(SymbolPinax symbols, String message) {
        symbols.set(LAST_ERROR, message);
    }

    private static Optional<String> read(SymbolPinax symbols, String name) {
        return symbols.recall(name).map(Reverie::get).map(String::valueOf);
    }
}
