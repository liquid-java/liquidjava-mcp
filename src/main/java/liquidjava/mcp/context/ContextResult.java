package liquidjava.mcp.context;

import java.util.Map;

public record ContextResult(Map<String, Object> context, Failure error) {
    public ContextResult {
        context = Map.copyOf(context);
    }

    public static ContextResult completed(Map<String, Object> context) {
        return new ContextResult(context, null);
    }

    public static ContextResult failed(ErrorCode code, String message) {
        return new ContextResult(Map.of(), new Failure(code, message));
    }

    public enum ErrorCode { INVALID_INPUT, VERIFIER_ERROR }

    public record Failure(ErrorCode code, String message) {}
}
