package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpError;

/**
 * Exposes aliases, ghost functions, and typestate definitions.
 */
public final class GetGlobalsTool extends AbstractMcpTool {
    private final ContextInspector inspector;

    public GetGlobalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_globals", """
            Runs LiquidJava on a path and returns available aliases, ghosts, and states, optionally filtering ghosts and states by file.
        """, jsonMapper);
        this.inspector = inspector;
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleRequest(arguments, ContextRequest::fromGlobalArguments,
            request -> toMcpResult(inspector.getGlobals(request)),
            message -> toMcpResult(ContextResult.failed(McpError.Code.INVALID_INPUT, message))
        );
    }

    private CallToolResult toMcpResult(ContextResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("aliases", result.context().getOrDefault("aliases", List.of()));
        content.put("ghosts", result.context().getOrDefault("ghosts", List.of()));
        content.put("states", result.context().getOrDefault("states", List.of()));
        return result(content, result.error());
    }
}
