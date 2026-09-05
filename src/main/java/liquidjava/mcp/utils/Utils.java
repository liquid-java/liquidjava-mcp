package liquidjava.mcp.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import spoon.reflect.cu.SourcePosition;
import java.util.regex.Pattern;

public final class Utils {

    public static Map<String, Object> mapPosition(SourcePosition position) {
        if (position == null || !position.isValidPosition() || position.getFile() == null) return null;
        return Map.of(
            "file", position.getFile().getAbsolutePath(),
            "startLine", position.getLine(), "startColumn", position.getColumn(),
            "endLine", position.getEndLine(), "endColumn", position.getEndColumn()
        );
    }

    public static String getPlainOutput(ByteArrayOutputStream bytes) {
        return Pattern.compile("\u001B\\[[0-9;]*m").matcher(bytes.toString(StandardCharsets.UTF_8)).replaceAll("");
    }
}
