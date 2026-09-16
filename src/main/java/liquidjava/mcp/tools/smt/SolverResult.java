package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import liquidjava.mcp.tools.McpError;

public abstract class SolverResult<S extends Enum<S>> {
    private final S status;
    private final List<Map<String, String>> assignments;
    private final McpError error;

    protected SolverResult(S status, List<Map<String, String>> assignments, McpError error) {
        this.status = status;
        this.assignments = assignments;
        this.error = error;
    }

    public final S status() {
        return status;
    }

    public final List<Map<String, String>> assignments() {
        return assignments;
    }

    public final McpError error() {
        return error;
    }

    public final String statusValue() {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }
}
