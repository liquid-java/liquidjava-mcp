package liquidjava.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.validity.ValidityChecker;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.verification.VerifyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

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
        assertEquals(content, McpJsonDefaults.getMapper().readValue(
                ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));
        return content;
    }

    private Map<String, Object> query(Map<String, String> variables, List<String> assumptions, String conclusion) {
        return Map.of("variables", variables, "assumptions", assumptions, "conclusion", conclusion);
    }

    @Test
    void checksValidityAndCounterexamples() throws Exception {
        assertEquals("valid", call(query(Map.of("x", "int"), List.of("x > 0"), "x >= 0"), false).get("status"));
        var invalid = call(query(Map.of("x", "int"), List.of("x >= 0"), "x > 0"), false);
        assertEquals("invalid", invalid.get("status"));
        assertEquals(true, invalid.get("success"));
        assertEquals(List.of(Map.of("variable", "x", "value", "0")), invalid.get("counterexample"));
        assertEquals("valid", call(query(Map.of(), List.of(), "true"), false).get("status"));
        assertEquals("invalid", call(query(Map.of(), List.of(), "false"), false).get("status"));
        assertEquals("valid", call(query(Map.of("x", "int"), List.of("x > 0", "x < 0"), "false"), false).get("status"));
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
        call(query(Map.of("x", "byte"), List.of(), "true"), true);
        call(query(Map.of(), List.of("1"), "true"), true);
        call(Map.of(), true);
        call(null, true);
        call(query(Map.of(), List.of(), " "), true);
        call(query(Map.of(), List.of(" "), "true"), true);
        call(Map.of("variables", Map.of(), "assumptions", List.of(), "conclusion", "true", "extra", true), true);
        call(Map.of("variables", Map.of("x", 1), "assumptions", List.of(), "conclusion", "true"), true);
        call(Map.of("variables", Map.of(), "assumptions", List.of(1), "conclusion", "true"), true);
    }

    @Test
    void serializesUnknownAndExecutionErrors() {
        for (var outcome : List.of(liquidjava.mcp.validity.ValidityResult.unknown("incomplete theory"),
                liquidjava.mcp.validity.ValidityResult.failed("VERIFIER_ERROR", "solver unavailable"))) {
            var stub = new CheckValidityTool(request -> outcome, McpJsonDefaults.getMapper());
            var result = stub.call(query(Map.of(), List.of(), "true"));
            assertEquals(!outcome.success(), result.isError());
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                    stub.specification().tool().outputSchema(), result.structuredContent()).valid());
        }
    }

    @Test
    void ignoresAndPreservesExistingGlobalDeclarations() throws Exception {
        var context = liquidjava.processor.context.Context.getInstance();
        var globals = List.copyOf(context.getCtxGlobalVars());
        try {
            context.addGlobalVariableToContext("previousGlobal", "test",
                    new spoon.Launcher().getFactory().Type().INTEGER_PRIMITIVE, new liquidjava.rj_language.Predicate());
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
        var verifier = new LiquidJavaVerifier();
        var request = VerifyRequest.fromArguments(Map.of("paths", List.of("src/test/resources/fixtures/Valid.java")));
        assertTrue(verifier.verify(request).success());
        call(query(Map.of(), List.of(), "onlyHere == 0"), true);
        call(query(Map.of("onlyHere", "boolean"), List.of(), "onlyHere || !onlyHere"), false);
        assertTrue(verifier.verify(request).success());
    }
}
