package liquidjava.mcp.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import liquidjava.diagnostics.TranslationTable;
import liquidjava.diagnostics.errors.RefinementError;
import liquidjava.diagnostics.errors.StateRefinementError;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.opt.VCSimplificationResult;
import liquidjava.smt.Counterexample;
import org.junit.jupiter.api.Test;

class DiagnosticMapperTest {
    @Test
    void exposesSimplifiedFoundAndBothVerificationConditionsForBothRefinementErrors() throws Exception {
        var original = new VCSimplificationResult(new VCImplication(new Predicate(RefinementsParser.createAST("x == 1 && x > 0 && true", ""))));
        var intermediate = new VCSimplificationResult(new VCImplication(new Predicate(RefinementsParser.createAST("x == 1 && x > 0", ""))), original, "first");
        var simplified = new VCSimplificationResult(new VCImplication(new Predicate(RefinementsParser.createAST("x == 1", ""))), intermediate, "second");
        var expected = new Predicate(RefinementsParser.createAST("x > 1", ""));
        for (var found : List.of(original, simplified)) {
            var diagnostics = List.of(
                    new RefinementError(null, null, expected, found, new TranslationTable(), new Counterexample(List.of()), null),
                    new StateRefinementError(null, null, expected, found, new TranslationTable(), null));
            for (var diagnostic : diagnostics) {
                var mapped = DiagnosticMapper.map(diagnostic);
                assertEquals(Map.of(
                        "expected", expected.toString(),
                        "found", found.getImplication().toPredicate().toString()),
                        mapped.get("refinements"));
                assertEquals(Map.of(
                        "simplified", found.getImplication() + " => \n" + expected,
                        "original", original.getImplication() + " => \n" + expected),
                        mapped.get("vc"));
            }
        }
    }
}
