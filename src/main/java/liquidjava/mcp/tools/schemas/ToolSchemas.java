package liquidjava.mcp.tools.schemas;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public record ToolSchemas(Map<String, Object> inputSchema, Map<String, Object> outputSchema) {
    public static ToolSchemas load(String name, McpJsonMapper jsonMapper) {
        String resourcePath = String.format("/schemas/%s.json", name);
        try (InputStream resource = ToolSchemas.class.getResourceAsStream(resourcePath)) {
            if (resource == null)
                throw new IllegalStateException("Missing tool schemas resource: " + resourcePath);

            String content = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Map<String, Object>> schemas = jsonMapper.readValue(content, new TypeRef<>() {});
            return new ToolSchemas(
                Objects.requireNonNull(schemas.get("inputSchema"), "missing input schema"),
                Objects.requireNonNull(schemas.get("outputSchema"), "missing output schema")
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load tool schemas resource: " + resourcePath, e);
        }
    }
}
