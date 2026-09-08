package liquidjava.mcp.tools;

/**
 * Describes an error returned by an MCP tool.
 */
public record McpError(Code code, String message) {
    public enum Code {
        INVALID_INPUT,
        VERIFIER_ERROR
    }
}
