package com.zarterstein.magid.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The formulas of {@code <math-calc>}, and the values of every math attribute: a small parser of its own, in
 * doubles from start to end (no JEXL, no side effects).
 *
 * <pre>
 * list       = expression ("," expression)*
 * expression = term (("+" | "-") term)*
 * term       = unary (("*" | "/" | "%") unary)*            % is mod: the remainder has the divisor's sign
 * unary      = ("-" | "+") unary | power
 * power      = postfix ("^" unary)?                          right-associative; -2^2 is -4
 * postfix    = primary ("°" | "deg")*                        degrees to radians: 30° or 30deg (or 30 deg)
 * primary    = number | "(" expression ")" | symbol | name | name "(" list? ")"
 * symbol     = "$"name | "#"name | name"."name | name"::"name     the story's symbols, read as numbers
 * </pre>
 * Also accepted: {@code ×} and {@code ·} for {@code *}, {@code ÷} for {@code /}, {@code −} for {@code -}. Names
 * are the {@linkplain Functions functions}; a name without parentheses is a constant ({@code pi}, {@code e},
 * {@code tau}). {@code sin(30°)}, {@code cos(…°)} and {@code tan(…°)} compute in degrees, exactly where a story
 * expects it ({@link Degrees}). {@code deg} is the same as {@code °}, for keyboards without the sign:
 * {@code sin(30deg)}; it is a whole word, so {@code degrees(pi)} is still the function. A symbol holding a list ({@code "1, 2, 3"}) spreads into an aggregate:
 * {@code mean($scores)}.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Calc {

    /** Where a formula reads the story's symbols. */
    @FunctionalInterface
    interface Env {
        /**
         * @param name a symbol, e.g. {@code $x}, {@code #rate}, {@code player.gold}
         * @return its value (an {@code Integer}, {@code Float}, {@code String} or {@code Boolean}); empty if unset
         */
        Optional<Object> symbol(String name);
    }

    /** A parsed formula. */
    interface Node {
        /**
         * @param env the story's symbols
         * @return the value
         */
        double eval(Env env);

        /**
         * @param env the story's symbols
         * @return the values, where an aggregate takes them: one, or a symbol's whole list
         */
        default List<Double> evalList(Env env) {
            return List.of(eval(env));
        }
    }

    private final String text;
    private int at;

    private Calc(String text) {
        this.text = text;
    }

    /**
     * @param text a formula, e.g. {@code sqrt($a^2 + $b^2)}
     * @return the parsed formula
     * @throws MathException if the text isn't one
     */
    static Node parse(String text) {
        Calc calc = new Calc(text == null ? "" : text);
        calc.skipSpaces();
        if (calc.atEnd()) throw new MathException("there is no formula");
        Node node = calc.expression();
        calc.skipSpaces();
        if (!calc.atEnd()) throw calc.error("unexpected '" + calc.text.charAt(calc.at) + "'");
        return node;
    }

    /**
     * @param text formulas separated by commas, e.g. {@code 1, 3, $x}; blank for none
     * @return the parsed formulas
     * @throws MathException if the text isn't such a list
     */
    static List<Node> parseList(String text) {
        Calc calc = new Calc(text == null ? "" : text);
        calc.skipSpaces();
        if (calc.atEnd()) return List.of();
        List<Node> nodes = calc.list();
        calc.skipSpaces();
        if (!calc.atEnd()) throw calc.error("unexpected '" + calc.text.charAt(calc.at) + "'");
        return nodes;
    }

    /**
     * @param nodes formulas
     * @param env   the story's symbols
     * @return their values, a symbol's list spread out
     */
    static double[] values(List<Node> nodes, Env env) {
        List<Double> values = new ArrayList<>();
        for (Node node : nodes) values.addAll(node.evalList(env));
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // ---- grammar ----

    private List<Node> list() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(expression());
        while (take(',')) nodes.add(expression());
        return nodes;
    }

    private Node expression() {
        Node left = term();
        while (true) {
            if (take('+')) {
                Node l = left, r = term();
                left = env -> l.eval(env) + r.eval(env);
            } else if (take('-') || take('−')) {
                Node l = left, r = term();
                left = env -> l.eval(env) - r.eval(env);
            } else {
                return left;
            }
        }
    }

    private Node term() {
        Node left = unary();
        while (true) {
            if (take('*') || take('×') || take('·')) {
                Node l = left, r = unary();
                left = env -> l.eval(env) * r.eval(env);
            } else if (take('/') || take('÷')) {
                Node l = left, r = unary();
                left = env -> Functions.divide(l.eval(env), r.eval(env), "/");
            } else if (take('%')) {
                Node l = left, r = unary();
                left = env -> Functions.floorMod(l.eval(env), r.eval(env));
            } else {
                return left;
            }
        }
    }

    private Node unary() {
        if (take('-') || take('−')) {
            Node operand = unary();
            return env -> -operand.eval(env);
        }
        if (take('+')) return unary();
        return power();
    }

    private Node power() {
        Node base = postfix();
        if (take('^')) {
            Node exponent = unary();
            return env -> StrictMath.pow(base.eval(env), exponent.eval(env));
        }
        return base;
    }

    private Node postfix() {
        Node node = primary();
        while (take('°') || takeWord("deg")) node = new InDegrees(node);
        return node;
    }

    private Node primary() {
        skipSpaces();
        if (atEnd()) throw error("a number, a name or '(' is missing");
        char c = text.charAt(at);
        if (take('(')) {
            Node inner = expression();
            expect(')');
            return inner;
        }
        if (Character.isDigit(c) || c == '.') return number();
        if (c == '$' || c == '#') {
            at++;
            String name = c + name();
            return new SymbolNode(name);
        }
        if (Character.isLetter(c) || c == '_') {
            String name = name();
            if (text.startsWith("::", at)) {
                at += 2;
                return new SymbolNode(name + "::" + name());
            }
            if (at < text.length() && text.charAt(at) == '.') {
                at++;
                return new SymbolNode(name + "." + name());
            }
            return call(name);
        }
        throw error("unexpected '" + c + "'");
    }

    private Node number() {
        int start = at;
        while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
        if (at < text.length() && text.charAt(at) == '.') {
            at++;
            while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
        }
        if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
            int mark = at;
            at++;
            if (at < text.length() && (text.charAt(at) == '+' || text.charAt(at) == '-')) at++;
            if (at < text.length() && Character.isDigit(text.charAt(at))) {
                while (at < text.length() && Character.isDigit(text.charAt(at))) at++;
            } else {
                at = mark;   // "2e" is 2 followed by the name e, which the caller rejects
            }
        }
        double value = Numbers.parse(text.substring(start, at));
        return env -> value;
    }

    private Node call(String name) {
        Functions.MathFunction function = Functions.get(name);
        if (function == null) throw error("unknown name '" + name + "'");
        List<Node> args = take('(') ? arguments() : null;
        if (args == null) {
            if (!function.params().isEmpty()) throw error(name + " needs parentheses: " + function.signature());
            return env -> Functions.call(function, new double[0]);
        }
        if (!function.aggregate() && !function.takes(args.size())) {
            throw error(function.signature() + " takes " + describeCount(function) + ", not " + args.size());
        }
        Functions.MathFunction degrees = degreeVariant(function, args);
        if (degrees != null) {
            Node angle = ((InDegrees) args.getFirst()).degrees();
            return env -> Functions.call(degrees, new double[]{angle.eval(env)});
        }
        if (function.aggregate()) return env -> Functions.call(function, values(args, env));
        return env -> {
            double[] values = new double[args.size()];
            for (int i = 0; i < values.length; i++) values[i] = args.get(i).eval(env);
            return Functions.call(function, values);
        };
    }

    private List<Node> arguments() {
        skipSpaces();
        if (take(')')) return List.of();
        List<Node> args = list();
        expect(')');
        return args;
    }

    /** {@code sin(30°)} is {@code sind(30)}, exact; any other use of ° converts to radians. */
    private static Functions.MathFunction degreeVariant(Functions.MathFunction function, List<Node> args) {
        if (args.size() != 1 || !(args.getFirst() instanceof InDegrees)) return null;
        return switch (function.name()) {
            case "sin", "cos", "tan" -> Functions.get(function.name() + "d");
            default -> null;
        };
    }

    private static String describeCount(Functions.MathFunction function) {
        int min = function.required();
        int max = function.params().size();
        if (min == max) return min + (min == 1 ? " argument" : " arguments");
        return min + " to " + max + " arguments";
    }

    // ---- tokens ----

    private String name() {
        int start = at;
        while (at < text.length() && (Character.isLetterOrDigit(text.charAt(at)) || text.charAt(at) == '_')) at++;
        if (start == at) throw error("a name is missing");
        return text.substring(start, at);
    }

    private boolean take(char c) {
        skipSpaces();
        if (at < text.length() && text.charAt(at) == c) {
            at++;
            return true;
        }
        return false;
    }

    /** Takes {@code word} if it stands next, as a whole word ({@code deg}, not the start of {@code degrees}). */
    private boolean takeWord(String word) {
        skipSpaces();
        if (!text.startsWith(word, at)) return false;
        int end = at + word.length();
        if (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
            return false;
        }
        at = end;
        return true;
    }

    private void expect(char c) {
        if (!take(c)) throw error("'" + c + "' is missing");
    }

    private void skipSpaces() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean atEnd() {
        return at >= text.length();
    }

    private MathException error(String message) {
        return new MathException(message + " at position " + (at + 1) + " of '" + text.strip() + "'");
    }

    // ---- nodes with a name ----

    /** An angle written in degrees: its value is in radians, unless a degree function takes it as it is. */
    private record InDegrees(Node degrees) implements Node {
        @Override
        public double eval(Env env) {
            return StrictMath.toRadians(degrees.eval(env));
        }
    }

    /** A symbol of the story, read as a number, or as a list where an aggregate takes it. */
    private record SymbolNode(String name) implements Node {
        @Override
        public double eval(Env env) {
            List<Double> values = evalList(env);
            if (values.size() != 1) {
                throw new MathException(name + " holds a list; use it where a list goes, e.g. sum(" + name + ")");
            }
            return values.getFirst();
        }

        @Override
        public List<Double> evalList(Env env) {
            Object value = env.symbol(name).orElseThrow(() -> new MathException(name + " isn't set"));
            return switch (value) {
                case Integer i -> List.of(i.doubleValue());
                case Long l -> List.of(l.doubleValue());
                case Double d -> List.of(d);
                case Float f -> List.of(Double.parseDouble(f.toString()));   // the float as the story sees it
                case Number n -> List.of(Numbers.parse(n.toString()));
                case String s -> parseText(s);
                default -> throw new MathException(name + " is " + value + ", not a number");
            };
        }

        private List<Double> parseText(String text) {
            try {
                if (!text.contains(",")) return List.of(Numbers.parse(text));
                List<Double> values = new ArrayList<>();
                for (String part : text.split(",")) {
                    if (!part.isBlank()) values.add(Numbers.parse(part));
                }
                return values;
            } catch (MathException e) {
                throw new MathException(name + " is '" + text.strip() + "', not a number or a list of numbers");
            }
        }
    }
}
