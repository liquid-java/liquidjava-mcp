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
import java.util.Map;
import java.util.Objects;

/**
 * Provides the common MCP lifecycle for LiquidJava tools.
 */
public abstract class AbstractMcpTool {
    private final SyncToolSpecification specification;
    private final McpJsonMapper jsonMapper;

    protected AbstractMcpTool(String name, String description, McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        Schemas schemas = Schemas.load(name, jsonMapper);
        Tool tool = Tool.builder(name, schemas.inputSchema())
            .description(description.stripIndent().trim())
            .outputSchema(schemas.outputSchema())
            .annotations(
                ToolAnnotations.builder()
                    .readOnlyHint(true)
                    .destructiveHint(false)
                    .idempotentHint(true)
                    .openWorldHint(false)
                    .build()
            )
            .build();
        this.specification = SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> call(request.arguments()))
            .build();
    }

    public final SyncToolSpecification specification() {
        return specification;
    }

    public abstract CallToolResult call(Map<String, Object> arguments);

    protected final void addError(Map<String, Object> content, String code, String message) {
        content.put("error", Map.of("code", code, "message", message));
    }

    protected final CallToolResult result(Map<String, Object> content, boolean error) {
        try {
            return CallToolResult.builder()
                .structuredContent(content)
                .addTextContent(jsonMapper.writeValueAsString(content))
                .isError(error)
                .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize tool result", e);
        }
    }

    private record Schemas(Map<String, Object> inputSchema, Map<String, Object> outputSchema) {
        static Schemas load(String name, McpJsonMapper jsonMapper) {
            String resourcePath = String.format("/schemas/%s.json", name);
            try (InputStream resource = AbstractMcpTool.class.getResourceAsStream(resourcePath)) {
                if (resource == null)
                    throw new IllegalStateException("Missing tool schemas resource: " + resourcePath);

                String content = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Map<String, Object>> schemas = jsonMapper.readValue(content, new TypeRef<>() {});
                return new Schemas(
                        Objects.requireNonNull(schemas.get("inputSchema"), "missing input schema"),
                        Objects.requireNonNull(schemas.get("outputSchema"), "missing output schema"));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not load tool schemas resource: " + resourcePath, e);
            }
        }
    }
}
