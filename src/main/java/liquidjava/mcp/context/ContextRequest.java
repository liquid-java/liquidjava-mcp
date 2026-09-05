package liquidjava.mcp.context;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record ContextRequest(String file, Integer line, Integer column) {
    public ContextRequest {
        if (file == null || file.isBlank())
            throw new IllegalArgumentException("file must be a nonblank string");
        Path path = Path.of(file).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) || !file.endsWith(".java"))
            throw new IllegalArgumentException("file must be an existing Java source file");
        file = path.toString();
        if ((line == null) != (column == null) || (line != null && (line < 1 || column < 1)))
            throw new IllegalArgumentException("line and column must both be positive one-based integers");
    }

    public static ContextRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !(arguments.get("file") instanceof String file)
                || !(arguments.keySet().equals(Set.of("file", "line", "column"))
                    || arguments.keySet().equals(Set.of("file"))))
            throw new IllegalArgumentException("expected file and optionally both line and column");
        return new ContextRequest(file,
                arguments.containsKey("line") ? coordinate(arguments.get("line")) : null,
                arguments.containsKey("column") ? coordinate(arguments.get("column")) : null);
    }

    private static int coordinate(Object value) {
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).intValueExact();
            } catch (ArithmeticException | NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("line and column must be positive one-based integers");
    }
}
