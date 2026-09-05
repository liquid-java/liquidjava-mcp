package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import liquidjava.mcp.tools.schemas.ToolSchemas;
import liquidjava.mcp.verification.Verifier;
import liquidjava.mcp.verification.VerifyRequest;
import liquidjava.mcp.verification.VerifyResult;

public final class GetDiagnosticsTool {
    private final Verifier verifier;
    private final McpJsonMapper jsonMapper;
    private final String name = "get_diagnostics";
    private final String description = """
        Runs LiquidJava and returns diagnostics in a structured, machine-readable format instead of plain-text terminal output.
        Prefer it over `verify` when you need to programmatically inspect, filter, or reason over individual errors or warnings.
        Receives one or more paths to verify and returns a `errors` and a `warnings` arrays, each containing structured diagnostics with type, severity, location, message, refinements, hints, and counterexamples when available.
        Locations use one-based lines and columns with inclusive ends.
    """;

    public GetDiagnosticsTool(Verifier verifier, McpJsonMapper jsonMapper) {
        this.verifier = verifier;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification specification() {
        ToolSchemas schemas = ToolSchemas.load(name, jsonMapper);
        Tool tool = Tool.builder(name, schemas.inputSchema())
            .description(description.stripIndent().trim())
            .outputSchema(schemas.outputSchema())
            .annotations(ToolAnnotations.builder().readOnlyHint(true).destructiveHint(false)
            .idempotentHint(true).openWorldHint(false).build())
            .build();
        return SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> call(request.arguments())).build();
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
        content.put("errors", result.errors());
        content.put("warnings", result.warnings());
        if (result.error() != null)
            content.put("error", Map.of("code", result.error().code().name(), "message", result.error().message()));

        try {
            return CallToolResult.builder()
                .structuredContent(content)
                .addTextContent(jsonMapper.writeValueAsString(content))
                .isError(result.error() != null)
                .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize verification result", e);
        }
    }
}
