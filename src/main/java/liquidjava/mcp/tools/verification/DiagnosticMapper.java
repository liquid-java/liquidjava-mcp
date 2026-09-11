package liquidjava.mcp.tools.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import liquidjava.diagnostics.LJDiagnostic;
import liquidjava.diagnostics.errors.InvalidRefinementError;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.NotFoundError;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.diagnostics.errors.StateConflictError;
import liquidjava.diagnostics.errors.StateRefinementError;
import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.diagnostics.warnings.UnsatisfiableRefinementWarning;
import liquidjava.diagnostics.warnings.ExternalClassNotFoundWarning;
import liquidjava.diagnostics.warnings.ExternalMethodNotFoundWarning;
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
        put(result, "severity", severity(diagnostic).value());
        put(result, "message", diagnostic.getMessage());
        put(result, "location", Utils.mapPosition(diagnostic.getPosition()));
        put(result, "declarationLocation", Utils.mapPosition(diagnostic.getDeclarationPosition()));
        put(result, "hint", diagnostic.getHint());
        put(result, "customMessage", diagnostic.getCustomMessage());
        put(result, "refinements", getRefinements(diagnostic));
        put(result, "vc", getVC(diagnostic));
        put(result, "counterexample", getCounterexample(diagnostic));
        put(result, "details", getDetails(diagnostic));
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

    private static Map<String, String> getRefinementsMap(Predicate expected, VCSimplificationResult found) {
        return Map.of(
            "expected", expected.toString(),
            "found", found.getImplication().toPredicate().toString()
        );
    }

    private static Map<String, Object> getVC(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case RefinementError e -> getVCMap(e.getExpected(), e.getFound());
            case StateRefinementError e -> getVCMap(e.getExpected(), e.getFoundSimplification());
            default -> null;
        };
    }

    private static Map<String, Object> getVCMap(Predicate expected, VCSimplificationResult found) {
        ArrayList<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
        for (VCSimplificationResult current = found; current != null; current = current.getOrigin()) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("implication", current.getImplication().toString());
            put(step, "simplification", current.getSimplification());
            history.add(Map.copyOf(step));
        }
        return Map.of("expected", expected.toString(), "found", found.getImplication().toString(),
                "history", List.copyOf(history.reversed()));
    }

    private static Map<String, Object> getDetails(LJDiagnostic diagnostic) {
        return switch (diagnostic) {
            case NotFoundError e -> Map.of("name", e.getName(), "kind", e.getKind().name().toLowerCase(Locale.ROOT));
            case ExternalClassNotFoundWarning w -> Map.of("className", w.getClassName());
            case ExternalMethodNotFoundWarning w -> Map.of(
                    "className", w.getClassName(), "signature", w.getSignature(),
                    "overloads", List.copyOf(Arrays.asList(w.getOverloads())));
            default -> null;
        };
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

    private static Severity severity(LJDiagnostic diagnostic) {
        return diagnostic instanceof LJError ? Severity.ERROR : Severity.WARNING;
    }

    private enum Severity {
        ERROR,
        WARNING;

        private String value() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
