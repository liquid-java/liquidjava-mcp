package liquidjava.mcp.tools.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import liquidjava.mcp.utils.PathUtils;

public record ContextRequest(String path, String file, Integer line, Integer column) {
    public ContextRequest {
        Path verificationPath = PathUtils.requireExisting(path);
        path = verificationPath.toString();

        if (file != null) {
            Path sourceFile = PathUtils.requireExistingJavaFile(file, "file");
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
