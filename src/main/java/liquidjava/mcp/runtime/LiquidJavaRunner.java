package liquidjava.mcp.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private static AnalysisKey cachedKey;
    private static String cachedOutput;

    private LiquidJavaRunner() {}

    /**
     * Runs LiquidJava in a separate thread, capturing its output and returning a result based on the provided snapshot and failure functions.
     * Reuses the latest unchanged analysis and maps its state while holding the runner lock.
     */
    public static <T> T run(
        String path,
        boolean debug,
        Function<String, T> snapshot,
        BiFunction<String, String, T> failure
    ) {
        synchronized (LOCK) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream previousOut = System.out;
            try (PrintStream capturedOut = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOut);

                // command line arguments
                CommandLineArgs args = CommandLineLauncher.cmdArgs;
                args.help = false;
                args.version = false;
                args.debugMode = debug;
                args.lspMode = true;
                args.paths = List.of(path);

                // reuse cached result if the analysis key matches
                AnalysisKey key = readKey(path, debug);
                if (key != null && key.equals(cachedKey))
                    return snapshot.apply(cachedOutput);

                // clear cached result and reinitialize context for a new analysis
                cachedKey = null;
                cachedOutput = null;
                Context.getInstance().reinitializeAllContext();

                // run LiquidJava on the specified path
                CommandLineLauncher.main(new String[] {"--", path});
                String output = Utils.stripAnsi(bytes);
                T result = snapshot.apply(output);

                // update cached result if the analysis key matches
                if (key != null && key.equals(readKey(path, debug))) {
                    cachedKey = key;
                    cachedOutput = output;
                }
                return result;
            } catch (Exception | LinkageError e) {
                cachedKey = null;
                cachedOutput = null;
                e.printStackTrace(System.err);
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return failure.apply(message, Utils.stripAnsi(bytes));
            } finally {
                CommandLineLauncher.cmdArgs.lspMode = false;
                CommandLineLauncher.cmdArgs.debugMode = false;
                System.setOut(previousOut);
            }
        }
    }

    private static AnalysisKey readKey(String path, boolean debug) {
        try {
            return AnalysisKey.read(path, debug);
        } catch (IOException e) {
            return null;
        }
    }
}
