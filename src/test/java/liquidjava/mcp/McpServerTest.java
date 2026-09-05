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
        assertEquals(List.of("verify"), tools.stream().map(tool -> tool.name()).toList());

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

    private static CallToolRequest verifyRequest(String fixture) {
        String path = Path.of("src/test/resources/fixtures", fixture).toAbsolutePath().toString();
        return new CallToolRequest("verify", Map.of("paths", List.of(path)));
    }
}
