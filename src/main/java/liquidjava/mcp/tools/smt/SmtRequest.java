package liquidjava.mcp.tools.smt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.parsing.RefinementsParser;

public record SmtRequest(
    Map<String, String> variables,
    Map<String, GhostDeclaration> ghosts,
    List<String> constraints
) {
    private static final String TYPE_PATTERN = "(?:[a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*";

    public SmtRequest {
        variables = Map.copyOf(variables);
        ghosts = Map.copyOf(ghosts);
        constraints = List.copyOf(constraints);
        validateVariables(variables);
        validateGhosts(ghosts);
    }

    public static SmtRequest fromArguments(Map<String, Object> arguments) {
        return new SmtRequest(
            variables(arguments),
            ghosts(arguments),
            ((List<?>) arguments.get("constraints")).stream().map(String.class::cast).toList()
        );
    }

    static Map<String, GhostDeclaration> ghosts(Map<String, Object> arguments) {
        Map<?, ?> declarations = (Map<?, ?>) arguments.get("ghosts");
        Map<String, GhostDeclaration> ghosts = new LinkedHashMap<>();
        declarations.forEach((name, declaration) ->
            ghosts.put((String) name, GhostDeclaration.fromArguments((Map<?, ?>) declaration))
        );
        return ghosts;
    }

    static Map<String, String> variables(Map<String, Object> arguments) {
        Map<?, ?> declarations = (Map<?, ?>) arguments.get("variables");
        Map<String, String> variables = new LinkedHashMap<>();
        declarations.forEach((name, type) -> variables.put((String) name, (String) type));
        return variables;
    }

    static void validateVariables(Map<String, String> variables) {
        variables.forEach((name, type) -> {
            if (!validName(name))
                throw new IllegalArgumentException("invalid variable name: " + name);
            if (!validType(type))
                throw new IllegalArgumentException("unsupported variable type: " + type);
        });
    }

    static void validateGhosts(Map<String, GhostDeclaration> ghosts) {
        ghosts.keySet().forEach(name -> {
            if (!validName(name))
                throw new IllegalArgumentException("invalid ghost function name: " + name);
        });
    }

    static void validateType(String type) {
        if (!validType(type))
            throw new IllegalArgumentException("unsupported type: " + type);
    }

    private static boolean validType(String type) {
        return type != null && type.matches(TYPE_PATTERN);
    }

    static boolean validName(String name) {
        if (name == null)
            return false;
        if (!name.matches("#*[a-zA-Z_][a-zA-Z0-9_#]*") || Set.of("_", "this", "old").contains(name))
            return false;
        try {
            return RefinementsParser.createAST(name, "") instanceof Var variable && variable.getName().equals(name);
        } catch (Exception e) {
            return false;
        }
    }
}
