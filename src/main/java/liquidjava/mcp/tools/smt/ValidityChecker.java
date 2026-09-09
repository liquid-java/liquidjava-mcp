package liquidjava.mcp.tools.smt;

import java.util.ArrayList;
import java.util.List;

public final class ValidityChecker {
    private final SmtChecker smtChecker = new SmtChecker();

    public ValidityResult check(ValidityRequest request) {
        List<String> constraints = new ArrayList<>(request.assumptions());
        constraints.add("!(" + request.conclusion() + ")");
        SmtResult result = smtChecker.check(new SmtRequest(request.variables(), request.ghosts(), constraints));
        if (result.error() != null)
            return ValidityResult.failed(result.error().code(), result.error().message());
        return switch (result.status()) {
            case SAT -> ValidityResult.invalid(result.assignment());
            case UNSAT -> ValidityResult.valid();
            case UNKNOWN -> ValidityResult.unknown();
        };
    }
}
