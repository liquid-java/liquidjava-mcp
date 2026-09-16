package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpError;
import liquidjava.smt.Counterexample;

public final class SmtResult extends SolverResult<SmtResult.Status> {
    public static SmtResult sat(Counterexample counterexample) {
        List<Map<String, String>> values = counterexample.assignments().stream()
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

    private SmtResult(Status status, List<Map<String, String>> assignment, McpError error) {
        super(status, assignment, error);
    }

    public List<Map<String, String>> assignment() {
        return assignments();
    }

    public enum Status {
        SAT,
        UNSAT,
        UNKNOWN
    }
}
