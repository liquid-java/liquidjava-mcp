package liquidjava.mcp.verification;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.mcp.runtime.LiquidJavaRunner;

public final class LiquidJavaVerifier implements Verifier {
    @Override
    public VerifyResult verify(VerifyRequest request) {
        return LiquidJavaRunner.run(request.paths(), false, output -> {
            Diagnostics diagnostics = Diagnostics.getInstance();
            return VerifyResult.completed(
                    !diagnostics.foundError(), output,
                    DiagnosticMapper.snapshot(diagnostics.getErrors()),
                    DiagnosticMapper.snapshot(diagnostics.getWarnings()));
        }, (message, output) -> VerifyResult.failed(VerifyResult.ErrorCode.VERIFIER_ERROR, message, output));
    }
}
