package liquidjava.mcp.tools.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import java.util.Map;
import liquidjava.mcp.tools.McpError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.Variable;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.parsing.RefinementsParser;
import liquidjava.smt.ExpressionToZ3Visitor;
import liquidjava.smt.TranslatorToZ3;
import spoon.Launcher;
import spoon.reflect.factory.Factory;

public final class SmtChecker {

    public SmtResult check(SmtRequest request) {
        try {
            Factory factory = new Launcher().getFactory();
            Context context = createContext(request.variables(), request.ghosts(), factory);
            Predicate constraints = new Predicate();
            try (TranslatorToZ3 translator = new TranslatorToZ3(context)) {
                ExpressionToZ3Visitor visitor = new ExpressionToZ3Visitor(translator);
                for (String text : request.constraints()) {
                    Predicate predicate = parse(text, request.variables(), request.ghosts(), context, factory, visitor);
                    constraints = Predicate.createConjunction(constraints, predicate);
                }
                Expr<?> expression = constraints.getExpression().accept(visitor);
                Solver solver = translator.makeSolverForExpression(expression);
                return switch (solver.check()) {
                    case SATISFIABLE -> {
                        Model model = solver.getModel();
                        yield SmtResult.sat(translator.getCounterexample(model));
                    }
                    case UNSATISFIABLE -> SmtResult.unsat();
                    case UNKNOWN -> SmtResult.unknown();
                };
            }
        } catch (IllegalArgumentException e) {
            return SmtResult.failed(McpError.Code.INVALID_INPUT, getMessage(e));
        } catch (Exception | LinkageError e) {
            return SmtResult.failed(McpError.Code.VERIFIER_ERROR, getMessage(e));
        }
    }

    private static Context createContext(Map<String, String> variables, Map<String, GhostDeclaration> ghosts,
            Factory factory) {
        Context context = Context.create();
        variables.forEach((name, type) ->
            context.addVarToContext(new Variable(name, factory.Type().createReference(type), new Predicate()))
        );
        ghosts.forEach((name, decl) -> 
            context.addGhostFunction(new GhostFunction(new GhostDTO(name, decl.parameterTypes(), decl.returnType()), factory, ""))
        );
        return context;
    }

    private static Predicate parse(String text, Map<String, String> variables, Map<String, GhostDeclaration> ghosts, Context context, Factory factory, ExpressionToZ3Visitor visitor) {
        try {
            Expression expression = RefinementsParser.createAST(text, "");
            validateReferences(expression, variables, ghosts);
            expression.validateGhostInvocations(context, factory);
            if (!(expression.accept(visitor) instanceof BoolExpr))
                throw new IllegalArgumentException("expected a boolean predicate");
            return new Predicate(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid predicate '" + text + "': " + getMessage(e), e);
        }
    }

    private static void validateReferences(Expression expression, Map<String, String> variables, Map<String, GhostDeclaration> ghosts) {
        if (expression == null)
            throw new IllegalArgumentException("expected a predicate expression");

        if (expression instanceof Var variable && !variables.containsKey(variable.getName()))
            throw new IllegalArgumentException("undeclared variable: " + variable.getName());

        if (expression instanceof AliasInvocation || expression instanceof Enum)
            throw new IllegalArgumentException("aliases and source constants are not supported");

        if (expression instanceof FunctionInvocation function && !ghosts.containsKey(function.getName()))
            throw new IllegalArgumentException("undeclared ghost function: " + function.getName());
        
        for (Expression child : expression.getChildren())
            validateReferences(child, variables, ghosts);
    }

    private static String getMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
