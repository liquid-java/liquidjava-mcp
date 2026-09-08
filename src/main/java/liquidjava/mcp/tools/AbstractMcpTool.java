package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
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
import java.util.function.Function;

/**
 * Provides the common MCP lifecycle for LiquidJava tools.
 */
public abstract class AbstractMcpTool {
    private final SyncToolSpecification specification;
    private final Map<String, Object> inputSchema;

    protected AbstractMcpTool(String name, String description, McpJsonMapper jsonMapper) {
        Schemas schemas = Schemas.load(name, jsonMapper);
        this.inputSchema = schemas.inputSchema();
        Tool tool = Tool.builder(name, schemas.inputSchema())
            .description(description.stripIndent().trim())
            .outputSchema(schemas.outputSchema())
            .annotations(ToolAnnotations.builder()
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

    protected final <T> CallToolResult handleRequest(
        Map<String, Object> arguments,
        Function<Map<String, Object>, T> parser,
        Function<T, CallToolResult> handler,
        Function<String, CallToolResult> invalidInput
    ) {
        String inputError = validateInput(arguments);
        if (inputError != null)
            return invalidInput.apply(inputError);

        T request;
        try {
            request = parser.apply(arguments);
        } catch (IllegalArgumentException e) {
            return invalidInput.apply(e.getMessage());
        }
        return handler.apply(request);
    }

    private String validateInput(Map<String, Object> arguments) {
        ValidationResponse validation = McpJsonDefaults.getSchemaValidator().validate(inputSchema, arguments == null ? Map.of() : arguments);
        return validation.valid() ? null : validation.errorMessage();
    }

    protected final CallToolResult result(Map<String, Object> content) {
        return result(content, null);
    }

    protected final CallToolResult result(Map<String, Object> content, McpError error) {
        if (error != null)
            content.put("error", Map.of("code", error.code().name(), "message", error.message()));
        return result(content, error != null);
    }

    private CallToolResult result(Map<String, Object> content, boolean error) {
        return CallToolResult.builder()
            .structuredContent(content)
            .isError(error)
            .build();
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
                    Objects.requireNonNull(schemas.get("outputSchema"), "missing output schema")
                );
            } catch (IOException e) {
                throw new UncheckedIOException("Could not load tool schemas resource: " + resourcePath, e);
            }
        }
    }
}
