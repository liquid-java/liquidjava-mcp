package liquidjava.mcp.verification;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

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
}
