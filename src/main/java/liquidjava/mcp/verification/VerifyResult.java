package liquidjava.mcp.verification;

import java.util.List;
import java.util.Map;

public record VerifyResult(boolean success, String output, List<Map<String, Object>> errors, List<Map<String, Object>> warnings, Failure error) {
    public VerifyResult {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
        if (success && error != null)
            throw new IllegalArgumentException("an execution error cannot be successful");
    }

    public static VerifyResult completed(boolean success, String output, List<Map<String, Object>> errors, List<Map<String, Object>> warnings) {
        return new VerifyResult(success, output, errors, warnings, null);
    }

    public static VerifyResult failed(ErrorCode code, String message, String output) {
        return new VerifyResult(false, output, List.of(), List.of(), new Failure(code, message));
    }

    public enum ErrorCode { INVALID_INPUT, VERIFIER_ERROR }

    public record Failure(ErrorCode code, String message) {}
}
