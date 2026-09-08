package liquidjava.mcp.tools.verification;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpErrorCode;

/**
 * Exposes LiquidJava verification with plain-text output.
 */
public final class VerifyTool extends AbstractMcpTool {
    private final Verifier verifier;

    public VerifyTool(Verifier verifier, McpJsonMapper jsonMapper) {
        super("verify", """
            Runs LiquidJava and returns the same high-level terminal output a developer would see.
            Prefer it over `get_diagnostics` for quick checks, to reduce token usage, or to inspect debug information by setting `debug` to true.
            Debug output shows verification conditions, their simplifications, and solver results, including counterexamples.
            Receives a file or directory path to verify and returns the verification status and plain-text LiquidJava output.
        """, jsonMapper);
        this.verifier = verifier;
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleRequest(arguments, VerifyRequest::fromArguments,
            request -> toMcpResult(verifier.verify(request)),
            message -> toMcpResult(VerifyResult.failed(McpErrorCode.INVALID_INPUT, message, ""))
        );
    }

    private CallToolResult toMcpResult(VerifyResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        content.put("output", result.output());
        if (result.error() != null)
            addError(content, result.error().code(), result.error().message());
        
        return result(content, result.error() != null);
    }
}
