package liquidjava.mcp.validity;

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
    }

    public static ValidityRequest fromArguments(Map<String, Object> arguments) {
        if (arguments == null || !arguments.keySet().equals(Set.of("variables", "assumptions", "conclusion")))
            throw new IllegalArgumentException("expected exactly variables, assumptions, and conclusion");

        if (!(arguments.get("variables") instanceof Map<?, ?> declarations))
            throw new IllegalArgumentException("variables must map names to types");

        Map<String, String> variables = new LinkedHashMap<>();
        declarations.forEach((name, type) -> {
            if (!(name instanceof String nameText) || !validName(nameText))
                throw new IllegalArgumentException("invalid variable name: " + name);
            if (!(type instanceof String typeText) || !TYPES.contains(typeText))
                throw new IllegalArgumentException("unsupported variable type: " + type);
            variables.put((String) name, (String) type);
        });
        if (!(arguments.get("assumptions") instanceof List<?> assumptions))
            throw new IllegalArgumentException("assumptions must be an array of predicates");

        return new ValidityRequest(
            variables,
            assumptions.stream().map(value -> predicate(value, "assumption")).toList(),
            predicate(arguments.get("conclusion"), "conclusion")
        );
    }

    private static String predicate(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank())
            throw new IllegalArgumentException(field + " must be a nonempty predicate string");
        return text;
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
