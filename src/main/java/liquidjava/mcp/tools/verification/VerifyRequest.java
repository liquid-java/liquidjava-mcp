package liquidjava.mcp.tools.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import liquidjava.mcp.utils.PathUtils;

public record VerifyRequest(String path, boolean debug) {
    public VerifyRequest {
        Path source = PathUtils.requireExisting(path);
        if (!Files.isDirectory(source) && (!Files.isRegularFile(source) || !source.getFileName().toString().endsWith(".java")))
            throw new IllegalArgumentException("path must be a Java source file or directory");
    }

    public static VerifyRequest fromArguments(Map<String, Object> arguments) {
        return new VerifyRequest((String) arguments.get("path"),
                arguments.containsKey("debug") && (Boolean) arguments.get("debug"));
    }
}
