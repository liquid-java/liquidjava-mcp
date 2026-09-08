package liquidjava.mcp.tools.context;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

public record ContextRequest(String path, String file, Integer line, Integer column) {
    public ContextRequest {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("path must be a nonblank string");
        Path verificationPath;
        try {
            verificationPath = Path.of(path).toAbsolutePath().normalize();
            if (!Files.exists(verificationPath))
                throw new IllegalArgumentException("The path " + path + " was not found");
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        }

        if (file != null) {
            if (file.isBlank())
                throw new IllegalArgumentException("file must be a nonblank string");
            Path sourceFile = Path.of(file).toAbsolutePath().normalize();
            if (!Files.isRegularFile(sourceFile) || !sourceFile.getFileName().toString().endsWith(".java"))
                throw new IllegalArgumentException("file must be an existing Java source file");
            boolean belongsToPath = Files.isDirectory(verificationPath)
                    ? sourceFile.startsWith(verificationPath)
                    : sourceFile.equals(verificationPath);
            if (!belongsToPath)
                throw new IllegalArgumentException("file must be within path");
            file = sourceFile.toString();
        }
        if (line != null || column != null) {
            if (file == null || line == null || column == null || line < 1 || column < 1)
                throw new IllegalArgumentException("line and column must both be positive integers");
        }
    }

    public static ContextRequest fromGlobalArguments(Map<String, Object> arguments) {
        return new ContextRequest((String) arguments.get("path"), (String) arguments.get("file"), null, null);
    }

    public static ContextRequest fromPositionArguments(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        int line = coordinate(arguments.get("line"));
        int column = coordinate(arguments.get("column"));
        return new ContextRequest(path, arguments.containsKey("file") ? (String) arguments.get("file") : path,
                line, column);
    }

    private static int coordinate(Object value) {
        return ((Number) value).intValue();
    }
}
