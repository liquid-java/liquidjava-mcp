package liquidjava.mcp.verification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import liquidjava.api.CommandLineArgs;
import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;

public final class LiquidJavaVerifier implements Verifier {
    private static final Object VERIFICATION_LOCK = new Object();

    @Override
    public VerifyResult verify(VerifyRequest request) {
        synchronized (VERIFICATION_LOCK) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream previousOut = System.out;
            try (PrintStream capturedOut = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOut);
                CommandLineArgs args = CommandLineLauncher.cmdArgs;
                args.help = false;
                args.version = false;
                args.debugMode = false;
                args.lspMode = false;
                args.paths = null;
                String[] arguments = new String[request.paths().size() + 1];
                arguments[0] = "--";
                for (int i = 0; i < request.paths().size(); i++) {
                    arguments[i + 1] = request.paths().get(i);
                }
                CommandLineLauncher.main(arguments);
                return VerifyResult.completed(!Diagnostics.getInstance().foundError(), plainOutput(bytes));
            } catch (Exception | LinkageError e) {
                e.printStackTrace(System.err);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return VerifyResult.failed(VerifyResult.ErrorCode.VERIFIER_ERROR, message, plainOutput(bytes));
            } finally {
                System.setOut(previousOut);
            }
        }
    }

    private static String plainOutput(ByteArrayOutputStream bytes) {
        return Pattern.compile("\u001B\\[[0-9;]*m").matcher(bytes.toString(StandardCharsets.UTF_8)).replaceAll("");
    }
}
