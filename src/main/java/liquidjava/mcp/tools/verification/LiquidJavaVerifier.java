package liquidjava.mcp.tools.verification;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.mcp.runtime.LiquidJavaRunner;
import liquidjava.mcp.tools.McpError;

public final class LiquidJavaVerifier implements Verifier {
    @Override
    public VerifyResult verify(VerifyRequest request) {
        return LiquidJavaRunner.run(request.path(), request.debug(), output -> {
            Diagnostics diagnostics = Diagnostics.getInstance();
            return VerifyResult.completed(
                !diagnostics.foundError(), output,
                DiagnosticMapper.snapshot(diagnostics.getErrors()),
                DiagnosticMapper.snapshot(diagnostics.getWarnings())
            );
        }, (message, output) -> VerifyResult.failed(McpError.Code.VERIFIER_ERROR, message, output));
    }
}
