package liquidjava.mcp.tools.context;

import java.util.Map;
import liquidjava.mcp.tools.McpErrorCode;

public record ContextResult(Map<String, Object> context, Failure error) {
    public ContextResult {
        context = Map.copyOf(context);
    }

    public static ContextResult completed(Map<String, Object> context) {
        return new ContextResult(context, null);
    }

    public static ContextResult failed(McpErrorCode code, String message) {
        return new ContextResult(Map.of(), new Failure(code, message));
    }

    public record Failure(McpErrorCode code, String message) {}
}
