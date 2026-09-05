package liquidjava.mcp.verification;

public record VerifyResult(boolean success, String output, Failure error) {
    public VerifyResult {
        if (success && error != null)
            throw new IllegalArgumentException("an execution error cannot be successful");
    }

    public static VerifyResult completed(boolean success, String output) {
        return new VerifyResult(success, output, null);
    }

    public static VerifyResult failed(ErrorCode code, String message, String output) {
        return new VerifyResult(false, output, new Failure(code, message));
    }

    public enum ErrorCode { INVALID_INPUT, VERIFIER_ERROR }

    public record Failure(ErrorCode code, String message) {}
}
