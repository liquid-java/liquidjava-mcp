package liquidjava.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import java.io.PrintStream;
import liquidjava.mcp.tools.VerifyTool;
import liquidjava.mcp.tools.GetLocalsTool;
import liquidjava.mcp.tools.GetGlobalsTool;
import liquidjava.mcp.tools.GetDiagnosticsTool;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.context.ContextInspector;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        PrintStream protocolOutput = System.out;
        System.setOut(System.err);
        McpJsonMapper mapper = McpJsonDefaults.getMapper();
        StdioServerTransportProvider transport = new StdioServerTransportProvider(mapper, System.in, protocolOutput);
        LiquidJavaVerifier verifier = new LiquidJavaVerifier();
        ContextInspector inspector = new ContextInspector();
        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("liquidjava-mcp", "0.1.0-SNAPSHOT")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .validateToolInputs(false)
                .tools(
                    new VerifyTool(verifier, mapper).specification(),
                    new GetDiagnosticsTool(verifier, mapper).specification(),
                    new GetLocalsTool(inspector, mapper).specification(),
                    new GetGlobalsTool(inspector, mapper).specification()
                )
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "mcp-shutdown"));
    }
}
