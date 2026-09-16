package liquidjava.mcp.tools.smt;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.AbstractMcpTool;

abstract class AbstractSmtTool<T, R extends SolverResult<?>> extends AbstractMcpTool {
    private final Function<T, R> checker;
    private final Function<String, R> invalidInput;

    protected AbstractSmtTool(
        String name,
        String description,
        Function<T, R> checker,
        Function<String, R> invalidInput,
        McpJsonMapper jsonMapper
    ) {
        super(name, description, jsonMapper);
        this.checker = checker;
        this.invalidInput = invalidInput;
    }

    protected final CallToolResult handleSmtRequest(
        Map<String, Object> arguments,
        Function<Map<String, Object>, T> parser,
        String resultName
    ) {
        return handleRequest(arguments, parser,
            request -> solverResult(checker.apply(request), resultName),
            message -> solverResult(invalidInput.apply(message), resultName)
        );
    }

    private CallToolResult solverResult(R result, String resultName) {
        Map<String, Object> content = new LinkedHashMap<>();
        if (result.status() != null) content.put("status", result.statusValue());
        if (result.assignments() != null) content.put(resultName, result.assignments());
        return result(content, result.error());
    }
}
