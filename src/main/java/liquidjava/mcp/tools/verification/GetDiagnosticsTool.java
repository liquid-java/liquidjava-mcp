package liquidjava.mcp.tools.verification;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;

/**
 * Exposes LiquidJava verification diagnostics as structured data.
 */
public final class GetDiagnosticsTool extends AbstractMcpTool {
    private final Verifier verifier;

    public GetDiagnosticsTool(Verifier verifier, McpJsonMapper jsonMapper) {
        super("get_diagnostics", """
            Runs LiquidJava and returns diagnostics in a structured, machine-readable format instead of plain-text terminal output.
            Prefer it over `verify` when you need to programmatically inspect, filter, or reason over individual errors or warnings.
            Receives a file or directory path to verify and returns a `errors` and a `warnings` arrays, each containing structured diagnostics with type, severity, location, message, refinements, hints, and counterexamples when available.
            Locations use one-based lines and columns with inclusive ends.
        """, jsonMapper);
        this.verifier = verifier;
    }

    public CallToolResult call(Map<String, Object> arguments) {
        VerifyRequest request;
        try {
            if (arguments == null || !arguments.keySet().equals(java.util.Set.of("path")))
                throw new IllegalArgumentException("expected exactly one argument: path");
            request = VerifyRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(VerifyResult.failed(VerifyResult.ErrorCode.INVALID_INPUT, e.getMessage(), ""));
        }
        return toMcpResult(verifier.verify(request));
    }

    private CallToolResult toMcpResult(VerifyResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        content.put("errors", result.errors());
        content.put("warnings", result.warnings());
        if (result.error() != null)
            addError(content, result.error().code().name(), result.error().message());
        return result(content, result.error() != null);
    }
}
