package liquidjava.mcp.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record AnalysisKey(Path path, boolean debug, List<Source> sources) {
    private record Source(Path path, String hash) {}

    public static AnalysisKey read(String input, boolean debug) throws IOException {
        Path path = Path.of(input).toRealPath();
        List<Path> files;
        if (Files.isDirectory(path)) {
            try (var paths = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                files = paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .sorted().toList();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        } else {
            files = List.of(path);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            var sources = new java.util.ArrayList<Source>();
            for (Path file : files) {
                try (var stream = Files.newInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
                }
                sources.add(new Source(file, HexFormat.of().formatHex(digest.digest())));
            }
            return new AnalysisKey(path, debug, List.copyOf(sources));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
