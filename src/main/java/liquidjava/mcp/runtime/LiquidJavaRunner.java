package liquidjava.mcp.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Function;
import liquidjava.api.CommandLineArgs;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.utils.Utils;
import liquidjava.processor.context.Context;

public final class LiquidJavaRunner {
    private static final Object LOCK = new Object();

    private LiquidJavaRunner() {}

    /**
     * Runs LiquidJava in a separate thread, capturing its output and returning a result based on the provided snapshot and failure functions.
     */
    public static <T> T run(
        String path,
        boolean captureContext,
        boolean debug,
        Function<String, T> snapshot,
        BiFunction<String, String, T> failure
    ) {
        synchronized (LOCK) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream previousOut = System.out;
            try (PrintStream capturedOut = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOut);
                if (captureContext) Context.getInstance().reinitializeAllContext();
                CommandLineArgs args = CommandLineLauncher.cmdArgs;
                args.help = false;
                args.version = false;
                args.debugMode = debug;
                args.lspMode = captureContext;
                args.paths = null;
                CommandLineLauncher.main(new String[] {"--", path});
                return snapshot.apply(Utils.getPlainOutput(bytes));
            } catch (Exception | LinkageError e) {
                e.printStackTrace(System.err);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return failure.apply(message, Utils.getPlainOutput(bytes));
            } finally {
                CommandLineLauncher.cmdArgs.lspMode = false;
                CommandLineLauncher.cmdArgs.debugMode = false;
                System.setOut(previousOut);
            }
        }
    }
}
