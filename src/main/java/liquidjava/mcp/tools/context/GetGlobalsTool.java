package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;

/**
 * Exposes aliases, ghost functions, and typestate definitions.
 */
public final class GetGlobalsTool extends AbstractContextTool<ContextRequest> {

    public GetGlobalsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_globals", """
            Runs LiquidJava on a path and returns available aliases, ghosts, and states, optionally filtering ghosts and states by file.
        """, inspector, jsonMapper);
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleContextRequest(
            arguments,
            ContextRequest::fromGlobalArguments,
            inspector::getGlobals,
            "aliases", "ghosts", "states"
        );
    }
}
