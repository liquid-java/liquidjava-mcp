package liquidjava.mcp.verification;

import java.util.Map;

public record ContextResult(VerifyResult verification, Map<String, Object> context) {
    public ContextResult {
        context = Map.copyOf(context);
    }
}
