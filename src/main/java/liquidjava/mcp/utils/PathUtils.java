package liquidjava.mcp.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class PathUtils {
    private PathUtils() {}

    public static Path requireExisting(String value) {
        requireNonBlank(value, "path");
        try {
            return canonicalPath(value);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        } catch (NoSuchFileException e) {
            throw new IllegalArgumentException("The path " + value + " was not found", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot resolve path " + value + ": " + e.getMessage(), e);
        }
    }

    public static Path requireExistingJavaFile(String value, String name) {
        requireNonBlank(value, name);
        Path source;
        try {
            source = canonicalPath(value);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("invalid path: " + e.getReason(), e);
        } catch (IOException e) {
            throw new IllegalArgumentException(name + " must be an existing Java source file", e);
        }
        if (!Files.isRegularFile(source) || !source.getFileName().toString().endsWith(".java"))
            throw new IllegalArgumentException(name + " must be an existing Java source file");
        return source;
    }

    public static Path canonicalPath(String path) throws IOException {
        return Path.of(path).toRealPath();
    }

    public static boolean samePath(String left, String right) {
        try {
            return canonicalPath(left).equals(canonicalPath(right));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must be a nonblank string");
        return value;
    }
}
