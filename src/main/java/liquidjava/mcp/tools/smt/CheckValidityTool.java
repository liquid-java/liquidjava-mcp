package liquidjava.mcp.tools.smt;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpError;

/**
 * Checks whether explicit refinement assumptions imply a conclusion.
 */
public final class CheckValidityTool extends AbstractMcpTool {
    private final Function<ValidityRequest, ValidityResult> checker;

    public CheckValidityTool(Function<ValidityRequest, ValidityResult> checker, McpJsonMapper jsonMapper) {
        super("check_validity", """
            Checks whether explicit assumptions imply a conclusion using LiquidJava's solver, without verifying any Java files.
            Receives `variables` (map of names to types), `ghosts` (map of names to uninterpreted function
            declarations with parameter types and a return type), `assumptions` (array of boolean predicates),
            and one `conclusion` (boolean predicate).
            Variables support boolean, short, char, int, long, float, double, and reference types.
            Uses LiquidJava's solver semantics.
            Does not support aliases, source constants, or implicit receiver/return/old-state bindings.
            Empty assumptions mean true, while contradictory assumptions make every conclusion valid.
            Returns `status` valid, invalid, or unknown (with counterexample assignments for invalid results).
        """, jsonMapper);
        this.checker = checker;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        return handleRequest(arguments, ValidityRequest::fromArguments,
            request -> toMcpResult(checker.apply(request)),
            message -> toMcpResult(ValidityResult.failed(McpError.Code.INVALID_INPUT, message))
        );
    }

    private CallToolResult toMcpResult(ValidityResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        if (result.status() != null) content.put("status", result.status().value());
        if (result.counterexample() != null) content.put("counterexample", result.counterexample());
        return result(content, result.error());
    }
}
