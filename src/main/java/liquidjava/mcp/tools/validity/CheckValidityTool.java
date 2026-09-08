package liquidjava.mcp.tools.validity;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.AbstractMcpTool;

/**
 * Checks whether explicit refinement assumptions imply a conclusion.
 */
public final class CheckValidityTool extends AbstractMcpTool {
    private final Function<ValidityRequest, ValidityResult> checker;

    public CheckValidityTool(Function<ValidityRequest, ValidityResult> checker, McpJsonMapper jsonMapper) {
        super("check_validity", """
            Checks whether explicit assumptions imply a conclusion using LiquidJava's solver, without verifying any Java files.
            Receives `variables` (map of names to types), `assumptions` (array of boolean predicates), and one `conclusion` (boolean predicate).
            Supported types: boolean, short, char, int, long, float, and double.
            Uses LiquidJava's solver semantics.
            Does not support ghost functions, states, aliases, source constants, or implicit bindings.
            Empty assumptions mean true, while contradictory assumptions make every conclusion valid.
            Returns `status` valid, invalid (with counterexample assignments), or unknown (with a reason).
        """, jsonMapper);
        this.checker = checker;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        ValidityRequest request;
        try {
            request = ValidityRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(ValidityResult.failed("INVALID_INPUT", e.getMessage()));
        }
        return toMcpResult(checker.apply(request));
    }

    private CallToolResult toMcpResult(ValidityResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        if (result.status() != null) content.put("status", result.status());
        if (result.counterexample() != null) content.put("counterexample", result.counterexample());
        if (result.reason() != null) content.put("reason", result.reason());
        if (result.error() != null)
            addError(content, result.error().code(), result.error().message());
        return result(content, result.error() != null);
    }
}
