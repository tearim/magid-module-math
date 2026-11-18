package com.zarterstein.magid.math.engine;

import com.zarterstein.magid.ParametrizedInvocation;
import com.zarterstein.magid.SceneStager;
import com.zarterstein.magid.StoryCommands;
import com.zarterstein.magid.api.reveries.Reverie;
import com.zarterstein.magid.api.tags.TagException;
import com.zarterstein.magidml.XmlBundleLoader;
import com.zarterstein.magidml.tagpacks.PackDiscovery;
import com.zarterstein.magidml.tagpacks.PackReport;
import com.zarterstein.magidml.tagpacks.PackSource;
import com.zarterstein.magidml.tagpacks.TagPack;
import com.zarterstein.magidml.tagpacks.TagPackRegistry;
import com.zarterstein.storyserver.plugins.PluginDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pack in the real engine, as a server runs it: this module's {@code target/classes} is copied into a plugin
 * directory as a directory pack, read by {@link PluginDirectory} (the pack gets its own class loader), checked and
 * installed by {@link PackDiscovery}, and the stories in {@code src/engine-test/resources/stories} are played.
 * <p>
 * Runs in the Maven profile {@code engine}, which turns itself on where the engine is in the local Maven repository.
 * </p>
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class MathStoryTest {

    private static final Path PACK_BUILD = Path.of("target/classes");
    private static final Path STORIES = Path.of("src/engine-test/resources/stories");
    private static final String PACK_ID = "magid-math";

    @TempDir
    Path plugins;

    private Map<String, TagPack> savedPacks;
    private PluginDirectory directory;
    private SceneStager stager;

    @BeforeEach
    void installPack() throws IOException {
        savedPacks = TagPackRegistry.getTagPacks();
        copyPackInto(plugins.resolve(PACK_ID));
        directory = PluginDirectory.open(plugins, MathStoryTest.class.getClassLoader());
        List<PackSource> sources = directory.tagPacks().stream()
                .map(p -> new PackSource(p.location().toString(), p.attributes(), directory.packLoader(p),
                        p.classNames()))
                .toList();
        List<PackReport> reports = PackDiscovery.discover(sources);
        PackDiscovery.requireValid(reports);
        PackDiscovery.install(reports);
        XmlBundleLoader.suppressInfoLogs();
    }

    @AfterEach
    void uninstallPack() throws IOException {
        TagPackRegistry.setTagPacks(savedPacks);
        if (directory != null) directory.close();
    }

    /** Copies {@code target/classes} to {@code <pack>/classes}: the pack in its directory form. */
    private static void copyPackInto(Path pack) throws IOException {
        assertTrue(Files.isRegularFile(PACK_BUILD.resolve("META-INF/MANIFEST.MF")),
                "precondition: the pack is built, with its manifest in target/classes");
        Path classes = pack.resolve("classes");
        try (Stream<Path> files = Files.walk(PACK_BUILD)) {
            for (Path file : files.toList()) {
                Path target = classes.resolve(PACK_BUILD.relativize(file).toString());
                if (Files.isDirectory(file)) Files.createDirectories(target);
                else Files.copy(file, target);
            }
        }
    }

    private void load(String story) {
        stager = new SceneStager();
        new XmlBundleLoader(STORIES.resolve(story).toAbsolutePath().toString(), "localhost", stager)
                .parseXmlAndPrepareServer();
        stager.jumpTo("start");
    }

    /** Plays a command (empty: show the scene) and returns the turn's payload as text. */
    private String play(String command) {
        Object payload = stager.play(StoryCommands.singleRun(new ParametrizedInvocation(command, new HashMap<>())))
                .payload();
        return payload == null ? "" : payload.toString();
    }

    private Object symbol(String name) {
        return stager.symbols().recall(name).map(Reverie::get).orElse(null);
    }

    // ---- discovery ----

    @Test
    @DisplayName("The engine accepts the pack: 61 tags under the prefix math")
    void packIsDiscovered() {
        TagPack pack = TagPackRegistry.getTagPack(PACK_ID);

        assertNotNull(pack);
        assertEquals("math", pack.getPrefix());
        assertEquals(61, pack.getTagDefinitions().size());
    }

    @Test
    @DisplayName("The pack's classes come from its own loader, not the test class path")
    void classesFromPackLoader() {
        Class<?> calc = TagPackRegistry.getTagPack(PACK_ID).getTag("calc").tagClass();

        assertSame(directory.packLoader(directory.tagPacks().getFirst()), calc.getClassLoader());
    }

    // ---- in a description ----

    @Test
    @DisplayName("<math-calc> prints inline: 2 + 3 * 4 is 14")
    void calcInline() {
        load("mathtest.xml");

        assertTrue(play("").contains("CALC: 14"), play(""));
    }

    @Test
    @DisplayName("Every digit of the double prints: 0.1 + 0.2")
    void allDigits() {
        load("mathtest.xml");

        assertTrue(play("").contains("SUM: 0.30000000000000004"), play(""));
    }

    @Test
    @DisplayName("sin(30°) is exactly 0.5")
    void sinDegrees() {
        load("mathtest.xml");

        assertTrue(play("").contains("SIN: 0.5\\r"), play(""));   // the payload is JSON: the line break is escaped
    }

    @Test
    @DisplayName("sin(30deg) is exactly 0.5, as sin(30°)")
    void sinDeg() {
        load("mathtest.xml");

        assertTrue(play("").contains("SINDEG: 0.5\r"), play(""));
    }

    @Test
    @DisplayName("A formula reads an actor's memory: player.gold * 1.5 is 10.5")
    void readsMemory() {
        load("mathtest.xml");

        assertTrue(play("").contains("GOLD: 10.5"), play(""));
    }

    @Test
    @DisplayName("{…} in a formula is expanded by the engine first: {player.gold} + 1 is 8")
    void bracesExpanded() {
        load("mathtest.xml");

        assertTrue(play("").contains("BRACES: 8"), play(""));
    }

    @Test
    @DisplayName("A function tag with digits: <math-sqrt x=\"2\" digits=\"3\"/> prints 1.414")
    void functionTagDigits() {
        load("mathtest.xml");

        assertTrue(play("").contains("SQRT: 1.414"), play(""));
    }

    @Test
    @DisplayName("An aggregate over a list with an expanded memory: mean(1, 2, 7)")
    void aggregate() {
        load("mathtest.xml");

        assertTrue(play("").contains("MEAN: 3.3333333333333335"), play(""));
    }

    // ---- in actions ----

    @Test
    @DisplayName("A stored result keeps every bit in the engine's symbols, and the next tag reads it exactly")
    void precisionKept() {
        load("mathtest.xml");

        String turn = play("keep");

        assertEquals("0.30000000000000004", symbol("$x"));
        assertTrue(turn.contains("0,6000000000000001"), turn);
    }

    @Test
    @DisplayName("A list declared by the story spreads into an aggregate: mean(3, 5, 6, 2) is 4")
    void storedList() {
        load("mathtest.xml");

        String turn = play("list");

        assertEquals(4, symbol("$avg"));
        assertTrue(turn.contains("\"4\""), turn);
    }

    @Test
    @DisplayName("Division by zero stops the story with the tag's name")
    void errorThrows() {
        load("mathtest.xml");

        TagException e = assertThrows(TagException.class, () -> play("bad"));
        assertTrue(e.getMessage().startsWith("math-div: division by zero"), e.getMessage());
    }

    @Test
    @DisplayName("With silent errors, division by zero keeps its message in $math_last_error, past the scene's re-rendering")
    void errorSilent() {
        load("mathtest.xml");

        play("quiet");

        assertEquals("math-div: division by zero", symbol("$math_last_error"));
    }

    @Test
    @DisplayName("<math-clear-error/>, a failing tag, then <math-last-error/> narrates the error")
    void lastErrorInAction() {
        load("mathtest.xml");

        String turn = play("checked");

        assertTrue(turn.contains("math-div: division by zero"), turn);
    }

    @Test
    @DisplayName("After <math-clear-error/>, a tag that succeeds leaves <math-last-error none> to print its text")
    void clearedErrorInAction() {
        load("mathtest.xml");

        String turn = play("fine");

        assertTrue(turn.contains("no error"), turn);
        assertEquals("0.5", symbol("$half"));
    }

    // ---- under a story's prefix ----

    @Test
    @DisplayName("Under <extend as=\"m\">, <m-sqrt x=\"16\"/> prints 4")
    void otherPrefix() {
        load("mathprefix.xml");

        assertTrue(play("").contains("ROOT: 4"), play(""));
    }

    @Test
    @DisplayName("Under <extend as=\"m\">, <m-sind x=\"150\"/> prints 0.5")
    void otherPrefixDegrees() {
        load("mathprefix.xml");

        assertTrue(play("").contains("SIND: 0.5"), play(""));
    }
}
