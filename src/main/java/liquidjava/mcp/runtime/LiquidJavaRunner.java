package liquidjava.mcp.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import liquidjava.api.CommandLineArgs;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.utils.Utils;
import liquidjava.processor.context.Context;

public final class LiquidJavaRunner {
    private static final Object LOCK = new Object();

    private LiquidJavaRunner() {}

    public static <T> T run(List<String> paths, boolean captureContext,
            Function<String, T> snapshot, BiFunction<String, String, T> failure) {
        synchronized (LOCK) {
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
                String[] arguments = new String[paths.size() + 1];
                arguments[0] = "--";
                for (int i = 0; i < paths.size(); i++) {
                    arguments[i + 1] = paths.get(i);
                }
                CommandLineLauncher.main(arguments);
                return snapshot.apply(Utils.getPlainOutput(bytes));
            } catch (Exception | LinkageError e) {
                e.printStackTrace(System.err);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return failure.apply(message, Utils.getPlainOutput(bytes));
            } finally {
                CommandLineLauncher.cmdArgs.lspMode = false;
                System.setOut(previousOut);
            }
        }
    }
}
