package liquidjava.mcp.tools.fsm;

import java.util.Map;
import liquidjava.mcp.utils.PathUtils;

public record StateMachineRequest(String path) {
    public StateMachineRequest {
        PathUtils.requireExistingJavaFile(path, "path");
    }

    public static StateMachineRequest fromArguments(Map<String, Object> arguments) {
        return new StateMachineRequest((String) arguments.get("path"));
    }
}
