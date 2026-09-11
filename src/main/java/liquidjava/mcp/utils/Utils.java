package liquidjava.mcp.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import spoon.reflect.cu.SourcePosition;

public final class Utils {
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
        return Regex.ANSI_ESCAPE.matcher(bytes.toString(StandardCharsets.UTF_8)).replaceAll("");
    }

    public static String getMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
