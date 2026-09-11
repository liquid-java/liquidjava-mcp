package liquidjava.mcp.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import liquidjava.mcp.utils.PathUtils;

public record AnalysisKey(Path path, boolean debug, List<Source> sources) {
    private record Source(Path path, String hash) {}

    public static AnalysisKey read(String input, boolean debug) throws IOException {
        Path path = PathUtils.canonicalPath(input);
        List<Path> files = sourceFiles(path);
        List<Source> sources = new ArrayList<>();
        for (Path file : files) {
            sources.add(new Source(file, hash(file)));
        }
        return new AnalysisKey(path, debug, List.copyOf(sources));
    }

    private static List<Path> sourceFiles(Path path) throws IOException {
        if (!Files.isDirectory(path)) return List.of(path);
        try (Stream<Path> paths = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
            return paths.filter(PathUtils::isJavaSourceFile).sorted().toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static String hash(Path file) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
