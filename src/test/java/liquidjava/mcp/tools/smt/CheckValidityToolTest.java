package liquidjava.mcp.tools.smt;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpError;
import liquidjava.mcp.tools.verification.LiquidJavaVerifier;
import liquidjava.mcp.tools.verification.VerifyRequest;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import spoon.Launcher;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
class CheckValidityToolTest {
    private final CheckValidityTool tool = new CheckValidityTool(new ValidityChecker()::check, McpJsonDefaults.getMapper());

    private Map<?, ?> call(Map<String, Object> input, boolean error) throws Exception {
        var result = tool.call(input);
        assertEquals(error, result.isError(), result.toString());
        var content = (Map<?, ?>) result.structuredContent();
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                tool.specification().tool().outputSchema(), content).valid(), content.toString());
        assertTrue(result.content().isEmpty());
        return content;
    }

    private Map<String, Object> query(Map<String, String> variables, List<String> assumptions, String conclusion) {
        return Map.of("variables", variables, "ghosts", Map.of(),
                "assumptions", assumptions, "conclusion", conclusion);
    }

    @Test
    void checksValidityAndCounterexamples() throws Exception {
        assertEquals("valid", call(query(Map.of("x", "int"), List.of("x > 0"), "x >= 0"), false).get("status"));
        var invalid = call(query(Map.of("x", "int"), List.of("x >= 0"), "x > 0"), false);
        assertEquals("invalid", invalid.get("status"));
        assertEquals(List.of(Map.of("variable", "x", "value", "0")), invalid.get("counterexample"));
        assertEquals("valid", call(query(Map.of(), List.of(), "true"), false).get("status"));
        assertEquals("invalid", call(query(Map.of(), List.of(), "false"), false).get("status"));
        assertEquals("valid", call(query(Map.of("x", "int"), List.of("x > 0", "x < 0"), "false"), false).get("status"));
    }

    @Test
    void supportsIntegerValuedTypestateFunctions() throws Exception {
        var arguments = Map.<String, Object>of(
                "variables", Map.of("door", "example.Door"),
                "ghosts", Map.of("state0", Map.of(
                        "parameterTypes", List.of("example.Door"),
                        "returnType", "int")),
                "assumptions", List.of("state0(door) == 0"),
                "conclusion", "door.state0() != 1");

        assertEquals("valid", call(arguments, false).get("status"));

        var emptyVariables = Map.<String, Object>of(
                "variables", Map.of(),
                "ghosts", Map.of("state0", Map.of(
                        "parameterTypes", List.of("int"),
                        "returnType", "int")),
                "assumptions", List.of("state0(1) == 0"),
                "conclusion", "state0(1) != 1");
        assertEquals("valid", call(emptyVariables, false).get("status"));
    }

    @Test
    void supportsTypesBooleanConditionsAndArrays() throws Exception {
        for (String type : List.of("short", "char", "int", "long", "float", "double"))
            assertEquals("valid", call(query(Map.of("x", type), List.of("x > 0"), "x >= 0"), false).get("status"));
        assertEquals("valid", call(query(Map.of("b", "boolean"), List.of("b"), "!(!b)"), false).get("status"));
        assertEquals("valid", call(query(Map.of("x", "int"), List.of("x == 2"), "(x > 0 ? x + 1 : 0) == 3"), false).get("status"));
    }

    @Test
    void rejectsInvalidPredicatesAndDeclarations() throws Exception {
        for (String predicate : List.of("x >", "missing > 0", "x", "1", "1 && true", "x == true",
                "custom(x)", "Positive(x)", "old(x) == x", "_ > 0", "this == this",
                "length(x) > 0", "getFromIndex(x, 0) == 0", "length() > 0", "int other",
                "Integer.MAX_VALUE > 0", "true ? 1 : 0")) {
            var content = call(query(Map.of("x", "int"), List.of(), predicate), true);
            assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"), predicate);
        }
        for (String name : List.of("", "bad name", "true", "int", "_", "this", "old", "X", "x.y"))
            call(query(Map.of(name, "int"), List.of(), "true"), true);
        call(query(Map.of("x", "byte"), List.of(), "true"), false);
        call(query(Map.of(), List.of("1"), "true"), true);
        call(Map.of(), true);
        call(Map.of("ghosts", Map.of(), "assumptions", List.of(), "conclusion", "true"), true);
        call(Map.of("variables", Map.of(), "assumptions", List.of(), "conclusion", "true"), true);
        call(null, true);
        call(query(Map.of(), List.of(), " "), true);
        call(query(Map.of(), List.of(" "), "true"), true);
        call(Map.of("variables", Map.of(), "assumptions", List.of(), "conclusion", "true", "extra", true), true);
        call(Map.of("variables", Map.of("x", 1), "assumptions", List.of(), "conclusion", "true"), true);
        call(Map.of("variables", Map.of(), "assumptions", List.of(1), "conclusion", "true"), true);
    }

    @Test
    void serializesExecutionErrors() {
        for (var outcome : List.of(ValidityResult.failed(McpError.Code.VERIFIER_ERROR, "solver unavailable"))) {
            var stub = new CheckValidityTool(request -> outcome, McpJsonDefaults.getMapper());
            var result = stub.call(query(Map.of(), List.of(), "true"));
            assertEquals(outcome.error() != null, result.isError());
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                    stub.specification().tool().outputSchema(), result.structuredContent()).valid());
        }
    }

    @Test
    void serializesUnknownSolverResults() {
        var stub = new CheckValidityTool(request -> ValidityResult.unknown(), McpJsonDefaults.getMapper());
        var result = stub.call(query(Map.of(), List.of(), "true"));
        assertFalse(result.isError());
        assertEquals("unknown", ((Map<?, ?>) result.structuredContent()).get("status"));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                stub.specification().tool().outputSchema(), result.structuredContent()).valid());
    }

    @Test
    void ignoresAndPreservesExistingGlobalDeclarations() throws Exception {
        var context = Context.getInstance();
        var globals = List.copyOf(context.getCtxGlobalVars());
        try {
            context.addGlobalVariableToContext("previousGlobal", "test",
                    new Launcher().getFactory().Type().INTEGER_PRIMITIVE, new Predicate());
            call(query(Map.of(), List.of(), "previousGlobal == 0"), true);
            call(query(Map.of("previousGlobal", "boolean"), List.of(), "previousGlobal || !previousGlobal"), false);
            assertEquals("int", context.getContext().get("previousGlobal").getQualifiedName());
        } finally {
            context.getCtxGlobalVars().clear();
            context.getCtxGlobalVars().addAll(globals);
        }
    }

    @Test
    void doesNotLeakAcrossRequestsOrVerification() throws Exception {
        call(query(Map.of("onlyHere", "int"), List.of(), "onlyHere == onlyHere"), false);
        call(query(Map.of(), List.of(), "onlyHere == 0"), true);
        call(Map.of(
                "variables", Map.of("x", "int"),
                "ghosts", Map.of("onlyHere", Map.of("parameterTypes", List.of("int"),
                        "returnType", "int")),
                "assumptions", List.of(),
                "conclusion", "onlyHere(x) == onlyHere(x)"), false);
        call(query(Map.of("x", "int"), List.of(), "onlyHere(x) == 0"), true);
        var verifier = new LiquidJavaVerifier();
        var request = VerifyRequest.fromArguments(Map.of("path", "src/test/resources/examples/Valid.java"));
        assertTrue(verifier.verify(request).success());
        call(query(Map.of(), List.of(), "onlyHere == 0"), true);
        call(query(Map.of("onlyHere", "boolean"), List.of(), "onlyHere || !onlyHere"), false);
        assertTrue(verifier.verify(request).success());
    }
}
