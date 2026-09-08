package liquidjava.mcp.tools.validity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import liquidjava.rj_language.ast.Var;
import liquidjava.rj_language.parsing.RefinementsParser;

public record ValidityRequest(Map<String, String> variables, List<String> assumptions, String conclusion) {
    private static final Set<String> TYPES = Set.of("boolean", "short", "char", "int", "long", "float", "double");

    public ValidityRequest {
        variables = Map.copyOf(variables);
        assumptions = List.copyOf(assumptions);
        variables.forEach((name, type) -> {
            if (!validName(name))
                throw new IllegalArgumentException("invalid variable name: " + name);
            if (!TYPES.contains(type))
                throw new IllegalArgumentException("unsupported variable type: " + type);
        });
    }

    public static ValidityRequest fromArguments(Map<String, Object> arguments) {
        Map<?, ?> declarations = (Map<?, ?>) arguments.get("variables");
        Map<String, String> variables = new LinkedHashMap<>();
        declarations.forEach((name, type) -> variables.put((String) name, (String) type));

        return new ValidityRequest(
            variables,
            ((List<?>) arguments.get("assumptions")).stream().map(String.class::cast).toList(),
            (String) arguments.get("conclusion")
        );
    }

    private static boolean validName(String name) {
        if (!name.matches("#*[a-zA-Z_][a-zA-Z0-9_#]*") || Set.of("_", "this", "old").contains(name))
            return false;
        try {
            return RefinementsParser.createAST(name, "") instanceof Var variable && variable.getName().equals(name);
        } catch (Exception e) {
            return false;
        }
    }
}
