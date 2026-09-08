package liquidjava.mcp.tools.fsm;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

public record StateMachineRequest(String path) {
    public StateMachineRequest {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("path must be a nonblank string");
        try {
            Path source = Path.of(path).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source) || !source.getFileName().toString().endsWith(".java"))
                throw new IllegalArgumentException("path must be an existing Java source file");
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        }
    }

    public static StateMachineRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !arguments.keySet().equals(java.util.Set.of("path")))
            throw new IllegalArgumentException("expected exactly one argument: path");
        if (!(arguments.get("path") instanceof String path))
            throw new IllegalArgumentException("path must be a nonblank string");
        return new StateMachineRequest(path);
    }
}
