package liquidjava.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
class GetDiagnosticsToolTest {
    private final GetDiagnosticsTool tool = new GetDiagnosticsTool(new LiquidJavaVerifier(), McpJsonDefaults.getMapper());

    private Map<?, ?> call(String... fixtures) throws Exception {
        var paths = java.util.Arrays.stream(fixtures)
                .map(name -> Path.of("src/test/resources/fixtures", name).toString()).toList();
        var result = tool.call(Map.of("paths", paths));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.specification().tool().outputSchema(), content).valid(), content.toString());
        assertEquals(content, McpJsonDefaults.getMapper().readValue(
                ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));
        assertFalse(content.containsKey("output"));
        assertFalse(content.containsKey("diagnostics"));
        assertNotNull(content.get("errors"));
        assertNotNull(content.get("warnings"));
        return content;
    }

    @Test
    void returnsRefinementsAndLocationsAndDoesNotLeakDiagnostics() throws Exception {
        var content = call("Invalid.java");
        assertEquals(false, content.get("success"));
        var diagnostic = (Map<?, ?>) ((List<?>) content.get("errors")).getFirst();
        assertEquals("RefinementError", diagnostic.get("type"));
        assertEquals("error", diagnostic.get("severity"));
        assertTrue(diagnostic.get("message").toString().contains("not a subtype"));
        var location = (Map<?, ?>) diagnostic.get("location");
        assertTrue(location.get("file").toString().endsWith("Invalid.java"));
        assertEquals(5, location.get("startLine"));
        assertTrue(((Map<?, ?>) diagnostic.get("refinements")).get("expected").toString().contains("> 0"));
        assertNotNull(((Map<?, ?>) diagnostic.get("refinements")).get("found"));
        assertEquals(Map.of("success", true, "errors", List.of(), "warnings", List.of()), call("Valid.java"));
    }

    @Test
    void exposesWarningsAndDiagnosticsWithoutLocations() throws Exception {
        var warningResult = call("Warning.java");
        assertEquals(true, warningResult.get("success"));
        var warning = (Map<?, ?>) ((List<?>) warningResult.get("warnings")).getFirst();
        assertEquals("warning", warning.get("severity"));
        assertEquals("UnsatisfiableRefinementWarning", warning.get("type"));
        assertNotNull(warning.get("refinements"));
        var missing = call("does-not-exist.java");
        assertEquals(false, missing.get("success"));
        var error = (Map<?, ?>) ((List<?>) missing.get("errors")).getFirst();
        assertEquals("CustomError", error.get("type"));
        assertFalse(error.containsKey("location"));
    }

    @Test
    void separatesErrorsAndWarningsFromTheSameRun() throws Exception {
        var content = call("Invalid.java", "Warning.java");
        assertEquals(false, content.get("success"));
        var errors = (List<?>) content.get("errors");
        var warnings = (List<?>) content.get("warnings");
        assertFalse(errors.isEmpty());
        assertFalse(warnings.isEmpty());
        assertTrue(errors.stream().allMatch(value -> "error".equals(((Map<?, ?>) value).get("severity"))));
        assertTrue(warnings.stream().allMatch(value -> "warning".equals(((Map<?, ?>) value).get("severity"))));
    }

    @Test
    void verifiesFoldersAndMultiplePathsTogether() throws Exception {
        assertEquals(call("joint"), call("joint/Contract.java", "joint/Caller.java"));
    }

    @Test
    void exposesCounterexamplesHintsAndDeclarationLocations() throws Exception {
        var content = call("Counterexample.java");
        var diagnostic = (Map<?, ?>) ((List<?>) content.get("errors")).getFirst();
        var assignments = (List<?>) diagnostic.get("counterexample");
        assertNotNull(assignments, diagnostic.toString());
        assertFalse(assignments.isEmpty());
        var assignment = (Map<?, ?>) assignments.getFirst();
        assertNotNull(assignment.get("variable"));
        assertNotNull(assignment.get("value"));
        assertTrue(diagnostic.get("hint").toString().contains("Counterexample:"));
        assertNotNull(diagnostic.get("declarationLocation"));
    }

    @Test
    void executionFailuresUseTheStructuredErrorContract() {
        var failing = new GetDiagnosticsTool(request -> liquidjava.mcp.verification.VerifyResult.failed(
                liquidjava.mcp.verification.VerifyResult.ErrorCode.VERIFIER_ERROR, "failed", "partial output"),
                McpJsonDefaults.getMapper());
        var result = failing.call(Map.of("paths", List.of("Example.java")));
        assertTrue(result.isError());
        assertEquals(Map.of("success", false, "errors", List.of(), "warnings", List.of(),
                "error", Map.of("code", "VERIFIER_ERROR", "message", "failed")), result.structuredContent());
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(
                failing.specification().tool().outputSchema(), result.structuredContent()).valid());
    }

    @Test
    void rejectsInvalidInputBeforeRunningVerifier() {
        var rejecting = new GetDiagnosticsTool(request -> fail("must not run"), McpJsonDefaults.getMapper());
        for (var arguments : List.of(Map.<String, Object>of(), Map.<String, Object>of("paths", List.of()),
                Map.<String, Object>of("paths", List.of(" ")), Map.<String, Object>of("paths", List.of(1)),
                Map.<String, Object>of("paths", List.of("a.java"), "debug", true),
                Map.<String, Object>of("paths", List.of("a.java"), "extra", true))) {
            var result = rejecting.call(arguments);
            assertTrue(result.isError());
            var content = (Map<?, ?>) result.structuredContent();
            assertEquals(List.of(), content.get("errors"));
            assertEquals(List.of(), content.get("warnings"));
            assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
        }
    }
}
