package liquidjava.mcp.tools.verification;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

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
        return new VerifyRequest((String) arguments.get("path"),
                arguments.containsKey("debug") && (Boolean) arguments.get("debug"));
    }
}
