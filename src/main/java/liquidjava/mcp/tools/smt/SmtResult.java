package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import liquidjava.mcp.tools.McpError;
import liquidjava.smt.Counterexample;

public record SmtResult(Status status, List<Map<String, String>> assignment, McpError error) {
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

    public static SmtResult failed(McpError.Code code, String message) {
        return new SmtResult(null, null, new McpError(code, message));
    }

    public enum Status {
        SAT,
        UNSAT,
        UNKNOWN;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

}
