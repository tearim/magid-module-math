package com.zarterstein.magid.math;

import com.zarterstein.magid.api.expressions.EvaluatorInterface;
import com.zarterstein.magid.api.reveries.Reverie;
import com.zarterstein.magid.api.som.StoryObjectModel;
import com.zarterstein.magid.api.symbols.ErrorReactionLevel;
import com.zarterstein.magid.api.symbols.SymbolPinax;
import com.zarterstein.magid.api.symbols.SymbolRef;
import com.zarterstein.magid.api.tags.MagidTag;
import com.zarterstein.magid.api.tags.Outcome;
import com.zarterstein.magid.api.tags.SourcePosition;
import com.zarterstein.magid.api.tags.TagContext;
import com.zarterstein.magid.api.tags.TagElement;
import com.zarterstein.magid.api.tags.TagNode;
import com.zarterstein.magid.api.tags.TagProcessorResult;
import com.zarterstein.magid.api.tags.TagProcessorResultInterface;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A story without an engine, for the tags' tests: symbols in a map, an element from a name, attributes and text, and
 * a context whose evaluator leaves text as it is.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Fakes {

    private Fakes() {
    }

    /**
     * The story's symbols in a map, stored as the engine stores them: a reverie as it is, numeric text cast to an
     * integer or a float (as {@code ReverieCasting.castToClosestType} does), anything else as text.
     */
    static final class Symbols implements SymbolPinax {
        final Map<String, Reverie<?>> values = new HashMap<>();

        @Override
        public Optional<Reverie<?>> recall(String symbol) {
            return Optional.ofNullable(values.get(symbol));
        }

        @Override
        public void set(String symbol, Object value) {
            values.put(symbol, switch (value) {
                case Reverie<?> r -> r;
                case Integer i -> Reverie.of(i);
                case Float f -> Reverie.of(f);
                case Boolean b -> Reverie.of(b);
                case String s when s.strip().matches("[+-]?\\d+") -> Reverie.of(Integer.parseInt(s.strip()));
                case String s when s.strip().matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?") ->
                        Reverie.of(Float.parseFloat(s.strip()));
                default -> Reverie.of(String.valueOf(value));
            });
        }

        @Override
        public void declare(String symbol, Object value) {
            set(symbol, value);
        }

        @Override
        public void clear(String symbol) {
            values.remove(symbol);
        }

        @Override
        public boolean has(String symbol) {
            return values.containsKey(symbol);
        }

        @Override
        public SymbolRef info(String symbol) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markTemporary(String symbol, Scope scope) {
        }

        @Override
        public boolean isDynamicCastingAllowed() {
            return true;
        }

        @Override
        public boolean areNonDeclaredSymbolsAllowed() {
            return true;
        }

        @Override
        public ErrorReactionLevel getDynamicCastLevel() {
            return null;
        }

        @Override
        public ErrorReactionLevel getNonDeclaredLevel() {
            return null;
        }

        /** @return the symbol's value as text, or {@code null} if unset */
        String text(String symbol) {
            Reverie<?> value = values.get(symbol);
            return value == null ? null : String.valueOf(value.get());
        }
    }

    /**
     * An element as the engine gives it to a tag; its local name is its written name, as the engine reports it today.
     *
     * @param name       the written name, e.g. {@code math-sqrt}
     * @param attributes the attributes
     * @param text       the text inside
     */
    record Element(String name, Map<String, String> attributes, String text) implements TagElement {
        @Override
        public String localName() {
            return name;
        }

        @Override
        public String attribute(String attribute) {
            return attributes.getOrDefault(attribute, "");
        }

        @Override
        public boolean hasAttribute(String attribute) {
            return attributes.containsKey(attribute);
        }

        @Override
        public Set<String> attributeNames() {
            return attributes.keySet();
        }

        @Override
        public boolean isTrue(String attribute, boolean fallback) {
            if (!attributes.containsKey(attribute)) return fallback;
            return Set.of("true", "yes", "1", "enabled").contains(attributes.get(attribute).strip().toLowerCase());
        }

        @Override
        public List<TagNode> children() {
            return List.of();
        }

        @Override
        public SourcePosition position() {
            return SourcePosition.UNKNOWN;
        }

        @Override
        public SourcePosition position(String attribute) {
            return SourcePosition.UNKNOWN;
        }
    }

    /** A story run: its symbols and what its actions narrated. */
    static final class Story {
        final Symbols symbols = new Symbols();
        final List<Object> narrated = new ArrayList<>();
        private final TagContext context;

        Story() {
            StoryObjectModel som = (StoryObjectModel) Proxy.newProxyInstance(Fakes.class.getClassLoader(),
                    new Class<?>[]{StoryObjectModel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("narrate")) narrated.add(args[0]);
                        return null;
                    });
            EvaluatorInterface evaluator = new EvaluatorInterface() {
                public boolean isEnabled() { return true; }
                public boolean hasExpressions(String text) { return false; }
                public String evaluate(String text) { return text; }
                public Object run(String script) { return null; }
            };
            context = new TagContext() {
                public StoryObjectModel som() { return som; }
                public com.zarterstein.magid.api.symbols.SymbolPinax symbols() { return symbols; }
                public EvaluatorInterface evaluator() { return evaluator; }
                public String blockName() { return "description"; }
                public Outcome executeBlock(List<TagNode> nodes) { return Outcome.proceed(); }
                public TagProcessorResultInterface renderBlock(List<TagNode> nodes) { return TagProcessorResult.empty(); }
                public Logger logger() { return Logger.getLogger("magid-math-test"); }
            };
        }

        /**
         * Renders a tag as a narration block does.
         *
         * @param tag        a new tag
         * @param name       its written name
         * @param attributes alternating attribute names and values
         * @return the text it printed
         */
        String render(MagidTag tag, String name, String... attributes) {
            return renderWithText(tag, name, "", attributes);
        }

        /**
         * @param tag        a new tag
         * @param name       its written name
         * @param text       the text inside it
         * @param attributes alternating attribute names and values
         * @return the text it printed
         */
        String renderWithText(MagidTag tag, String name, String text, String... attributes) {
            Element element = element(name, text, attributes);
            tag.bind(element, context);
            return tag.process(element, "description").get(TagProcessorResult.TEXT);
        }

        /**
         * Runs a tag as an action does.
         *
         * @param tag        a new tag
         * @param name       its written name
         * @param text       the text inside it
         * @param attributes alternating attribute names and values
         * @return its outcome
         */
        Outcome execute(MagidTag tag, String name, String text, String... attributes) {
            tag.bind(element(name, text, attributes), context);
            return tag.execute();
        }

        private static Element element(String name, String text, String... attributes) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < attributes.length; i += 2) map.put(attributes[i], attributes[i + 1]);
            return new Element(name, map, text);
        }
    }
}
