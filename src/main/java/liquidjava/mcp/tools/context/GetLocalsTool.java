package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpErrorCode;

/**
 * Exposes refined variables visible at a source position.
 */
public final class GetLocalsTool extends AbstractMcpTool {
    private final ContextInspector inspector;

    public GetLocalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_locals", """
            Runs LiquidJava on a path and returns variables and their refinements filtered by source file, scope, and position.
            Locations use one-based lines and columns with inclusive ends.
        """, jsonMapper);
        this.inspector = inspector;
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleRequest(arguments, ContextRequest::fromPositionArguments,
            request -> toMcpResult(inspector.getLocals(request)),
            message -> toMcpResult(ContextResult.failed(McpErrorCode.INVALID_INPUT, message))
        );
    }

    private CallToolResult toMcpResult(ContextResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("variables", result.context().getOrDefault("variables", List.of()));
        if (result.error() != null)
            addError(content, result.error().code(), result.error().message());
        
        return result(content, result.error() != null);
    }
}
