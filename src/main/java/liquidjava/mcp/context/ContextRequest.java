package liquidjava.mcp.context;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record ContextRequest(String path, String file, Integer line, Integer column) {
    public ContextRequest {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("path must be a nonblank string");
        try {
            if (!Files.exists(Path.of(path)))
                throw new IllegalArgumentException("The path " + path + " was not found");
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        }

        if (file != null) {
            if (file.isBlank())
                throw new IllegalArgumentException("file must be a nonblank string");
            Path sourceFile = Path.of(file).toAbsolutePath().normalize();
            if (!Files.isRegularFile(sourceFile) || !file.endsWith(".java"))
                throw new IllegalArgumentException("file must be an existing Java source file");
            file = sourceFile.toString();
        }
        if (line != null || column != null) {
            if (file == null || line == null || column == null || line < 1 || column < 1)
                throw new IllegalArgumentException("line and column must both be positive integers");
        }
    }

    public static ContextRequest fromGlobalArguments(Map<String, Object> arguments) {
        if (arguments == null || !(arguments.get("path") instanceof String path)
                || !(arguments.keySet().equals(Set.of("path"))
                    || arguments.keySet().equals(Set.of("path", "file"))))
            throw new IllegalArgumentException("expected path and optional file");

        Object file = arguments.get("file");
        if (arguments.containsKey("file") && !(file instanceof String))
            throw new IllegalArgumentException("file must be a nonblank string");

        return new ContextRequest(path, (String) file, null, null);
    }

    public static ContextRequest fromPositionArguments(Map<String, Object> arguments) {
        if (arguments == null || !(arguments.get("path") instanceof String path))
            throw new IllegalArgumentException("expected path, line, and column");

        if (!(arguments.keySet().equals(Set.of("path", "line", "column"))
                || arguments.keySet().equals(Set.of("path", "file", "line", "column"))))
            throw new IllegalArgumentException("expected path, line, and column, and optional file");

        Object fileArgument = arguments.get("file");
        if (arguments.containsKey("file") && !(fileArgument instanceof String))
            throw new IllegalArgumentException("file must be a nonblank string");

        int line = coordinate(arguments.get("line"));
        int column = coordinate(arguments.get("column"));
        return new ContextRequest(path, arguments.containsKey("file") ? (String) fileArgument : path, line, column);
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
