package liquidjava.mcp;

import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
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
    void advertisesTools() {
        var tools = client.listTools().tools();
        assertEquals(List.of("verify", "get_diagnostics", "get_locals", "get_globals", "get_contracts", "check_validity", "check_satisfiability", "get_state_machine"), tools.stream().map(tool -> tool.name()).toList());
    }

    @Test
    void verifyTool() {
        CallToolRequest request = CallToolRequest.builder("verify").arguments(
            Map.of("path", examplePath("Valid.java"))
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void getDiagnosticsTool() {
        CallToolRequest request = CallToolRequest.builder("get_diagnostics").arguments(
            Map.of("path", examplePath("Counterexample.java"))
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void getLocalsTool() {
        CallToolRequest request = CallToolRequest.builder("get_locals").arguments(
            Map.of("path", examplePath("Context.java"), "line", 12, "column", 9)
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void getGlobalsTool() {
        CallToolRequest request = CallToolRequest.builder("get_globals").arguments(
            Map.of("path", examplePath("Context.java"))
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void getContractsTool() {
        CallToolRequest request = CallToolRequest.builder("get_contracts").arguments(
            Map.of("path", examplePath("Contracts.java"), "signature", "examples.Contracts.increment(int)")
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void checkValidityTool() {
        CallToolRequest request = CallToolRequest.builder("check_validity").arguments(Map.of(
            "variables", Map.of("x", "int"),
            "ghosts", Map.of(),
            "assumptions", List.of("x >= 0"),
            "conclusion", "x > 0")
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void checkSatisfiabilityTool() {
        CallToolRequest request = CallToolRequest.builder("check_satisfiability").arguments(Map.of(
            "variables", Map.of("x", "int"),
            "ghosts", Map.of(),
            "constraints", List.of("x == 11"))
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    @Test
    void getStateMachineTool() {
        CallToolRequest request = CallToolRequest.builder("get_state_machine").arguments(
            Map.of("path", examplePath("StateMachine.java"))
        ).build();
        CallToolResult result = client.callTool(request);
        assertFalse(result.isError(), result.toString());
    }

    private static String examplePath(String example) {
        return Path.of("src/test/resources/examples", example).toAbsolutePath().toString();
    }
}
