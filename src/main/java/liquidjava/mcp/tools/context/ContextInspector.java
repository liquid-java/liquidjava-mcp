package liquidjava.mcp.tools.context;

import java.util.Map;
import java.util.function.Supplier;
import liquidjava.mcp.runtime.LiquidJavaRunner;
import liquidjava.mcp.tools.McpError;

public final class ContextInspector {
    public ContextResult getLocals(ContextRequest request) {
        return inspect(request.path(), () -> ContextMapper.locals(request));
    }

    public ContextResult getGlobals(ContextRequest request) {
        return inspect(request.path(), () -> ContextMapper.globals(request));
    }

    public ContextResult getContracts(ContractRequest request) {
        return inspect(request.path(), () -> ContextMapper.contracts(request));
    }

    private ContextResult inspect(String path, Supplier<Map<String, Object>> snapshot) {
        return LiquidJavaRunner.run(
            path,
            false,
            output -> ContextResult.completed(snapshot.get()),
            (message, output) -> ContextResult.failed(McpError.Code.VERIFIER_ERROR, message)
        );
    }
}
