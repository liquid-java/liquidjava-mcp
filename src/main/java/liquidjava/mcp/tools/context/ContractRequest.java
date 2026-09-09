package liquidjava.mcp.tools.context;

import java.util.Map;
import liquidjava.mcp.utils.PathUtils;

public record ContractRequest(String path, String className, String signature) {
    public ContractRequest {
        path = PathUtils.requireExisting(path).toString();
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
