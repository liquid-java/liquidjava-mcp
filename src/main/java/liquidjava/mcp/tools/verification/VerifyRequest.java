package liquidjava.mcp.tools.verification;

import java.util.Map;
import liquidjava.mcp.utils.PathUtils;

public record VerifyRequest(String path, boolean debug) {
    public VerifyRequest {
        PathUtils.requireValid(path, "path");
    }

    public static VerifyRequest fromArguments(Map<String, Object> arguments) {
        return new VerifyRequest((String) arguments.get("path"),
                arguments.containsKey("debug") && (Boolean) arguments.get("debug"));
    }
}
