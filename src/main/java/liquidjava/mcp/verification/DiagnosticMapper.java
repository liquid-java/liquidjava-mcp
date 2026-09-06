package liquidjava.mcp.verification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.errors.*;
import liquidjava.diagnostics.warnings.UnsatisfiableRefinementWarning;
import liquidjava.mcp.utils.Utils;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.opt.VCSimplificationResult;

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
        put(result, "location", Utils.mapPosition(diagnostic.getPosition()));
        put(result, "declarationLocation", Utils.mapPosition(diagnostic.getDeclarationPosition()));
        put(result, "hint", diagnostic.getHint());
        put(result, "customMessage", diagnostic.getCustomMessage());
        Map<String, String> refinements = getRefinements(diagnostic);
        if (!refinements.isEmpty()) result.put("refinements", refinements);
        if (diagnostic instanceof RefinementError error && !error.getCounterexample().isEmpty()) {
            result.put("counterexample", error.getCounterexample().assignments().stream()
                .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList());
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> getRefinements(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case RefinementError e -> getRefinementPredicates(e.getExpected(), e.getFound());
            case StateRefinementError e -> getRefinementPredicates(e.getExpected(), e.getFoundSimplification());
            case InvalidRefinementError e -> Map.of("refinement", e.getRefinement());
            case SyntaxError e -> Map.of("refinement", e.getRefinement());
            case StateConflictError e -> Map.of("state", e.getState());
            case UnsatisfiableRefinementWarning w -> Map.of("refinement", w.getRefinement());
            default -> Map.of();
        };
    }

    private static Map<String, String> getRefinementPredicates(Predicate expected, VCSimplificationResult found) {
        VCSimplificationResult original = found;
        while (original.getOrigin() != null) original = original.getOrigin();
        return Map.of(
            "expected", expected.toString(),
            "found", original.getImplication().toPredicate().toString(),
            "foundSimplified", found.getImplication().toPredicate().toString()
        );
    }

    private static void put(Map<String, Object> result, String key, Object value) {
        if (value != null) result.put(key, value);
    }
}
