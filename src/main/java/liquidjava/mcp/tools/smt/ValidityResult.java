package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpErrorCode;
import liquidjava.smt.Counterexample;

public record ValidityResult(Status status, List<Map<String, String>> counterexample, Failure error) {
    public static ValidityResult valid() {
        return new ValidityResult(Status.VALID, null, null);
    }

    public static ValidityResult invalid(Counterexample counterexample) {
        List<Map<String, String>> assignments = counterexample.assignments().stream()
            .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList();
        return new ValidityResult(Status.INVALID, assignments, null);
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
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Failure(McpErrorCode code, String message) {}
}
