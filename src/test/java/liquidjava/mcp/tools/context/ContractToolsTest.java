package liquidjava.mcp.tools.context;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
@ResourceLock("java.lang.System.err")
class ContractToolsTest {
    private static final String FILE = "src/test/resources/examples/Contracts.java";

    private final ContextInspector inspector = new ContextInspector();
    private final GetContractsTool contracts = new GetContractsTool(inspector, McpJsonDefaults.getMapper());

    @Test
    void returnsMethodsConstructorsParametersReturnsStatesAndLocations() throws Exception {
        var result = contracts.call(Map.of("path", FILE));

        assertFalse(result.isError(), result.toString());
        var content = (Map<?, ?>) result.structuredContent();
        var entries = entries(content);
        assertEquals(4, entries.size());

        var increment = entry(entries, "examples.Contracts.increment(int)");
        assertEquals("examples.Contracts", increment.get("className"));
        assertEquals("int", increment.get("returnType"));
        var parameter = ((List<Map<?, ?>>) increment.get("parameters")).getFirst();
        assertEquals("value", parameter.get("name"));
        assertEquals("int", parameter.get("type"));
        assertTrue(parameter.get("refinement").toString().contains("> 0"));
        assertTrue(increment.get("returnRefinement").toString().contains("value + 1"));
        assertNotNull(increment.get("location"));
        assertEquals(Path.of(FILE).toRealPath().toString(), ((Map<?, ?>) increment.get("location")).get("file"));

        var close = entry(entries, "examples.Contracts.close()");
        var transition = ((List<Map<?, ?>>) close.get("stateTransitions")).getFirst();
        assertTrue(transition.get("from").toString().contains("open"));
        assertTrue(transition.get("to").toString().contains("closed"));

        var constructor = entry(entries, "examples.Contracts.examples.Contracts()");
        assertNull(((List<Map<?, ?>>) constructor.get("stateTransitions")).getFirst().get("from"));
        assertTrue(((List<Map<?, ?>>) constructor.get("stateTransitions")).getFirst().get("to").toString()
                .contains("open"));
    }

    @Test
    void filtersByClassAndCompleteSignature() {
        var all = contracts.call(Map.of("path", FILE, "className", "examples.Contracts"));
        assertFalse(all.isError(), all.toString());
        assertEquals(4, entries((Map<?, ?>) all.structuredContent()).size());

        var overload = contracts.call(Map.of("path", FILE, "signature", "examples.Contracts.increment(long)"));
        assertFalse(overload.isError(), overload.toString());
        var overloads = entries((Map<?, ?>) overload.structuredContent());
        assertEquals(1, overloads.size());
        assertEquals("long", ((List<Map<?, ?>>) overloads.getFirst().get("parameters")).getFirst().get("type"));
        assertEquals(List.of(), entries((Map<?, ?>) contracts.call(Map.of(
                "path", FILE, "signature", "examples.Contracts.increment" )).structuredContent()));
    }

    @Test
    void unmatchedFiltersReturnEmptyContracts() {
        var result = contracts.call(Map.of("path", FILE, "className", "examples.Missing"));

        assertFalse(result.isError());
        assertEquals(List.of(), entries((Map<?, ?>) result.structuredContent()));
    }

    @Test
    void returnsExternalRefinementContracts() {
        var result = contracts.call(Map.of("path", "src/test/resources/examples/ExternalContracts.java"));

        assertFalse(result.isError(), result.toString());
        var external = entry(entries((Map<?, ?>) result.structuredContent()), "java.lang.Math.abs(int)");
        assertEquals("java.lang.Math", external.get("className"));
        assertEquals("int", external.get("returnType"));
        assertEquals(Path.of("src/test/resources/examples/ExternalContracts.java").toAbsolutePath().normalize().toString(),
                ((Map<?, ?>) external.get("location")).get("file"));
    }

    @Test
    void cachedQueriesDoNotLeakContractsAcrossPaths() {
        var first = contracts.call(Map.of("path", FILE));
        assertFalse(first.isError(), first.toString());

        var empty = contracts.call(Map.of("path", "src/test/resources/examples/Definitions.java"));
        assertFalse(empty.isError(), empty.toString());
        assertEquals(List.of(), entries((Map<?, ?>) empty.structuredContent()));

        var repeated = contracts.call(Map.of("path", FILE));
        assertFalse(repeated.isError(), repeated.toString());
        assertEquals(first.structuredContent(), repeated.structuredContent());
    }

    @Test
    void invalidInputUsesStructuredErrorContract() {
        var result = contracts.call(Map.of("path", FILE, "signature", " "));

        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(List.of(), entries(content));
        assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
        assertTrue(McpJsonDefaults.getSchemaValidator()
                .validate(contracts.specification().tool().outputSchema(), content).valid());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> entries(Map<?, ?> content) {
        return (List<Map<?, ?>>) content.get("contracts");
    }

    private static Map<?, ?> entry(List<Map<?, ?>> entries, String signature) {
        return entries.stream().filter(entry -> signature.equals(entry.get("signature"))).findFirst()
                .orElseThrow(() -> new AssertionError(entries.toString()));
    }
}
