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
import java.util.function.Function;
import liquidjava.mcp.tools.schemas.ToolSchemas;
import liquidjava.mcp.validity.ValidityRequest;
import liquidjava.mcp.validity.ValidityResult;

public final class CheckValidityTool {
    private final Function<ValidityRequest, ValidityResult> checker;
    private final McpJsonMapper jsonMapper;
    private final String name = "check_validity";
    private final String description = """
        Checks whether explicit assumptions imply a conclusion using LiquidJava's solver, without verifying any Java files.
        Receives `variables` (map of names to types), `assumptions` (array of boolean predicates), and one `conclusion` (boolean predicate).
        Supported types: boolean, short, char, int, long, float, and double.
        Uses LiquidJava's solver semantics.
        Does not support ghost functions, states, aliases, source constants, or implicit bindings.
        Empty assumptions mean true, while contradictory assumptions make every conclusion valid.
        Returns `status` valid, invalid (with counterexample assignments), or unknown (with a reason).
    """;

    public CheckValidityTool(Function<ValidityRequest, ValidityResult> checker, McpJsonMapper jsonMapper) {
        this.checker = checker;
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
        ValidityRequest request;
        try {
            request = ValidityRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return toMcpResult(ValidityResult.failed("INVALID_INPUT", e.getMessage()));
        }
        return toMcpResult(checker.apply(request));
    }

    private CallToolResult toMcpResult(ValidityResult result) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("success", result.success());
        if (result.status() != null) content.put("status", result.status());
        if (result.counterexample() != null) content.put("counterexample", result.counterexample());
        if (result.reason() != null) content.put("reason", result.reason());
        if (result.error() != null)
            content.put("error", Map.of("code", result.error().code(), "message", result.error().message()));

        try {
            return CallToolResult.builder()
                .structuredContent(content)
                .addTextContent(jsonMapper.writeValueAsString(content))
                .isError(result.error() != null)
                .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize validity result", e);
        }
    }
}
