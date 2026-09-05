package liquidjava.mcp.verification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.function.Supplier;

import liquidjava.api.CommandLineArgs;
import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.processor.context.Context;

public final class LiquidJavaVerifier implements Verifier {
    private static final Object VERIFICATION_LOCK = new Object();

    public ContextResult getLocals(ContextRequest request) {
        return inspect(request, () -> ContextMapper.locals(request));
    }

    public ContextResult getGlobals(ContextRequest request) {
        return inspect(request, ContextMapper::globals);
    }

    private ContextResult inspect(ContextRequest request, Supplier<Map<String, Object>> snapshot) {
        synchronized (VERIFICATION_LOCK) {
            VerifyResult result = verify(new VerifyRequest(List.of(request.file())), true);
            return new ContextResult(result, result.error() == null
                    ? snapshot.get() : Map.of());
        }
    }

    @Override
    public VerifyResult verify(VerifyRequest request) {
        return verify(request, false);
    }

    private VerifyResult verify(VerifyRequest request, boolean captureContext) {
        synchronized (VERIFICATION_LOCK) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream previousOut = System.out;
            try (PrintStream capturedOut = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOut);
                if (captureContext) Context.getInstance().reinitializeAllContext();
                CommandLineArgs args = CommandLineLauncher.cmdArgs;
                args.help = false;
                args.version = false;
                args.debugMode = false;
                args.lspMode = captureContext;
                args.paths = null;
                String[] arguments = new String[request.paths().size() + 1];
                arguments[0] = "--";
                for (int i = 0; i < request.paths().size(); i++) {
                    arguments[i + 1] = request.paths().get(i);
                }
                CommandLineLauncher.main(arguments);
                Diagnostics diagnostics = Diagnostics.getInstance();
                return VerifyResult.completed(
                    !diagnostics.foundError(),
                    plainOutput(bytes),
                    DiagnosticMapper.snapshot(diagnostics.getErrors()),
                    DiagnosticMapper.snapshot(diagnostics.getWarnings())
                );
            } catch (Exception | LinkageError e) {
                e.printStackTrace(System.err);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return VerifyResult.failed(VerifyResult.ErrorCode.VERIFIER_ERROR, message, plainOutput(bytes));
            } finally {
                CommandLineLauncher.cmdArgs.lspMode = false;
                System.setOut(previousOut);
            }
        }
    }

    private static String plainOutput(ByteArrayOutputStream bytes) {
        return Pattern.compile("\u001B\\[[0-9;]*m").matcher(bytes.toString(StandardCharsets.UTF_8)).replaceAll("");
    }
}
