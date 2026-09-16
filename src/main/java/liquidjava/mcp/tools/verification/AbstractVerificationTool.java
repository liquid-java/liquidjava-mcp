package liquidjava.mcp.tools.verification;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpError;

abstract class AbstractVerificationTool extends AbstractMcpTool {
    private final Verifier verifier;

    protected AbstractVerificationTool(String name, String description, Verifier verifier,
            McpJsonMapper jsonMapper) {
        super(name, description, jsonMapper);
        this.verifier = verifier;
    }

    protected final CallToolResult handleVerification(Map<String, Object> arguments) {
        return handleRequest(arguments, VerifyRequest::fromArguments,
            request -> toMcpResult(verifier.verify(request)),
            message -> toMcpResult(VerifyResult.failed(McpError.Code.INVALID_INPUT, message, ""))
        );
    }

    protected abstract CallToolResult toMcpResult(VerifyResult result);
}
