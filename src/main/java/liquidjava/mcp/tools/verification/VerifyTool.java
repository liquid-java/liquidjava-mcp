package liquidjava.mcp.tools.verification;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;

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

    public CallToolResult call(Map<String, Object> arguments) {
        VerifyRequest request;
        try {
            request = VerifyRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(VerifyResult.failed(VerifyResult.ErrorCode.INVALID_INPUT, e.getMessage(), ""));
        }
        return toMcpResult(verifier.verify(request));
    }

    private CallToolResult toMcpResult(VerifyResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        content.put("output", result.output());
        if (result.error() != null)
            addError(content, result.error().code().name(), result.error().message());
        return result(content, result.error() != null);
    }
}
