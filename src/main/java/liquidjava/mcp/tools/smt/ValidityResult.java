package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import liquidjava.mcp.tools.McpErrorCode;

public record ValidityResult(Status status, List<Map<String, String>> counterexample, Failure error) {
    public static ValidityResult valid() {
        return new ValidityResult(Status.VALID, null, null);
    }

    public static ValidityResult invalid(List<Map<String, String>> counterexample) {
        return new ValidityResult(Status.INVALID, counterexample, null);
    }

    public static ValidityResult unknown() {
        return new ValidityResult(Status.UNKNOWN, null, null);
    }

    public static ValidityResult failed(McpErrorCode code, String message) {
        return new ValidityResult(null, null, new Failure(code, message));
    }

    public enum Status {
        VALID,
        INVALID,
        UNKNOWN;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Failure(McpErrorCode code, String message) {}
}
