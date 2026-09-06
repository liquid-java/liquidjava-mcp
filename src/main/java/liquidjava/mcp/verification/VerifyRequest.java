package liquidjava.mcp.verification;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record VerifyRequest(String path, boolean debug) {
    public VerifyRequest {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("path must be a nonblank string");
        try {
            Path.of(path);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        }
    }

    public static VerifyRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !arguments.containsKey("path")
                || !Set.of("path", "debug").containsAll(arguments.keySet()))
            throw new IllegalArgumentException("expected path and optional debug");

        if (!(arguments.get("path") instanceof String path))
            throw new IllegalArgumentException("path must be a nonblank string");
        if (arguments.containsKey("debug") && !(arguments.get("debug") instanceof Boolean))
            throw new IllegalArgumentException("debug must be a boolean");

        return new VerifyRequest(path, Boolean.TRUE.equals(arguments.get("debug")));
    }
}
