package liquidjava.mcp.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import spoon.reflect.cu.SourcePosition;

public final class Utils {
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\u001B\\[[0-9;]*m");

    private Utils() {}

    public static Map<String, Object> mapPosition(SourcePosition position) {
        if (position == null || !position.isValidPosition() || position.getFile() == null) return null;
        return Map.of(
            "file", position.getFile().getAbsolutePath(),
            "startLine", position.getLine(), "startColumn", position.getColumn(),
            "endLine", position.getEndLine(), "endColumn", position.getEndColumn()
        );
    }

    public static String stripAnsi(ByteArrayOutputStream bytes) {
        return ANSI_ESCAPE_PATTERN.matcher(bytes.toString(StandardCharsets.UTF_8)).replaceAll("");
    }

    public static boolean samePath(String left, String right) {
        return Path.of(left).toAbsolutePath().normalize().equals(Path.of(right).toAbsolutePath().normalize());
    }
}
