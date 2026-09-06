package liquidjava.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.context.ContextInspector;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.verification.VerifyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
class ContextToolsTest {
    private final LiquidJavaVerifier verifier = new LiquidJavaVerifier();
    private final ContextInspector inspector = new ContextInspector();
    private final GetLocalsTool locals = new GetLocalsTool(inspector, McpJsonDefaults.getMapper());
    private final GetGlobalsTool globals = new GetGlobalsTool(inspector, McpJsonDefaults.getMapper());
    private static final String FILE = "src/test/resources/examples/Context.java";

    private static Map<String, Object> globalArguments(String file) {
        return Map.of("path", file, "file", file);
    }

    @Test
    void filtersScopesAndKeepsRefinementsAndInternalNames() {
        var inside = localVariables(10, 28);
        assertTrue(inside.stream().anyMatch(v -> v.get("name").equals("nested")), inside.toString());
        var input = inside.stream().filter(v -> v.get("internalName").equals("input")).findFirst().orElseThrow();
        assertEquals("int", input.get("type"));
        assertTrue(input.get("refinement").toString().contains("> 0"), input.toString());
        assertFalse(localVariables(12, 9).stream().anyMatch(v -> v.get("name").equals("nested")));
        assertTrue(localVariables(3, 1).isEmpty());
        assertFalse(localVariables(16, 5).stream().anyMatch(v -> List.of("input", "first", "nested", "last").contains(v.get("name"))));
    }

    @Test
    void globalsAreFilteredByFileAndDoNotLeakAcrossCalls() {
        var first = globals.call(globalArguments(FILE));
        assertFalse(first.isError());
        var content = (Map<?, ?>) first.structuredContent();
        String file = Path.of(FILE).toAbsolutePath().normalize().toString();
        for (String key : List.of("ghosts", "states")) {
            assertTrue(((List<?>) content.get(key)).stream()
                    .allMatch(entry -> file.equals(((Map<?, ?>) entry).get("file"))));
        }
        var empty = (Map<?, ?>) globals.call(globalArguments("src/test/resources/examples/Valid.java")).structuredContent();
        for (String key : List.of("aliases", "ghosts", "states")) assertEquals(List.of(), empty.get(key));
        assertFalse(CommandLineLauncher.cmdArgs.lspMode);
        assertTrue(verifier.verify(new VerifyRequest("src/test/resources/examples/Valid.java", false)).success());
        assertEquals(first.structuredContent(), globals.call(globalArguments(FILE)).structuredContent());
    }

    @Test
    void missingPathsReturnInputErrors(@TempDir Path temporary) {
        String missing = temporary.resolve("missing.java").toString();
        for (var response : List.of(
                Map.entry(globals.call(Map.of("path", missing)), globals.specification().tool().outputSchema()),
                Map.entry(globals.call(Map.of("path", missing, "file", FILE)), globals.specification().tool().outputSchema()),
                Map.entry(locals.call(Map.of("path", missing, "file", FILE, "line", 1, "column", 1)), locals.specification().tool().outputSchema()))) {
            var result = response.getKey();
            assertTrue(result.isError(), result.toString());
            var content = (Map<?, ?>) result.structuredContent();
            assertEquals(Map.of("code", "INVALID_INPUT", "message", "The path " + missing + " was not found"),
                    content.get("error"));
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(response.getValue(), content).valid());
        }
    }

    @Test
    void verificationErrorsReturnContextWithoutDiagnostics() {
        for (String example : List.of("Invalid.java", "Warning.java")) {
            String file = "src/test/resources/examples/" + example;
            for (var response : List.of(
                    Map.entry(locals.call(Map.of("path", file, "file", file, "line", 6, "column", 5)), locals.specification().tool().outputSchema()),
                    Map.entry(globals.call(globalArguments(file)), globals.specification().tool().outputSchema()))) {
                var result = response.getKey();
                assertFalse(result.isError());
                var content = (Map<?, ?>) result.structuredContent();
                assertFalse(content.containsKey("errors"));
                assertFalse(content.containsKey("warnings"));
                assertTrue(McpJsonDefaults.getSchemaValidator().validate(response.getValue(), content).valid());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void rejectsInvalidArguments(Map<String, Object> arguments) {
        for (var response : List.of(
                Map.entry(locals.call(arguments), locals.specification().tool().outputSchema()),
                Map.entry(globals.call(arguments), globals.specification().tool().outputSchema()))) {
            var result = response.getKey();
            assertTrue(result.isError(), String.valueOf(arguments));
            var content = (Map<?, ?>) result.structuredContent();
            assertFalse(content.containsKey("errors"));
            assertFalse(content.containsKey("warnings"));
            assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(response.getValue(), content).valid());
        }
    }

    static Stream<Map<String, Object>> invalidArguments() {
        return Stream.of(null, Map.of(), Map.of("path", FILE, "file", FILE, "line", 1),
                Map.of("path", FILE, "file", FILE, "line", 1, "column", 1, "extra", true),
                Map.of("path", FILE, "file", FILE, "line", 0, "column", 1),
                Map.of("path", FILE, "file", FILE, "line", 1.5, "column", 1),
                Map.of("path", FILE, "file", FILE, "line", 2147483648L, "column", 1),
                Map.of("path", FILE, "file", FILE, "line", "1", "column", 1),
                Map.of("path", FILE, "file", "missing.java", "line", 1, "column", 1),
                Map.of("path", FILE, "file", "src/test/resources/examples", "line", 1, "column", 1),
                Map.of("path", FILE, "file", "a\u0000b", "line", 1, "column", 1));
    }

    @Test
    void globalsIncludeDefinitionsWithoutMethods() {
        var result = globals.call(globalArguments("src/test/resources/examples/Definitions.java"));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(1, ((List<?>) content.get("aliases")).size());
        assertEquals(List.of("count"), ((List<Map<?, ?>>) content.get("ghosts")).stream()
                .map(ghost -> ghost.get("name")).toList());
        assertEquals(2, ((List<?>) content.get("states")).size());
    }

    @Test
    void globalsUsePathForVerificationAndFileForFiltering() {
        String path = "src/test/resources/examples/globals";
        String file = path + "/First.java";
        var result = globals.call(Map.of("path", path, "file", file));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(Set.of("FirstAlias", "SecondAlias"), ((List<Map<?, ?>>) content.get("aliases")).stream()
                .map(alias -> (String) alias.get("name")).collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of("firstGhost"), ((List<Map<?, ?>>) content.get("ghosts")).stream()
                .map(ghost -> ghost.get("name")).toList());
        assertEquals(Set.of("firstOpen", "firstClosed"), ((List<Map<?, ?>>) content.get("states")).stream()
                .map(state -> (String) state.get("name")).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("path"), Set.copyOf((List<?>) globals.specification().tool().inputSchema()
                .get("required")));
        assertEquals(Set.of("path", "file"), ((Map<?, ?>) globals.specification().tool().inputSchema()
                .get("properties")).keySet());
    }

    @Test
    void globalsWithoutFileReturnAllGhostsAndStates() {
        String path = "src/test/resources/examples/globals";
        var result = globals.call(Map.of("path", path));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(Set.of("FirstAlias", "SecondAlias"), ((List<Map<?, ?>>) content.get("aliases")).stream()
                .map(alias -> (String) alias.get("name")).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("firstGhost", "secondGhost"), ((List<Map<?, ?>>) content.get("ghosts")).stream()
                .map(ghost -> (String) ghost.get("name")).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("firstOpen", "firstClosed", "secondOpen", "secondClosed"),
                ((List<Map<?, ?>>) content.get("states")).stream()
                        .map(state -> (String) state.get("name")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void localsUsePathForVerificationAndFileForFiltering() {
        String path = "src/test/resources/examples/globals";
        String file = path + "/First.java";
        var result = locals.call(Map.of("path", path, "file", file, "line", 10, "column", 26));
        assertFalse(result.isError());
        assertTrue(((List<Map<?, ?>>) ((Map<?, ?>) result.structuredContent()).get("variables")).stream()
                .anyMatch(variable -> variable.get("name").equals("local")));
        assertEquals(Set.of("path", "file", "line", "column"),
                Set.copyOf((List<?>) locals.specification().tool().inputSchema().get("required")));
    }

    @Test
    void localsRequirePosition() {
        assertTrue(locals.call(Map.of("path", FILE, "file", FILE)).isError());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> localVariables(int line, int column) {
        var result = locals.call(Map.of("path", FILE, "file", FILE, "line", line, "column", column));
        assertFalse(result.isError(), result.toString());
        return (List<Map<String, Object>>) ((Map<?, ?>) result.structuredContent()).get("variables");
    }
}
