package liquidjava.mcp.tools.smt;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpErrorCode;

/** Checks whether a set of refinement predicates has a satisfying assignment. */
public final class CheckSatisfiabilityTool extends AbstractMcpTool {
    private final Function<SmtRequest, SmtResult> checker;

    public CheckSatisfiabilityTool(Function<SmtRequest, SmtResult> checker, McpJsonMapper jsonMapper) {
        super("check_satisfiability", """
            Checks whether explicit predicates are satisfiable using LiquidJava's solver, without verifying any Java files.
            Receives `variables` (map of names to types) and `constraints` (array of boolean predicates).
            Supported types: boolean, short, char, int, long, float, and double.
            Uses LiquidJava's solver semantics and returns sat, unsat, or unknown.
            Sat results include a satisfying assignment when the solver provides one.
            Does not support ghost functions, states, aliases, source constants, or implicit bindings.
        """, jsonMapper);
        this.checker = checker;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        String inputError = validateInput(arguments);
        if (inputError != null)
            return toMcpResult(SmtResult.failed(McpErrorCode.INVALID_INPUT, inputError));

        SmtRequest request;
        try {
            request = SmtRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(SmtResult.failed(McpErrorCode.INVALID_INPUT, e.getMessage()));
        }
        return toMcpResult(checker.apply(request));
    }

    private CallToolResult toMcpResult(SmtResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        if (result.status() != null) content.put("status", result.status().wireValue());
        if (result.assignment() != null) content.put("assignment", result.assignment());
        if (result.error() != null)
            addError(content, result.error().code(), result.error().message());
        return result(content, result.error() != null);
    }
}
