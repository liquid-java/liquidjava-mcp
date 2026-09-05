package liquidjava.mcp.verification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.errors.*;
import liquidjava.diagnostics.warnings.UnsatisfiableRefinementWarning;
import spoon.reflect.cu.SourcePosition;

final class DiagnosticMapper {
    private DiagnosticMapper() {}

    public static List<Map<String, Object>> snapshot(Collection<? extends LJDiagnostic> diagnostics) {
        return diagnostics.stream().map(DiagnosticMapper::map).toList();
    }

    public static Map<String, Object> map(LJDiagnostic diagnostic) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", diagnostic.getClass().getSimpleName());
        result.put("severity", diagnostic instanceof LJError ? "error" : "warning");
        result.put("message", diagnostic.getMessage());
        put(result, "location", location(diagnostic.getPosition()));
        put(result, "declarationLocation", location(diagnostic.getDeclarationPosition()));
        put(result, "hint", diagnostic.getHint());
        put(result, "customMessage", diagnostic.getCustomMessage());
        Map<String, String> refinements = switch (diagnostic) {
            case RefinementError e -> Map.of(
                "expected", e.getExpected().toString(),
                "found", e.getFound().getImplication().toPredicate().toString()
            );
            case StateRefinementError e -> Map.of(
                "expected", e.getExpected().toString(),
                "found", e.getFound().toPredicate().toString()
            );
            case InvalidRefinementError e -> Map.of("refinement", e.getRefinement());
            case SyntaxError e -> Map.of("refinement", e.getRefinement());
            case StateConflictError e -> Map.of("state", e.getState());
            case UnsatisfiableRefinementWarning w -> Map.of("refinement", w.getRefinement());
            default -> Map.of();
        };
        if (!refinements.isEmpty()) result.put("refinements", refinements);
        if (diagnostic instanceof RefinementError error && !error.getCounterexample().isEmpty()) {
            result.put("counterexample", error.getCounterexample().assignments().stream()
                .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList());
        }
        return Map.copyOf(result);
    }

    static Map<String, Object> location(SourcePosition position) {
        if (position == null || !position.isValidPosition() || position.getFile() == null) return null;
        return Map.of(
            "file", position.getFile().getAbsolutePath(),
            "startLine", position.getLine(), "startColumn", position.getColumn(),
            "endLine", position.getEndLine(), "endColumn", position.getEndColumn()
        );
    }

    private static void put(Map<String, Object> result, String key, Object value) {
        if (value != null) result.put(key, value);
    }
}
