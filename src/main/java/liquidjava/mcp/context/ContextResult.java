package liquidjava.mcp.context;

import java.util.Map;

public record ContextResult(boolean success, Map<String, Object> context, Failure error) {
    public ContextResult {
        context = Map.copyOf(context);
        if (success && error != null)
            throw new IllegalArgumentException("an execution error cannot be successful");
    }

    public static ContextResult completed(boolean success, Map<String, Object> context) {
        return new ContextResult(success, context, null);
    }

    public static ContextResult failed(ErrorCode code, String message) {
        return new ContextResult(false, Map.of(), new Failure(code, message));
    }

    public enum ErrorCode { INVALID_INPUT, VERIFIER_ERROR }

    public record Failure(ErrorCode code, String message) {}
}
