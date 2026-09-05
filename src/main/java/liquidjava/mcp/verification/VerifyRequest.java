package liquidjava.mcp.verification;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record VerifyRequest(List<String> paths) {
    public VerifyRequest {
        if (paths == null || paths.isEmpty())
            throw new IllegalArgumentException("paths must contain at least one file or directory");

        for (String path : paths) {
            if (path == null || path.isBlank())
                throw new IllegalArgumentException("each path must be a nonblank string");

            try {
                Path.of(path);
            } catch (InvalidPathException e) {
                throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
            }
        }
        paths = List.copyOf(paths);
    }

    public static VerifyRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !arguments.keySet().equals(java.util.Set.of("paths")))
            throw new IllegalArgumentException("expected exactly one argument: paths");

        if (!(arguments.get("paths") instanceof List<?> paths))
            throw new IllegalArgumentException("paths must be an array of strings");

        List<String> values = new ArrayList<>(paths.size());
        for (Object path : paths) {
            if (!(path instanceof String value))
                throw new IllegalArgumentException("each path must be a nonblank string");

            values.add(value);
        }
        return new VerifyRequest(values);
    }
}
