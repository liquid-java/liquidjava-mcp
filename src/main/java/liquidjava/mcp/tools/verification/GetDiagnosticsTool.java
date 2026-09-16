package liquidjava.mcp.tools.verification;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes LiquidJava verification diagnostics as structured data.
 */
public final class GetDiagnosticsTool extends AbstractVerificationTool {

    public GetDiagnosticsTool(Verifier verifier, McpJsonMapper jsonMapper) {
        super("get_diagnostics", """
            Runs LiquidJava and returns diagnostics in a structured, machine-readable format instead of plain-text terminal output.
            Prefer it over `verify` when you need to programmatically inspect, filter, or reason over individual errors or warnings.
            Receives a file or directory path to verify and returns `errors` and `warnings` arrays, each containing structured diagnostics with type, severity, location, message, refinements, verification conditions, details, hints, and counterexamples when available.
            Locations use one-based lines and columns with inclusive ends.
        """, verifier, jsonMapper);
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        return handleVerification(arguments);
    }

    protected CallToolResult toMcpResult(VerifyResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        content.put("errors", result.errors());
        content.put("warnings", result.warnings());
        return result(content, result.error());
    }
}
