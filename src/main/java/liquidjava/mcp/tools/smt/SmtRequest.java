package liquidjava.mcp.tools.smt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SmtRequest(
    Map<String, String> variables,
    Map<String, GhostDeclaration> ghosts,
    List<String> constraints
) {
    public SmtRequest {
        variables = copyVariables(variables);
        ghosts = copyGhosts(ghosts);
        constraints = List.copyOf(constraints);
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

    static Map<String, String> copyVariables(Map<String, String> variables) {
        return Map.copyOf(variables);
    }

    static Map<String, GhostDeclaration> copyGhosts(Map<String, GhostDeclaration> ghosts) {
        return Map.copyOf(ghosts);
    }
}
