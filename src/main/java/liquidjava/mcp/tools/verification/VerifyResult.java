package liquidjava.mcp.tools.verification;

import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpError;

public record VerifyResult(boolean success, String output, List<Map<String, Object>> errors, List<Map<String, Object>> warnings, McpError error) {
    public VerifyResult {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
        if (success && error != null)
            throw new IllegalArgumentException("an execution error cannot be successful");
    }

    public static VerifyResult completed(boolean success, String output, List<Map<String, Object>> errors, List<Map<String, Object>> warnings) {
        return new VerifyResult(success, output, errors, warnings, null);
    }

    public static VerifyResult failed(McpError.Code code, String message, String output) {
        return new VerifyResult(false, output, List.of(), List.of(), new McpError(code, message));
    }
}
