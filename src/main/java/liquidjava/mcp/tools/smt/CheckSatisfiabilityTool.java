package liquidjava.mcp.tools.smt;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.McpError;

/** Checks whether a set of refinement predicates has a satisfying assignment. */
public final class CheckSatisfiabilityTool extends AbstractSmtTool<SmtRequest, SmtResult> {

    public CheckSatisfiabilityTool(Function<SmtRequest, SmtResult> checker, McpJsonMapper jsonMapper) {
        super("check_satisfiability", """
            Checks whether explicit predicates are satisfiable using LiquidJava's solver, without verifying any Java files.
            Receives `variables` (map of names to types), `ghosts` (map of names to uninterpreted function
            declarations with parameter types and a return type), and `constraints` (array of boolean predicates).
            Variables support boolean, short, char, int, long, float, double, and reference types.
            Uses LiquidJava's solver semantics and returns sat, unsat, or unknown.
            Sat results include a satisfying assignment when the solver provides one.
            Does not support aliases, source constants, or implicit receiver/return/old-state bindings.
        """, checker, message -> SmtResult.failed(McpError.Code.INVALID_INPUT, message), jsonMapper);
    }

    public CallToolResult call(Map<String, Object> arguments) {
        return handleSmtRequest(arguments, SmtRequest::fromArguments, "assignment");
    }
}
