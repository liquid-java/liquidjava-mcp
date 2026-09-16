package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;

/**
 * Exposes registered LiquidJava method and constructor contracts.
 */
public final class GetContractsTool extends AbstractContextTool<ContractRequest> {

    public GetContractsTool(ContextInspector inspector, McpJsonMapper jsonMapper) {
        super("get_contracts", """
            Returns contracts for methods and constructors.
            Each contract includes fully qualified signature, parameter names, types and refinements, return type and refinement, state transitions, and declaration location.
            Optional `className` and `signature` filters use exact matches and can be combined.
            The `signature` must be fully qualified, e.g. `com.example.MyClass.myMethod(int, java.lang.String)`.
        """, inspector, jsonMapper);
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleContextRequest(
            arguments,
            ContractRequest::fromArguments,
            inspector::getContracts,
            "contracts"
        );
    }
}
