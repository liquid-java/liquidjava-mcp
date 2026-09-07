package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import liquidjava.mcp.context.ContextRequest;
import liquidjava.mcp.context.ContextInspector;
import liquidjava.mcp.context.ContextResult;

public final class GetLocalsTool extends AbstractMcpTool {
    private final ContextInspector inspector;

    public GetLocalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_locals", """
            Runs LiquidJava on a path and returns variables and their refinements filtered by source file, scope, and position.
            Locations use one-based lines and columns with inclusive ends.
        """, jsonMapper);
        this.inspector = inspector;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        ContextRequest request;
        try {
            request = ContextRequest.fromPositionArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(ContextResult.failed(ContextResult.ErrorCode.INVALID_INPUT, e.getMessage()));
        }
        ContextResult result = inspector.getLocals(request);
        return toMcpResult(result);
    }

    private CallToolResult toMcpResult(ContextResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("variables", result.context().getOrDefault("variables", List.of()));
        if (result.error() != null)
            addError(content, result.error().code().name(), result.error().message());
        return result(content, result.error() != null);
    }
}
