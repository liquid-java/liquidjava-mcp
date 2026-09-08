package liquidjava.mcp.tools.context;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import liquidjava.mcp.utils.Utils;

public record ContractRequest(String path, String className, String signature) {
    public ContractRequest {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("path must be a nonblank string");
        try {
            path = Utils.canonicalPath(path).toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        } catch (NoSuchFileException e) {
            throw new IllegalArgumentException("The path " + path + " was not found", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot resolve path " + path + ": " + e.getMessage(), e);
        }
        if (className != null && className.isBlank())
            throw new IllegalArgumentException("className must be a nonblank string");
        if (signature != null && signature.isBlank())
            throw new IllegalArgumentException("signature must be a nonblank string");
    }

    public static ContractRequest fromArguments(Map<String, Object> arguments) {
        return new ContractRequest(
            (String) arguments.get("path"),
            (String) arguments.get("className"),
            (String) arguments.get("signature")
        );
    }
}
