package liquidjava.mcp.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import liquidjava.api.CommandLineArgs;
import liquidjava.api.CommandLineLauncher;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.warnings.CustomWarning;
import liquidjava.mcp.utils.Utils;
import liquidjava.processor.context.Context;

public final class LiquidJavaRunner {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(
        1, Thread.ofPlatform().daemon().name("liquidjava-runner").factory());
    static {
        EXECUTOR.setRemoveOnCancelPolicy(true);
    }
    private static CachedAnalysis cachedAnalysis;
    private record CachedAnalysis(AnalysisKey key, String output) {}

    private LiquidJavaRunner() {}

    /**
     * Runs analysis and snapshots on a single worker because LiquidJava uses global state.
     * Reuses the latest unchanged analysis and captures output. The timeout includes queue time.
     */
    public static <T> T run(
        String path,
        boolean debug,
        Function<String, T> snapshot,
        BiFunction<String, String, T> failure
    ) {
        return run(path, debug, snapshot, failure, TIMEOUT, () -> CommandLineLauncher.main(new String[] {"--", path}));
    }

    static <T> T run(
        String path,
        boolean debug,
        Function<String, T> snapshot,
        BiFunction<String, String, T> failure,
        Duration timeout,
        Runnable analysis
    ) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AtomicBoolean cancelled = new AtomicBoolean();
        Future<T> future = EXECUTOR.submit(() -> execute(path, debug, snapshot, failure, analysis, bytes, cancelled));
        try {
            return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException | InterruptedException e) {
            cancelled.set(true);
            future.cancel(true);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            String message = e instanceof TimeoutException
                ? "LiquidJava timed out after " + timeout.toMillis() + " ms"
                : "LiquidJava execution interrupted";
            return failure.apply(message, Utils.stripAnsi(bytes));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof RuntimeException error) throw error;
            throw new IllegalStateException(cause);
        }
    }

    private static <T> T execute(
        String path,
        boolean debug,
        Function<String, T> snapshot,
        BiFunction<String, String, T> failure,
        Runnable analysis,
        ByteArrayOutputStream bytes,
        AtomicBoolean cancelled
    ) {
        PrintStream previousOut = System.out;
        try (PrintStream capturedOut = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);

            configureArguments(path, debug);

            AnalysisKey key = readKey(path, debug);
            if (cancelled.get()) return null;
            if (key != null && cachedAnalysis != null && key.equals(cachedAnalysis.key()))
                return snapshot.apply(cachedAnalysis.output());

            cachedAnalysis = null;
            Context.getInstance().reinitializeAllContext();

            if (key != null && key.sources().isEmpty())
                return failure.apply("No Java source files found in " + path, "");

            analysis.run();
            if (cancelled.get()) return null;
            String output = Utils.stripAnsi(bytes);
            if (javaCompilationWarning())
                return failure.apply("Java compilation encountered issues.\n" + output, output);

            T result = snapshot.apply(output);
            // only cache analyses whose sources stayed unchanged throughout execution
            if (!cancelled.get() && key != null && key.equals(readKey(path, debug))) {
                cachedAnalysis = new CachedAnalysis(key, output);
            }
            return result;
        } catch (Exception | LinkageError e) {
            cachedAnalysis = null;
            if (cancelled.get()) return null;
            e.printStackTrace(System.err);
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return failure.apply(message, Utils.stripAnsi(bytes));
        } finally {
            CommandLineLauncher.cmdArgs.lspMode = false;
            CommandLineLauncher.cmdArgs.debugMode = false;
            System.setOut(previousOut);
        }
    }

    private static void configureArguments(String path, boolean debug) {
        CommandLineArgs args = CommandLineLauncher.cmdArgs;
        args.help = false;
        args.version = false;
        args.debugMode = debug;
        args.lspMode = true;
        args.paths = List.of(path);
    }

    private static boolean javaCompilationWarning() {
        return Diagnostics.getInstance().getWarnings().stream().anyMatch(w -> w instanceof CustomWarning && w.getPosition() == null);
    }

    private static AnalysisKey readKey(String path, boolean debug) {
        try {
            return AnalysisKey.read(path, debug);
        } catch (IOException e) {
            return null;
        }
    }
}
