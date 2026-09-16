package liquidjava.mcp.tools.context;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpError;

abstract class AbstractContextTool<T> extends AbstractMcpTool {
    protected final ContextInspector inspector;

    protected AbstractContextTool(
        String name,
        String description,
        ContextInspector inspector,
        McpJsonMapper jsonMapper
    ) {
        super(name, description, jsonMapper);
        this.inspector = inspector;
    }

    protected final CallToolResult handleContextRequest(
        Map<String, Object> arguments,
        Function<Map<String, Object>, T> parser,
        Function<T, ContextResult> operation,
        String... resultNames
    ) {
        return handleRequest(arguments, parser,
            request -> toMcpResult(operation.apply(request), resultNames),
            message -> toMcpResult(ContextResult.failed(McpError.Code.INVALID_INPUT, message), resultNames)
        );
    }

    private CallToolResult toMcpResult(ContextResult result, String... resultNames) {
        Map<String, Object> content = new LinkedHashMap<>();
        for (String resultName : resultNames)
            content.put(resultName, result.context().getOrDefault(resultName, List.of()));
        return result(content, result.error());
    }
}
