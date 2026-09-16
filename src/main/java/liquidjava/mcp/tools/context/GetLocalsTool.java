package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;

/**
 * Exposes refined variables visible at a source position.
 */
public final class GetLocalsTool extends AbstractContextTool<ContextRequest> {

    public GetLocalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_locals", """
            Runs LiquidJava on a path and returns variables and their refinements filtered by source file, scope, and position.
            Locations use one-based lines and columns with inclusive ends.
        """, inspector, jsonMapper);
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleContextRequest(
            arguments,
            ContextRequest::fromPositionArguments,
            inspector::getLocals,
            "variables"
        );
    }
}
