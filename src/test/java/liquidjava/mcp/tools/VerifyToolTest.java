package liquidjava.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import liquidjava.mcp.verification.VerifyRequest;
import liquidjava.mcp.verification.VerifyResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class VerifyToolTest {
    private static final TypeRef<Map<String, Object>> MAP = new TypeRef<>() {};

    @Test
    void passesAllPathsTogetherWithoutChangingThem() throws Exception {
        var received = new AtomicReference<VerifyRequest>();
        var tool = new VerifyTool(request -> {
            received.set(request);
            return VerifyResult.completed(true, "Correct! Passed Verification.\n");
        }, McpJsonDefaults.getMapper());
        List<String> paths = List.of("file with spaces.java", "folder/../folder", "file with spaces.java");
        var result = tool.call(Map.of("paths", paths));
        assertEquals(paths, received.get().paths());
        assertFalse(result.isError());
        assertEquals(Map.of("success", true, "output", "Correct! Passed Verification.\n"), result.structuredContent());
        assertEquals(result.structuredContent(), McpJsonDefaults.getMapper().readValue(((TextContent) result.content().getFirst()).text(), MAP));
    }

    @Test
    void verificationFailureIsNotAToolExecutionError() {
        var tool = new VerifyTool(
            request -> VerifyResult.completed(false, "Refinement Error"),
            McpJsonDefaults.getMapper()
        );
        var result = tool.call(Map.of("paths", List.of("Example.java")));
        assertFalse(result.isError());
        assertEquals(Map.of("success", false, "output", "Refinement Error"), result.structuredContent());
    }

    @Test
    void executionErrorIncludesPartialOutputAndStructuredFailure() throws Exception {
        var tool = new VerifyTool(
            request -> VerifyResult.failed(VerifyResult.ErrorCode.VERIFIER_ERROR, "parse failed", "Running LiquidJava on: Example.java\n"),
            McpJsonDefaults.getMapper()
        );
        var result = tool.call(Map.of("paths", List.of("Example.java")));
        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertEquals("Running LiquidJava on: Example.java\n", content.get("output"));
        assertEquals(Map.of("code", "VERIFIER_ERROR", "message", "parse failed"), content.get("error"));
        assertEquals(content, McpJsonDefaults.getMapper().readValue(((TextContent) result.content().getFirst()).text(), MAP));
    }

    @Test
    void advertisesOnlyThePathsInputAndAResultSchema() {
        var tool = new VerifyTool(request -> fail("not invoked"), McpJsonDefaults.getMapper()).specification().tool();
        assertEquals("verify", tool.name());
        assertEquals(List.of("paths"), tool.inputSchema().get("required"));
        assertEquals(false, tool.inputSchema().get("additionalProperties"));
        assertEquals(List.of("success", "output"), tool.outputSchema().get("required"));
        assertTrue(tool.annotations().readOnlyHint());
    }

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void rejectsInvalidArgumentsBeforeVerification(Map<String, Object> arguments) {
        var tool = new VerifyTool(request -> fail("verifier must not run"), McpJsonDefaults.getMapper());
        var result = tool.call(arguments);
        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertEquals("", content.get("output"));
        assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
    }

    private static Stream<Map<String, Object>> invalidArguments() {
        return Stream.of(
            null, Map.of(), Map.of("paths", "x.java"), Map.of("paths", 1),
            Map.of("paths", List.of()), Map.of("paths", List.of("")), Map.of("paths", List.of(" \t\n")),
            Map.of("paths", List.of("x\u0000.java")), Map.of("paths", List.of(1)),
            Map.of("paths", Arrays.asList("x.java", null)), Map.of("paths", List.of("x.java"), "debug", true)
        );
    }
}
