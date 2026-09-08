package liquidjava.mcp.tools.validity;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import liquidjava.mcp.tools.McpErrorCode;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import org.junit.jupiter.api.Test;

class ValidityCheckerTest {
    private final ValidityRequest request = new ValidityRequest(Map.of(), List.of(), "true");

    @Test
    void reportsExecutionFailures() {
        var checker = new ValidityChecker(new SMTEvaluator() {
            @Override
            public SMTResult verifySubtype(Predicate premises, Predicate conclusion, Context context, boolean silent) {
                throw new UnsatisfiedLinkError("native solver unavailable");
            }
        });
        assertEquals(ValidityResult.failed(McpErrorCode.VERIFIER_ERROR, "native solver unavailable"), checker.check(request));
    }
}
