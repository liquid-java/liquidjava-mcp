package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import liquidjava.mcp.verification.Verifier;
import liquidjava.mcp.verification.VerifyRequest;
import liquidjava.mcp.verification.VerifyResult;

public final class VerifyTool {
    private final Verifier verifier;
    private final McpJsonMapper jsonMapper;
    private final String schemaResourcePath = "/schemas/verify.json";
    private final String description = """
        Run normal LiquidJava verification on one or more file/directory paths.
        Relative paths use the server's startup working directory.
        Returns success and plain-text LiquidJava output;
        Verification errors set success=false.
    """;

    public VerifyTool(Verifier verifier, McpJsonMapper jsonMapper) {
        this.verifier = verifier;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification specification() {
        Map<String, Map<String, Object>> schemas = loadSchemas();
        Map<String, Object> inputSchema = Objects.requireNonNull(schemas.get("inputSchema"), "missing input schema");
        Map<String, Object> outputSchema = Objects.requireNonNull(schemas.get("outputSchema"), "missing output schema");
        Tool tool = Tool.builder("verify", inputSchema)
            .description(description.stripIndent().trim())
            .outputSchema(outputSchema)
            .annotations(ToolAnnotations.builder().readOnlyHint(true).destructiveHint(false)
            .idempotentHint(true).openWorldHint(false).build())
            .build();
        return SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> call(request.arguments())).build();
    }

    private Map<String, Map<String, Object>> loadSchemas() {
        try (InputStream resource = VerifyTool.class.getResourceAsStream(schemaResourcePath)) {
            if (resource == null) 
                throw new IllegalStateException("Missing tool schemas resource: " + schemaResourcePath);
            
            return jsonMapper.readValue(new String(resource.readAllBytes(), StandardCharsets.UTF_8), new TypeRef<>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load tool schemas resource: " + schemaResourcePath, e);
        }
    }

    CallToolResult call(Map<String, Object> arguments) {
        VerifyRequest request;
        try {
            request = parseRequest(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(VerifyResult.failed(VerifyResult.ErrorCode.INVALID_INPUT, e.getMessage(), ""));
        }
        return toMcpResult(verifier.verify(request));
    }

    private static VerifyRequest parseRequest(Map<String, Object> arguments) {
        if (arguments == null || !arguments.keySet().equals(java.util.Set.of("paths")))
            throw new IllegalArgumentException("expected exactly one argument: paths");

        if (!(arguments.get("paths") instanceof List<?> paths))
            throw new IllegalArgumentException("paths must be an array of strings");
    
        List<String> values = new ArrayList<>(paths.size());
        for (Object path : paths) {
            if (!(path instanceof String value))
                throw new IllegalArgumentException("each path must be a nonblank string");

            values.add(value);
        }
        return new VerifyRequest(values);
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
