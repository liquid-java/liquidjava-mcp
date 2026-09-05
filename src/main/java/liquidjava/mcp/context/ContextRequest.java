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
        if (line != null || column != null) {
            if (line == null || column == null || line < 1 || column < 1)
                throw new IllegalArgumentException("line and column must both be positive integers");
        }
    }

    public static ContextRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !(arguments.get("file") instanceof String file))
            throw new IllegalArgumentException("expected file and optionally both line and column");

        if (arguments.keySet().equals(Set.of("file")))
            return new ContextRequest(file, null, null);

        if (!arguments.keySet().equals(Set.of("file", "line", "column")))
            throw new IllegalArgumentException("expected file and optionally both line and column");

        int line = coordinate(arguments.get("line"));
        int column = coordinate(arguments.get("column"));
        return new ContextRequest(file, line, column);
    }

    private static int coordinate(Object value) {
        if (!(value instanceof Number number))
            throw new IllegalArgumentException("line and column must be positive integers");

        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("line and column must be positive integers", e);
        }
    }
}
