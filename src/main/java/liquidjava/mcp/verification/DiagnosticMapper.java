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
        put(result, "type", diagnostic.getClass().getSimpleName());
        put(result, "severity", diagnostic instanceof LJError ? "error" : "warning");
        put(result, "message", diagnostic.getMessage());
        put(result, "location", Utils.mapPosition(diagnostic.getPosition()));
        put(result, "declarationLocation", Utils.mapPosition(diagnostic.getDeclarationPosition()));
        put(result, "hint", diagnostic.getHint());
        put(result, "customMessage", diagnostic.getCustomMessage());
        put(result, "refinements", getRefinements(diagnostic));;
        put(result, "vc", getVC(diagnostic));
        put(result, "counterexample", getCounterexample(diagnostic));
        return Map.copyOf(result);
    }

    private static Map<String, String> getRefinements(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case RefinementError e -> getRefinementsMap(e.getExpected(), e.getFound());
            case StateRefinementError e -> getRefinementsMap(e.getExpected(), e.getFoundSimplification());
            case InvalidRefinementError e -> Map.of("refinement", e.getRefinement());
            case SyntaxError e -> Map.of("refinement", e.getRefinement());
            case StateConflictError e -> Map.of("state", e.getState());
            case UnsatisfiableRefinementWarning w -> Map.of("refinement", w.getRefinement());
            default -> null;
        };
    }

    private static Map<String, String> getRefinementsMap(Predicate expected,VCSimplificationResult found) {
        return Map.of(
            "expected", expected.toString(),
            "found", found.getImplication().toPredicate().toString()
        );
    }

    private static Map<String, String> getVC(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case RefinementError e -> getVCMap(e.getExpected(), e.getFound());
            case StateRefinementError e -> getVCMap(e.getExpected(), e.getFoundSimplification());
            default -> null;
        };
    }

    private static Map<String, String> getVCMap(Predicate expected, VCSimplificationResult found) {
        return Map.of(
            "simplified", formatVC(found, expected),
            "original", formatVC(getOriginalVC(found), expected)
        );
    }

    private static VCSimplificationResult getOriginalVC(VCSimplificationResult result) {
        VCSimplificationResult original = result;
        while (original.getOrigin() != null) original = original.getOrigin();
        return original;
    }

    private static String formatVC(VCSimplificationResult result, Predicate expected) {
        return result.getImplication() + " => \n" + expected;
    }

    private static List<Map<String, String>> getCounterexample(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case RefinementError e -> e.getCounterexample()
                .assignments()
                .stream()
                .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList();
            default -> null;
        };
    }

    private static void put(Map<String, Object> result, String key, Object value) {
        if (value != null) result.put(key, value);
    }
}
