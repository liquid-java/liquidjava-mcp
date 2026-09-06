package liquidjava.mcp.validity;

import java.util.List;
import java.util.Map;
import liquidjava.smt.Counterexample;

public record ValidityResult(String status, List<Map<String, String>> counterexample,
        String reason, Failure error) {
    public static ValidityResult valid() {
        return new ValidityResult("valid", null, null, null);
    }

    public static ValidityResult invalid(Counterexample counterexample) {
        List<Map<String, String>> assignments = counterexample.assignments().stream()
            .map(pair -> Map.of("variable", pair.first(), "value", pair.second())).toList();
        return new ValidityResult("invalid", assignments, null, null);
    }

    public static ValidityResult unknown(String reason) {
        return new ValidityResult("unknown", null, reason, null);
    }

    public static ValidityResult failed(String code, String message) {
        return new ValidityResult(null, null, null, new Failure(code, message));
    }

    public record Failure(String code, String message) {}
}
