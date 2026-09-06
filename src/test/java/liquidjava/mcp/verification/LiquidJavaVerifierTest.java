package liquidjava.mcp.verification;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import liquidjava.api.CommandLineLauncher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
@ResourceLock("java.lang.System.err")
class LiquidJavaVerifierTest {
    @TempDir Path temporary;
    private final LiquidJavaVerifier verifier = new LiquidJavaVerifier();

    private static String example(String name) {
        return Path.of("src", "test", "resources", "examples", name).toString();
    }

    private VerifyResult verify(String path) {
        return verifier.verify(new VerifyRequest(path, false));
    }

    @Test
    void verifiesAValidFile() {
        var result = verify(example("Valid.java"));
        assertTrue(result.success(), result.toString());
        assertNull(result.error());
        assertTrue(result.output().contains("Correct! Passed Verification."));
    }

    @Test
    void reportsRefinementErrorsWithoutParsingOutputForSuccess() {
        var result = verify(example("Invalid.java"));
        assertFalse(result.success());
        assertNull(result.error());
        assertTrue(result.output().contains("Refinement Error"));
        assertTrue(result.output().contains("Invalid.java:5"));
        assertFalse(result.output().contains("\u001B"));
    }

    @Test
    void warningsAloneStillPass() {
        var result = verify(example("Warning.java"));
        assertTrue(result.success(), result.toString());
        assertNull(result.error());
        assertTrue(result.output().contains("Warning"), result.output());
        assertTrue(result.output().contains("This refinement can never be true"), result.output());
        assertTrue(result.output().contains("Correct! Passed Verification."));
    }

    @Test
    void verifiesCooperatingFilesInADirectory() {
        var result = verify(example("joint"));
        assertFalse(result.success(), result.toString());
        assertNull(result.error());
        assertTrue(result.output().contains("Refinement Error"), result.output());
        assertTrue(result.output().contains("Caller.java"));
    }

    @Test
    void missingPathUsesLiquidJavaDiagnostic() {
        String missing = temporary.resolve("missing.java").toString();
        var result = verify(missing);
        assertFalse(result.success());
        assertNull(result.error());
        assertTrue(result.output().contains("The path " + missing + " was not found"));
        assertFalse(result.output().contains("Passed Verification"));
    }

    @Test
    void supportsSpacesAndUnicodeInAbsolutePaths() throws Exception {
        Path file = Files.createDirectories(temporary.resolve("espaço com acentuação")).resolve("Valid.java");
        Files.copy(Path.of(example("Valid.java")), file);
        var result = verify(file.toString());
        assertTrue(result.success(), result.toString());
        assertTrue(result.output().contains(file.toString()));
    }

    @Test
    void cliOptionsAreTreatedAsLiteralPaths() {
        var result = verify("--help");
        assertFalse(result.success());
        assertNull(result.error());
        assertTrue(result.output().contains("The path --help was not found"), result.output());
        assertFalse(result.output().contains("Usage:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Valid.java", "Invalid.java", "Warning.java", "Malformed.java", "joint"})
    void matchesDirectCliOutputExceptColors(String name) {
        String path = example(name);
        var bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (var capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            CommandLineLauncher.main(new String[] {"--", path});
        } finally {
            System.setOut(original);
        }
        String expected = bytes.toString(StandardCharsets.UTF_8).replaceAll("\u001B\\[[0-9;]*m", "");
        assertEquals(expected, verify(path).output());
    }

    @Test
    void resetsOptionsAndRestoresStdoutAfterSuccessAndException() {
        var args = CommandLineLauncher.cmdArgs;
        PrintStream output = System.out;
        PrintStream errorOutput = System.err;
        var errors = new ByteArrayOutputStream();
        try (var capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            for (String name : List.of("Valid.java", "duplicate")) {
                args.help = args.version = args.debugMode = args.lspMode = true;
                args.paths = List.of("original.java");
                var result = verify(example(name));
                if (name.equals("duplicate")) {
                    assertFalse(result.success());
                    assertNotNull(result.error(), result.toString());
                    assertEquals(VerifyResult.ErrorCode.VERIFIER_ERROR, result.error().code());
                    assertTrue(result.output().startsWith("Running LiquidJava on:"));
                    assertTrue(errors.toString(StandardCharsets.UTF_8).contains("spoon."));
                    assertFalse(result.output().contains("\tat "));
                } else {
                    assertTrue(result.success(), result.toString());
                    assertFalse(result.output().contains("[SMT]"));
                }
                assertSame(output, System.out);
                assertFalse(args.help || args.version || args.debugMode || args.lspMode);
                assertEquals(List.of(example(name)), args.paths);
            }
        } finally {
            System.setErr(errorOutput);
        }
        assertTrue(verify(example("Valid.java")).success());
    }

    @Test
    void repeatedCallsDoNotLeakDiagnostics() {
        for (int i = 0; i < 3; i++) {
            assertFalse(verify(example("Invalid.java")).success());
            var result = verify(example("Valid.java"));
            assertTrue(result.success(), result.toString());
            assertFalse(result.output().contains("Refinement Error"));
            assertTrue(result.errors().isEmpty());
            assertTrue(result.warnings().isEmpty());
        }
    }

    @Test
    void concurrentAdapterInstancesDoNotMixStateOrOutput() throws Exception {
        var jobs = new ArrayList<Callable<VerifyResult>>();
        for (int i = 0; i < 8; i++) {
            String file = example(i % 2 == 0 ? "Valid.java" : "Invalid.java");
            jobs.add(() -> new LiquidJavaVerifier().verify(new VerifyRequest(file, false)));
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            var results = pool.invokeAll(jobs);
            for (int i = 0; i < results.size(); i++) {
                var result = results.get(i).get();
                assertEquals(i % 2 == 0, result.success(), result.toString());
                assertNull(result.error());
                assertEquals(i % 2 == 0, result.errors().isEmpty());
                String other = i % 2 == 0 ? "Invalid.java" : "Valid.java";
                assertFalse(result.output().contains(other), result.output());
                assertEquals(1, result.output().split("Running LiquidJava on:", -1).length - 1);
            }
        }
    }

    @Test
    void requestRejectsInvalidPaths() {
        assertThrows(IllegalArgumentException.class, () -> new VerifyRequest(null, false));
        assertThrows(IllegalArgumentException.class, () -> new VerifyRequest(" ", false));
        assertThrows(IllegalArgumentException.class, () -> new VerifyRequest("a\u0000b", false));
    }
}
