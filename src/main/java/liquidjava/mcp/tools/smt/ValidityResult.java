package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpError;

public final class ValidityResult extends SolverResult<ValidityResult.Status> {
    public static ValidityResult valid() {
        return new ValidityResult(Status.VALID, null, null);
    }

    public static ValidityResult invalid(List<Map<String, String>> counterexample) {
        return new ValidityResult(Status.INVALID, counterexample, null);
    }

    public static ValidityResult unknown() {
        return new ValidityResult(Status.UNKNOWN, null, null);
    }

    public static ValidityResult failed(McpError.Code code, String message) {
        return new ValidityResult(null, null, new McpError(code, message));
    }

    private ValidityResult(Status status, List<Map<String, String>> counterexample, McpError error) {
        super(status, counterexample, error);
    }

    public List<Map<String, String>> counterexample() {
        return assignments();
    }

    public enum Status {
        VALID,
        INVALID,
        UNKNOWN
    }
}
