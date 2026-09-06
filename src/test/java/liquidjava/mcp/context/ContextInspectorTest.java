package liquidjava.mcp.context;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import liquidjava.api.CommandLineLauncher;
import liquidjava.mcp.verification.LiquidJavaVerifier;
import liquidjava.mcp.verification.VerifyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
@ResourceLock("java.lang.System.err")
class ContextInspectorTest {
    @TempDir Path temporary;

    @Test
    void concurrentContextAndVerificationCallsKeepSeparateSnapshots() throws Exception {
        var jobs = new ArrayList<Callable<Void>>();
        var request = new ContextRequest("src/test/resources/examples/Context.java",
                "src/test/resources/examples/Context.java", 12, 9);
        var globalsRequest = new ContextRequest("src/test/resources/examples/Context.java",
                "src/test/resources/examples/Context.java", null, null);
        for (int i = 0; i < 4; i++) {
            jobs.add(() -> {
                var result = new ContextInspector().getLocals(request);
                assertNull(result.error(), result.toString());
                var variables = (List<?>) result.context().get("variables");
                assertTrue(variables.stream().anyMatch(v -> ((Map<?, ?>) v).get("name").equals("input")));
                assertFalse(variables.stream().anyMatch(v -> ((Map<?, ?>) v).get("name").equals("nested")));
                return null;
            });
            jobs.add(() -> {
                var result = new ContextInspector().getGlobals(globalsRequest);
                assertNull(result.error(), result.toString());
                assertEquals(2, ((List<?>) result.context().get("states")).size());
                return null;
            });
            jobs.add(() -> {
                var result = new LiquidJavaVerifier().verify(new VerifyRequest(List.of("src/test/resources/examples/Invalid.java"), false));
                assertFalse(result.success());
                assertNull(result.error());
                assertFalse(result.errors().isEmpty());
                assertFalse(result.output().contains("Context.java"));
                return null;
            });
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            for (var result : pool.invokeAll(jobs)) result.get();
        }
        assertFalse(CommandLineLauncher.cmdArgs.lspMode);
    }

    @Test
    void executionFailureRestoresOutputAndDoesNotLeakContext() throws Exception {
        Path broken = temporary.resolve("Duplicate.java");
        Files.writeString(broken, "class Duplicate {} class Duplicate {}");
        var inspector = new ContextInspector();
        var valid = new ContextRequest("src/test/resources/examples/Context.java",
                "src/test/resources/examples/Context.java", null, null);
        var before = inspector.getGlobals(valid);
        assertNull(before.error());
        String snapshot = before.toString();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (var capture = new PrintStream(new ByteArrayOutputStream())) {
            System.setErr(capture);
            var result = inspector.getGlobals(new ContextRequest(broken.toString(), broken.toString(), null, null));
            assertNotNull(result.error());
            assertEquals(ContextResult.ErrorCode.VERIFIER_ERROR, result.error().code());
            assertTrue(result.context().isEmpty());
        } finally {
            System.setErr(originalErr);
        }
        assertSame(originalOut, System.out);
        assertFalse(CommandLineLauncher.cmdArgs.lspMode);
        assertEquals(snapshot, before.toString());
        assertEquals(before, inspector.getGlobals(valid));
    }
}
