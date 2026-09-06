package liquidjava.mcp.context;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import liquidjava.mcp.runtime.LiquidJavaRunner;

public final class ContextInspector {
    public ContextResult getLocals(ContextRequest request) {
        if (request.line() == null)
            return ContextResult.failed(ContextResult.ErrorCode.INVALID_INPUT, "expected path, file, line, and column");
        return inspect(request.path(), () -> ContextMapper.locals(request));
    }

    public ContextResult getGlobals(ContextRequest request) {
        return inspect(request.path(), () -> ContextMapper.globals(request));
    }

    private ContextResult inspect(String path, Supplier<Map<String, Object>> snapshot) {
        return LiquidJavaRunner.run(
            List.of(path),
            true, 
            false,
            output -> ContextResult.completed(snapshot.get()),
            (message, output) -> ContextResult.failed(ContextResult.ErrorCode.VERIFIER_ERROR, message)
        );
    }
}
