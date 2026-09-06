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

public final class VerifyTool {
    private final Verifier verifier;
    private final McpJsonMapper jsonMapper;
    private final String name = "verify";
    private final String description = """
        Runs LiquidJava and returns the same high-level terminal output a developer would see.
        Prefer it over `get_diagnostics` for quick checks, to reduce token usage, or to inspect debug information by setting `debug` to true.
        Debug output shows verification conditions, their simplifications, and solver results, including counterexamples.
        Receives a file or directory path to verify and returns the verification status and plain-text LiquidJava output.
    """;

    public VerifyTool(Verifier verifier, McpJsonMapper jsonMapper) {
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
        content.put("output", result.output());
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
