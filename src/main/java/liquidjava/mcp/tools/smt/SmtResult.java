package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpErrorCode;
import liquidjava.smt.Counterexample;

public record SmtResult(Status status, List<Map<String, String>> assignment, Failure error) {
    public static SmtResult sat(Counterexample assignment) {
        List<Map<String, String>> values = assignment.assignments().stream()
            .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList();
        return new SmtResult(Status.SAT, values, null);
    }

    public static SmtResult unsat() {
        return new SmtResult(Status.UNSAT, null, null);
    }

    public static SmtResult unknown() {
        return new SmtResult(Status.UNKNOWN, null, null);
    }

    public static SmtResult failed(McpErrorCode code, String message) {
        return new SmtResult(null, null, new Failure(code, message));
    }

    public enum Status {
        SAT,
        UNSAT,
        UNKNOWN;

        public String wireValue() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Failure(McpErrorCode code, String message) {}
}
