package liquidjava.mcp.tools.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetStateMachineToolTest {
    private final GetStateMachineTool tool = new GetStateMachineTool(McpJsonDefaults.getMapper());

    @Test
    void returnsStateMachineParsedByLiquidJavaFsm() throws Exception {
        var result = tool.call(Map.of("path", "src/test/resources/examples/StateMachine.java"));

        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        var stateMachine = (Map<?, ?>) content.get("stateMachine");
        assertEquals("examples.StateMachine", stateMachine.get("className"));
        assertEquals(List.of("open", "closed"), stateMachine.get("states"));
        var initialTransition = (Map<?, ?>) ((List<?>) stateMachine.get("initialTransitions")).getFirst();
        assertEquals("open", initialTransition.get("to"));
        assertNull(initialTransition.get("toCondition"));
        assertEquals(2, ((List<?>) stateMachine.get("transitions")).size());
        var transition = (Map<?, ?>) ((List<?>) stateMachine.get("transitions")).getFirst();
        assertNull(transition.get("fromCondition"));
        assertNull(transition.get("toCondition"));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.specification().tool().outputSchema(), content).valid());
        assertTrue(result.content().isEmpty());
    }

    @Test
    void returnsNullForFilesWithoutAStateMachine() {
        var result = tool.call(Map.of("path", "src/test/resources/examples/Valid.java"));

        assertFalse(result.isError());
        assertNull(((Map<?, ?>) result.structuredContent()).get("stateMachine"));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(
            tool.specification().tool().outputSchema(), result.structuredContent()).valid());
    }

    @Test
    void rejectsDirectoriesMissingFilesAndNonJavaFiles() throws Exception {
        for (String path : List.of("src/test/resources/examples", "missing.java", "README.md")) {
            var result = tool.call(Map.of("path", path));
            assertTrue(result.isError(), path);
            var content = (Map<?, ?>) result.structuredContent();
            assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
            assertNull(content.get("stateMachine"));
        }
    }

    @Test
    void reportsParserFailuresAsVerifierErrors() {
        var result = tool.call(Map.of("path", "src/test/resources/examples/Malformed.java"));

        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals("VERIFIER_ERROR", ((Map<?, ?>) content.get("error")).get("code"));
        assertNull(content.get("stateMachine"));
    }
}
