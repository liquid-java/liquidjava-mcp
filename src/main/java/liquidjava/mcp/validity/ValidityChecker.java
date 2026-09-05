package liquidjava.mcp.validity;

import com.microsoft.z3.BoolExpr;
import java.util.Map;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.Variable;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.ExpressionToZ3Visitor;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTUnknownException;
import liquidjava.smt.TranslatorToZ3;
import spoon.Launcher;

public final class ValidityChecker {
    private final SMTEvaluator evaluator;

    public ValidityChecker() {
        this(new SMTEvaluator());
    }

    ValidityChecker(SMTEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public ValidityResult check(ValidityRequest request) {
        try {
            Context context = Context.create();
            var factory = new Launcher().getFactory();
            request.variables().forEach((name, type) -> 
                context.addVarToContext(new Variable(name, factory.Type().createReference(type), new Predicate()))
            );
            Predicate assumptions = new Predicate();
            Predicate conclusion;
            try (var translator = new TranslatorToZ3(context)) {
                var visitor = new ExpressionToZ3Visitor(translator);
                for (String text : request.assumptions())
                    assumptions = Predicate.createConjunction(assumptions, parse(text, request.variables(), visitor));
                conclusion = parse(request.conclusion(), request.variables(), visitor);
            }
            var result = evaluator.verifySubtype(assumptions, conclusion, context, true);
            return result.isOk() ? ValidityResult.valid() : ValidityResult.invalid(result.getCounterexample());
        } catch (SMTUnknownException e) {
            return ValidityResult.unknown(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ValidityResult.failed("INVALID_INPUT", getMessage(e));
        } catch (Exception | LinkageError e) {
            return ValidityResult.failed("VERIFIER_ERROR", getMessage(e));
        }
    }

    private static Predicate parse(String text, Map<String, String> variables, ExpressionToZ3Visitor visitor) {
        try {
            Expression expression = RefinementsParser.createAST(text, "");
            validateReferences(expression, variables);
            if (!(expression.accept(visitor) instanceof BoolExpr))
                throw new IllegalArgumentException("expected a boolean predicate");
            return new Predicate(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid predicate '" + text + "': " + getMessage(e), e);
        }
    }

    private static void validateReferences(Expression expression, Map<String, String> variables) {
        if (expression == null)
            throw new IllegalArgumentException("expected a predicate expression");

        if (expression instanceof Var variable && !variables.containsKey(variable.getName()))
            throw new IllegalArgumentException("undeclared variable: " + variable.getName());

        if (expression instanceof AliasInvocation || expression instanceof liquidjava.rj_language.ast.Enum)
            throw new IllegalArgumentException("aliases and source constants are not supported");

        if (expression instanceof FunctionInvocation function)
            throw new IllegalArgumentException("function invocations are not supported: " + function.getName());
        
        for (Expression child : expression.getChildren())
            validateReferences(child, variables);
    }

    private static String getMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
