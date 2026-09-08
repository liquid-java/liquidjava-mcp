package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpErrorCode;

/**
 * Exposes registered LiquidJava method and constructor contracts.
 */
public final class GetContractsTool extends AbstractMcpTool {
    private final ContextInspector inspector;

    public GetContractsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_contracts", """
            Returns contracts for methods and constructors.
            Each contract includes fully qualified signature, parameter names, types and refinements, return type and refinement, state transitions, and declaration location.
            Optional `className` and `signature` filters use exact matches and can be combined.
            The `signature` must be fully qualified, e.g. `com.example.MyClass.myMethod(int, java.lang.String)`.
        """, jsonMapper);
        this.inspector = inspector;
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        String inputError = validateInput(arguments);
        if (inputError != null)
            return toMcpResult(ContextResult.failed(McpErrorCode.INVALID_INPUT, inputError));

        ContractRequest request;
        try {
            request = ContractRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(ContextResult.failed(McpErrorCode.INVALID_INPUT, e.getMessage()));
        }
        return toMcpResult(inspector.getContracts(request));
    }

    private CallToolResult toMcpResult(ContextResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("contracts", result.context().getOrDefault("contracts", List.of()));
        if (result.error() != null)
            addError(content, result.error().code(), result.error().message());
        return result(content, result.error() != null);
    }
}
