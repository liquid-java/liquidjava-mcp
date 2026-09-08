package liquidjava.mcp.tools.context;

import java.util.Map;
import liquidjava.mcp.tools.McpError;

public record ContextResult(Map<String, Object> context, McpError error) {
    public ContextResult {
        context = Map.copyOf(context);
    }

    public static ContextResult completed(Map<String, Object> context) {
        return new ContextResult(context, null);
    }

    public static ContextResult failed(McpError.Code code, String message) {
        return new ContextResult(Map.of(), new McpError(code, message));
    }
}
