package liquidjava.mcp.tools.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
class CheckSatisfiabilityToolTest {
    private final CheckSatisfiabilityTool tool = new CheckSatisfiabilityTool(new SmtChecker()::check, McpJsonDefaults.getMapper());

    private Map<?, ?> call(Map<String, Object> input, boolean error) {
        var result = tool.call(input);
        assertEquals(error, result.isError(), result.toString());
        var content = (Map<?, ?>) result.structuredContent();
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                tool.specification().tool().outputSchema(), content).valid(), content.toString());
        assertTrue(result.content().isEmpty());
        return content;
    }

    @Test
    void reportsSatisfiabilityAndAssignments() {
        var sat = call(query(Map.of("x", "int"), List.of("x == 11")), false);
        assertEquals("sat", sat.get("status"));
        assertEquals(List.of(Map.of("variable", "x", "value", "11")), sat.get("assignment"));

        var unsat = call(query(Map.of("x", "int"), List.of("x > 10", "x < 5")), false);
        assertEquals("unsat", unsat.get("status"));
        assertFalse(unsat.containsKey("assignment"));

        var unconstrained = call(query(Map.of(), List.of()), false);
        assertEquals("sat", unconstrained.get("status"));
        assertEquals(List.of(), unconstrained.get("assignment"));
    }

    @Test
    void supportsGhostArithmetic() {
        var ghosts = Map.of("size", Map.of(
                "parameterTypes", List.of("int"),
                "returnType", "int"));

        var sat = call(query(Map.of("x", "int"), ghosts,
                List.of("size(x) == 3", "size(x) + 1 == 4")), false);
        assertEquals("sat", sat.get("status"));
        assertTrue(((List<?>) sat.get("assignment")).contains(Map.of("variable", "size(x)", "value", "3")));

        var unsat = call(query(Map.of("x", "int"), ghosts,
                List.of("size(x) == 3", "size(x) + 1 == 5")), false);
        assertEquals("unsat", unsat.get("status"));

        var emptyVariables = call(Map.of(
                "variables", Map.of(),
                "ghosts", ghosts,
                "constraints", List.of("size(1) == 3", "size(1) + 1 == 4")), false);
        assertEquals("sat", emptyVariables.get("status"));
    }

    @Test
    void reusesValidityPredicateValidation() {
        for (String constraint : List.of("x >", "missing > 0", "x", "custom(x)", "old(x) == x")) {
            var content = call(query(Map.of("x", "int"), List.of(constraint)), true);
            assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"), constraint);
        }
        call(Map.of(), true);
        call(Map.of("ghosts", Map.of(), "constraints", List.of("true")), true);
        call(Map.of("variables", Map.of(), "constraints", List.of("true")), true);
        call(query(Map.of("x", "byte"), List.of()), false);
        call(Map.of(
                "variables", Map.of("x", "int"),
                "ghosts", Map.of("size", Map.of("parameterTypes", List.of("boolean"),
                        "returnType", "int")),
                "constraints", List.of("size(x) > 0")), true);
        call(Map.of(
                "variables", Map.of(),
                "ghosts", Map.of("size", Map.of("parameterTypes", List.of("int"))),
                "constraints", List.of("true")), true);
    }

    @Test
    void preservesUnknownSolverResults() {
        var unknownTool = new CheckSatisfiabilityTool(request -> SmtResult.unknown(),
                McpJsonDefaults.getMapper());
        var result = unknownTool.call(query(Map.of(), List.of("true")));
        assertFalse(result.isError());
        assertEquals("unknown", ((Map<?, ?>) result.structuredContent()).get("status"));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(unknownTool.specification().tool().outputSchema(), result.structuredContent()).valid());
    }

    private static Map<String, Object> query(Map<String, String> variables, List<String> constraints) {
        return Map.of("variables", variables, "ghosts", Map.of(), "constraints", constraints);
    }

    private static Map<String, Object> query(Map<String, String> variables, Map<String, ?> ghosts, List<String> constraints) {
        return Map.of("variables", variables, "ghosts", ghosts, "constraints", constraints);
    }
}
