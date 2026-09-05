package liquidjava.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

record ToolSchemas(Map<String, Object> inputSchema, Map<String, Object> outputSchema) {
    public static ToolSchemas load(String resourcePath, McpJsonMapper jsonMapper) {
        try (InputStream resource = ToolSchemas.class.getResourceAsStream(resourcePath)) {
            if (resource == null)
                throw new IllegalStateException("Missing tool schemas resource: " + resourcePath);

            Map<String, Map<String, Object>> schemas = jsonMapper.readValue(
                    new String(resource.readAllBytes(), StandardCharsets.UTF_8), new TypeRef<>() {});
            return new ToolSchemas(
                    Objects.requireNonNull(schemas.get("inputSchema"), "missing input schema"),
                    Objects.requireNonNull(schemas.get("outputSchema"), "missing output schema"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load tool schemas resource: " + resourcePath, e);
        }
    }
}
