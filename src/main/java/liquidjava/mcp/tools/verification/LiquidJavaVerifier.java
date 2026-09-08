package liquidjava.mcp.tools.verification;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.mcp.runtime.LiquidJavaRunner;
import liquidjava.mcp.tools.McpErrorCode;

public final class LiquidJavaVerifier implements Verifier {
    @Override
    public VerifyResult verify(VerifyRequest request) {
        return LiquidJavaRunner.run(request.path(), false, request.debug(), output -> {
            Diagnostics diagnostics = Diagnostics.getInstance();
            return VerifyResult.completed(
                !diagnostics.foundError(), output,
                DiagnosticMapper.snapshot(diagnostics.getErrors()),
                DiagnosticMapper.snapshot(diagnostics.getWarnings())
            );
        }, (message, output) -> VerifyResult.failed(McpErrorCode.VERIFIER_ERROR, message, output));
    }
}
