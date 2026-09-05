package liquidjava.mcp.context;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.mcp.runtime.LiquidJavaRunner;

public final class ContextInspector {
    public ContextResult getLocals(ContextRequest request) {
        if (request.line() == null)
            return ContextResult.failed(ContextResult.ErrorCode.INVALID_INPUT, "expected file, line, and column");
        return inspect(request, () -> ContextMapper.locals(request));
    }

    public ContextResult getGlobals(ContextRequest request) {
        return inspect(request, ContextMapper::globals);
    }

    private ContextResult inspect(ContextRequest request, Supplier<Map<String, Object>> snapshot) {
        return LiquidJavaRunner.run(
            List.of(request.file()),
            true, 
            false,
            output -> ContextResult.completed(!Diagnostics.getInstance().foundError(), snapshot.get()),
            (message, output) -> ContextResult.failed(ContextResult.ErrorCode.VERIFIER_ERROR, message)
        );
    }
}
