package liquidjava.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.context.ContextInspector;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.verification.VerifyRequest;
import org.junit.jupiter.api.Test;
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
    private static final String FILE = "src/test/resources/fixtures/Context.java";

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
    void globalsAreIndependentOfPositionAndDoNotLeakAcrossCalls() {
        var first = globals.call(Map.of("file", FILE));
        var positioned = globals.call(Map.of("file", FILE, "line", 12, "column", 9));
        assertFalse(first.isError());
        assertEquals(first.structuredContent(), positioned.structuredContent());
        var empty = (Map<?, ?>) globals.call(Map.of("file", "src/test/resources/fixtures/Valid.java")).structuredContent();
        for (String key : List.of("aliases", "ghosts", "states")) assertEquals(List.of(), empty.get(key));
        assertFalse(CommandLineLauncher.cmdArgs.lspMode);
        assertTrue(verifier.verify(new VerifyRequest(List.of("src/test/resources/fixtures/Valid.java"))).success());
        assertEquals(first.structuredContent(), globals.call(Map.of("file", FILE)).structuredContent());
    }

    @Test
    void verificationErrorsReturnStatusWithoutDiagnostics() {
        for (String fixture : List.of("Invalid.java", "Warning.java")) {
            var arguments = Map.<String, Object>of("file", "src/test/resources/fixtures/" + fixture, "line", 6, "column", 5);
            for (var response : List.of(
                    Map.entry(locals.call(arguments), locals.specification().tool().outputSchema()),
                    Map.entry(globals.call(arguments), globals.specification().tool().outputSchema()))) {
                var result = response.getKey();
                assertFalse(result.isError());
                var content = (Map<?, ?>) result.structuredContent();
                assertEquals(fixture.equals("Warning.java"), content.get("success"));
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
        return Stream.of(null, Map.of(), Map.of("file", FILE, "line", 1),
                Map.of("file", FILE, "line", 1, "column", 1, "extra", true),
                Map.of("file", FILE, "line", 0, "column", 1),
                Map.of("file", FILE, "line", 1.5, "column", 1),
                Map.of("file", FILE, "line", 2147483648L, "column", 1),
                Map.of("file", FILE, "line", "1", "column", 1),
                Map.of("file", "missing.java", "line", 1, "column", 1),
                Map.of("file", "src/test/resources/fixtures", "line", 1, "column", 1),
                Map.of("file", "a\u0000b", "line", 1, "column", 1));
    }

    @Test
    void globalsIncludeDefinitionsWithoutMethods() {
        var result = globals.call(Map.of("file", "src/test/resources/fixtures/Definitions.java"));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(1, ((List<?>) content.get("aliases")).size());
        assertEquals(2, ((List<?>) content.get("ghosts")).size());
        assertEquals(2, ((List<?>) content.get("states")).size());
    }

    @Test
    void localsRequirePosition() {
        assertTrue(locals.call(Map.of("file", FILE)).isError());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> localVariables(int line, int column) {
        var result = locals.call(Map.of("file", FILE, "line", line, "column", column));
        assertFalse(result.isError(), result.toString());
        return (List<Map<String, Object>>) ((Map<?, ?>) result.structuredContent()).get("variables");
    }
}
