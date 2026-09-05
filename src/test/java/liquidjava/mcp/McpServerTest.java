package liquidjava.mcp;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpServerTest {
    private McpSyncClient client;

    @BeforeEach
    void startServer() {
        var parameters = ServerParameters
            .builder(Path.of(System.getProperty("java.home"), "bin", "java").toString())
            .args("-jar", System.getProperty("server.jar"))
            .build();
        var transport = new StdioClientTransport(parameters, McpJsonDefaults.getMapper());
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
        client.initialize();
    }

    @AfterEach
    void stopServer() {
        if (client == null) return;
        assertTrue(client.closeGracefully(), "MCP client should shut down cleanly");
    }

    @Test
    void advertisesVerifyAndReturnsStructuredOutput() throws Exception {
        var tools = client.listTools().tools();
        assertEquals(List.of("verify", "get_diagnostics", "get_locals", "get_globals"), tools.stream().map(tool -> tool.name()).toList());

        var result = client.callTool(verifyRequest("Valid.java"));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(true, content.get("success"));
        assertTrue(((String) content.get("output")).contains("Correct! Passed Verification."));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(tools.getFirst().outputSchema(), content).valid());
        assertEquals(1, result.content().size());
        assertEquals(content, McpJsonDefaults.getMapper().readValue(
                ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));
    }

    @Test
    void returnsDebugOutputOverStdioWithoutLeakingIntoTheNextCall() throws Exception {
        var paths = verifyRequest("Invalid.java").arguments().get("paths");
        var result = client.callTool(new CallToolRequest("verify", Map.of("paths", paths, "debug", true)));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertTrue(((String) content.get("output")).contains("[SMT]"));
        assertTrue(((String) content.get("output")).contains("Refinement Error"));
        assertEquals(content, McpJsonDefaults.getMapper().readValue(
                ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));

        var quiet = client.callTool(verifyRequest("Valid.java"));
        assertFalse(quiet.isError());
        var quietContent = (Map<?, ?>) quiet.structuredContent();
        assertEquals(true, quietContent.get("success"));
        assertFalse(((String) quietContent.get("output")).contains("[SMT]"));
    }

    @Test
    void reportsRefinementFailureAsANormalToolResult() {
        var result = client.callTool(verifyRequest("Invalid.java"));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertTrue(((String) content.get("output")).contains("Refinement Error"));
    }

    @Test
    void invalidInputUsesTheStructuredErrorContract() {
        var result = client.callTool(new CallToolRequest("verify", Map.of()));
        assertTrue(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        assertEquals("INVALID_INPUT", ((Map<?, ?>) content.get("error")).get("code"));
    }

    @Test
    void returnsDiagnosticsOverStdio() throws Exception {
        var tool = client.listTools().tools().stream()
                .filter(candidate -> candidate.name().equals("get_diagnostics")).findFirst().orElseThrow();
        var result = client.callTool(new CallToolRequest("get_diagnostics", verifyRequest("Counterexample.java").arguments()));
        assertFalse(result.isError());
        var content = (Map<?, ?>) result.structuredContent();
        assertEquals(false, content.get("success"));
        var diagnostic = (Map<?, ?>) ((List<?>) content.get("errors")).getFirst();
        assertEquals("RefinementError", diagnostic.get("type"));
        assertNotNull(diagnostic.get("counterexample"));
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.outputSchema(), content).valid());
        assertEquals(content, McpJsonDefaults.getMapper().readValue(
                ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));
        var invalid = client.callTool(new CallToolRequest("get_diagnostics", Map.of()));
        assertTrue(invalid.isError());
        assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.outputSchema(), invalid.structuredContent()).valid());
    }

    @Test
    void returnsContextOverStdio() throws Exception {
        String file = Path.of("src/test/resources/fixtures/Context.java").toAbsolutePath().toString();
        for (String name : List.of("get_locals", "get_globals")) {
            var tool = client.listTools().tools().stream()
                    .filter(t -> t.name().equals(name)).findFirst().orElseThrow();
            var arguments = name.equals("get_locals")
                    ? Map.<String, Object>of("path", file, "file", file, "line", 12, "column", 9)
                    : Map.<String, Object>of("path", file, "file", file);
            var result = client.callTool(new CallToolRequest(name, arguments));
            assertFalse(result.isError(), result.toString());
            var content = (Map<?, ?>) result.structuredContent();
            assertFalse(content.containsKey("errors"));
            assertFalse(content.containsKey("warnings"));
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.outputSchema(), content).valid());
            assertEquals(content, McpJsonDefaults.getMapper().readValue(
                    ((TextContent) result.content().getFirst()).text(), new TypeRef<Map<String, Object>>() {}));
            if (name.equals("get_locals")) {
                var variables = (List<Map<String, Object>>) content.get("variables");
                assertTrue(variables.stream().anyMatch(v -> v.get("name").equals("input")));
                assertTrue(variables.stream().anyMatch(v -> v.get("name").equals("first")));
                assertFalse(variables.stream().anyMatch(v -> List.of("nested", "last", "unrelated").contains(v.get("name"))));
            } else {
                assertTrue(((List<Map<String, Object>>) content.get("aliases")).stream().anyMatch(a -> a.get("name").equals("Positive")));
                assertTrue(((List<Map<String, Object>>) content.get("ghosts")).stream().anyMatch(g -> g.get("name").equals("size")));
                assertEquals(2, ((List<?>) content.get("states")).size());
            }
            var invalid = client.callTool(new CallToolRequest(name, Map.of()));
            assertTrue(invalid.isError());
            assertTrue(McpJsonDefaults.getSchemaValidator().validate(tool.outputSchema(), invalid.structuredContent()).valid());
        }
    }

    private static CallToolRequest verifyRequest(String fixture) {
        String path = Path.of("src/test/resources/fixtures", fixture).toAbsolutePath().toString();
        return new CallToolRequest("verify", Map.of("paths", List.of(path)));
    }
}
