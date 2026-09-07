package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import liquidjava.mcp.context.ContextRequest;
import liquidjava.mcp.context.ContextInspector;
import liquidjava.mcp.context.ContextResult;

public final class GetGlobalsTool extends AbstractMcpTool {
    private final ContextInspector inspector;

    public GetGlobalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_globals", """
            Runs LiquidJava on a path and returns available aliases, ghosts, and states, optionally filtering ghosts and states by file.
        """, jsonMapper);
        this.inspector = inspector;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        ContextRequest request;
        try {
            request = ContextRequest.fromGlobalArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(ContextResult.failed(ContextResult.ErrorCode.INVALID_INPUT, e.getMessage()));
        }
        ContextResult result = inspector.getGlobals(request);
        return toMcpResult(result);
    }

    private CallToolResult toMcpResult(ContextResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("aliases", result.context().getOrDefault("aliases", List.of()));
        content.put("ghosts", result.context().getOrDefault("ghosts", List.of()));
        content.put("states", result.context().getOrDefault("states", List.of()));
        if (result.error() != null)
            addError(content, result.error().code().name(), result.error().message());
        return result(content, result.error() != null);
    }
}
