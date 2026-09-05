package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import liquidjava.mcp.tools.schemas.ToolSchemas;
import liquidjava.mcp.verification.ContextRequest;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.verification.VerifyResult;

public final class GetGlobalsTool {
    private final LiquidJavaVerifier verifier;
    private final McpJsonMapper jsonMapper;
    private final String name = "get_globals";
    private final String description = """
        Runs LiquidJava on a Java file and returns global aliases, ghosts, and states.
    """;

    public GetGlobalsTool(LiquidJavaVerifier verifier, McpJsonMapper jsonMapper) {
        this.verifier = verifier;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification specification() {
        ToolSchemas schemas = ToolSchemas.load(name, jsonMapper);
        Tool tool = Tool.builder(name, schemas.inputSchema())
                .description(description.stripIndent().trim())
                .outputSchema(schemas.outputSchema())
                .annotations(ToolAnnotations.builder().readOnlyHint(true).destructiveHint(false)
                        .idempotentHint(true).openWorldHint(false).build()).build();
        return SyncToolSpecification.builder().tool(tool)
                .callHandler((exchange, request) -> call(request.arguments())).build();
    }

    public CallToolResult call(Map<String, Object> arguments) {
        ContextRequest request;
        try {
            request = ContextRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(VerifyResult.failed(VerifyResult.ErrorCode.INVALID_INPUT, e.getMessage(), ""), Map.of());
        }
        var result = verifier.getGlobals(request);
        return toMcpResult(result.verification(), result.context());
    }

    private CallToolResult toMcpResult(VerifyResult result, Map<String, Object> context) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        content.put("aliases", context.getOrDefault("aliases", List.of()));
        content.put("ghosts", context.getOrDefault("ghosts", List.of()));
        content.put("states", context.getOrDefault("states", List.of()));
        if (result.error() != null)
            content.put("error", Map.of("code", result.error().code().name(), "message", result.error().message()));
        try {
            return CallToolResult.builder().structuredContent(content)
                    .addTextContent(jsonMapper.writeValueAsString(content))
                    .isError(result.error() != null).build();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize context result", e);
        }
    }
}
