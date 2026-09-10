package liquidjava.mcp.tools.verification;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.json.McpJsonDefaults;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.tools.McpError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
class VerifyToolTest {
    @Test
    void rejectsNonJavaFilesAndMissingPathsBeforeVerification(@TempDir Path temporary) throws Exception {
        Path text = Files.writeString(temporary.resolve("input.txt"), "plain text, not Java");
        Verifier verifier = request -> fail("verifier must not run");
        var mapper = McpJsonDefaults.getMapper();
        for (var tool : List.of(new VerifyTool(verifier, mapper), new GetDiagnosticsTool(verifier, mapper))) {
            for (Path path : List.of(text, temporary.resolve("missing.java"))) {
                var result = tool.call(Map.of("path", path.toString()));
                assertTrue(result.isError());
                var content = (Map<?, ?>) result.structuredContent();
                assertEquals(false, content.get("success"));
                assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
            }
        }
    }

    @Test
    void passesPathWithoutChangingIt(@TempDir Path temporary) throws Exception {
        var received = new AtomicReference<VerifyRequest>();
        var tool = new VerifyTool(request -> {
            received.set(request);
            return VerifyResult.completed(true, "Correct! Passed Verification.\n", List.of(), List.of());
        }, McpJsonDefaults.getMapper());
        Files.createDirectory(temporary.resolve("folder"));
        Files.writeString(temporary.resolve("folder/file with spaces.java"), "class Example {}");
        String path = temporary.resolve("folder/../folder/file with spaces.java").toString();
        var result = tool.call(Map.of("path", path));
        assertEquals(path, received.get().path());
        assertFalse(result.isError());
        assertEquals(Map.of("success", true, "output", "Correct! Passed Verification.\n"), result.structuredContent());
        assertTrue(result.content().isEmpty());
    }

    @Test
    void returnsDebugOutputWhenRequestedAndDefaultsToQuietOutput() {
        var tool = new VerifyTool(new LiquidJavaVerifier(), McpJsonDefaults.getMapper());
        for (var arguments : List.of(
                Map.<String, Object>of("path", "src/test/resources/examples/Valid.java", "debug", true),
                Map.<String, Object>of("path", "src/test/resources/examples/Valid.java"),
                Map.<String, Object>of("path", "src/test/resources/examples/Valid.java", "debug", false))) {
            var result = tool.call(arguments);
            assertFalse(result.isError(), result.toString());
            var content = (Map<?, ?>) result.structuredContent();
            assertEquals(true, content.get("success"));
            String output = (String) content.get("output");
            assertEquals(Boolean.TRUE.equals(arguments.get("debug")), output.contains("[SMT]"), output);
            assertFalse(output.contains("\u001B"));
            assertFalse(CommandLineLauncher.cmdArgs.debugMode);
        }
    }

    @Test
    void verificationFailureIsNotAToolExecutionError() {
        var tool = new VerifyTool(
            request -> VerifyResult.completed(false, "Refinement Error", List.of(), List.of()),
            McpJsonDefaults.getMapper()
        );
        var result = tool.call(Map.of("path", "src/test/resources/examples/Valid.java"));
        assertFalse(result.isError());
        assertEquals(Map.of("success", false, "output", "Refinement Error"), result.structuredContent());
    }

    @Test
    void executionErrorIncludesPartialOutputAndStructuredFailure() throws Exception {
        var tool = new VerifyTool(
            request -> VerifyResult.failed(McpError.Code.VERIFIER_ERROR, "parse failed", "Running LiquidJava on: Example.java\n"),
            McpJsonDefaults.getMapper()
        );
        var result = tool.call(Map.of("path", "src/test/resources/examples/Valid.java"));
        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertEquals("Running LiquidJava on: Example.java\n", content.get("output"));
        assertEquals(Map.of("code", "VERIFIER_ERROR", "message", "parse failed"), content.get("error"));
        assertTrue(result.content().isEmpty());
    }

    @Test
    void advertisesPathAndOptionalDebugAndAResultSchema() {
        var tool = new VerifyTool(request -> fail("not invoked"), McpJsonDefaults.getMapper()).specification().tool();
        assertEquals("verify", tool.name());
        assertEquals(List.of("path"), tool.inputSchema().get("required"));
        assertEquals(false, tool.inputSchema().get("additionalProperties"));
        var properties = (Map<?, ?>) tool.inputSchema().get("properties");
        var debug = (Map<?, ?>) properties.get("debug");
        assertNotNull(debug);
        assertEquals("boolean", debug.get("type"));
        assertEquals(false, debug.get("default"));
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
        var nullDebug = new HashMap<String, Object>();
        nullDebug.put("path", "x.java");
        nullDebug.put("debug", null);
        return Stream.of(
            null, Map.of(), Map.of("path", 1), Map.of("path", ""),
            Map.of("path", "x.java", "debug", "true"), nullDebug,
            Map.of("path", "x.java", "extra", true)
        );
    }
}
