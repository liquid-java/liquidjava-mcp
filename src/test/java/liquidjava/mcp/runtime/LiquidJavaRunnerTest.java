package liquidjava.mcp.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import liquidjava.mcp.tools.context.ContextInspector;
import liquidjava.mcp.tools.context.ContextRequest;
import liquidjava.mcp.tools.verification.LiquidJavaVerifier;
import liquidjava.mcp.tools.verification.VerifyRequest;
import liquidjava.processor.context.ContextHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("liquidjava-global-state")
@ResourceLock("java.lang.System.out")
@ResourceLock("java.lang.System.err")
class LiquidJavaRunnerTest {
    @TempDir Path temporary;

    private Path source() throws Exception {
        return Files.copy(Path.of("src/test/resources/examples/Context.java"), temporary.resolve("Context.java"));
    }

    private Object analyze(Path path, boolean debug) {
        var result = new LiquidJavaVerifier().verify(new VerifyRequest(path.toString(), debug));
        assertNull(result.error(), result.toString());
        return ContextHistory.getInstance().getLocalVars().iterator().next();
    }

    @Test
    void relatedQueriesReuseTheSameAnalysisAcrossCanonicalPaths() throws Exception {
        Path file = source();
        var inspector = new ContextInspector();
        var request = new ContextRequest(file.toString(), file.toString(), 12, 9);
        assertNull(inspector.getLocals(request).error());
        Object first = ContextHistory.getInstance().getLocalVars().iterator().next();
        assertSame(first, analyze(file.toRealPath(), false));
        assertNull(inspector.getGlobals(request).error());
        assertSame(first, ContextHistory.getInstance().getLocalVars().iterator().next());
        assertSame(first, analyze(file.getParent().resolve(".").resolve(file.getFileName()), false));
    }

    @Test
    void contentChangesInvalidateEvenWithSameSizeAndTimestamp() throws Exception {
        Path file = source();
        Object first = analyze(file, false);
        FileTime timestamp = Files.getLastModifiedTime(file);
        Files.writeString(file, Files.readString(file).replace("input", "other"));
        Files.setLastModifiedTime(file, timestamp);
        Object changed = analyze(file, false);
        assertNotSame(first, changed);
        assertSame(changed, analyze(file, false));
    }

    @Test
    void changedContentsRefreshDiagnostics() throws Exception {
        Path file = Files.copy(Path.of("src/test/resources/examples/Invalid.java"), temporary.resolve("Invalid.java"));
        var verifier = new LiquidJavaVerifier();
        var request = new VerifyRequest(file.toString(), false);
        var invalid = verifier.verify(request);
        assertFalse(invalid.success());
        assertFalse(invalid.errors().isEmpty());
        assertEquals(invalid, verifier.verify(request));
        Files.writeString(file, Files.readString(file).replace("= -1", "= 1"));
        var valid = verifier.verify(request);
        assertTrue(valid.success(), valid.toString());
        assertTrue(valid.errors().isEmpty());
        assertFalse(invalid.errors().isEmpty());
    }

    @Test
    void directoryEditsAdditionsAndDeletionsInvalidate() throws Exception {
        Path file = source();
        Object first = analyze(temporary, false);
        Path added = Files.createDirectories(temporary.resolve("nested")).resolve("Added.java");
        Files.writeString(added, "class Added {}");
        Object second = analyze(temporary, false);
        assertNotSame(first, second);
        Files.delete(added);
        Object third = analyze(temporary, false);
        assertNotSame(second, third);
        Files.writeString(file, Files.readString(file).replace("input", "other"));
        assertNotSame(third, analyze(temporary, false));
    }

    @Test
    void optionsAndPathsInvalidate() throws Exception {
        Path file = source();
        Object first = analyze(file, false);
        Object debug = analyze(file, true);
        assertNotSame(first, debug);
        assertSame(debug, analyze(file, true));
        Object directory = analyze(temporary, true);
        assertNotSame(debug, directory);
        assertNotSame(directory, analyze(file, true));
    }
}
