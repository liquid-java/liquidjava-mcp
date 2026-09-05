package liquidjava.mcp.validity;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;
import liquidjava.smt.SMTUnknownException;
import org.junit.jupiter.api.Test;

class ValidityCheckerTest {
    private final ValidityRequest request = new ValidityRequest(Map.of(), List.of(), "true");

    @Test
    void preservesUnknownReason() {
        var checker = new ValidityChecker(new SMTEvaluator() {
            @Override
            public SMTResult verifySubtype(Predicate premises, Predicate conclusion, Context context, boolean silent)
                    throws Exception {
                throw new SMTUnknownException("incomplete theory");
            }
        });
        assertEquals(ValidityResult.unknown("incomplete theory"), checker.check(request));
    }

    @Test
    void reportsExecutionFailures() {
        var checker = new ValidityChecker(new SMTEvaluator() {
            @Override
            public SMTResult verifySubtype(Predicate premises, Predicate conclusion, Context context, boolean silent) {
                throw new UnsatisfiedLinkError("native solver unavailable");
            }
        });
        assertEquals(ValidityResult.failed("VERIFIER_ERROR", "native solver unavailable"), checker.check(request));
    }
}
