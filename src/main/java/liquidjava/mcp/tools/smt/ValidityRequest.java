package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;

public record ValidityRequest(
    Map<String, String> variables,
    Map<String, GhostDeclaration> ghosts,
    List<String> assumptions,
    String conclusion
) {
    public ValidityRequest {
        variables = Map.copyOf(variables);
        ghosts = Map.copyOf(ghosts);
        assumptions = List.copyOf(assumptions);
        SmtRequest.validateVariables(variables);
        SmtRequest.validateGhosts(ghosts);
    }

    public static ValidityRequest fromArguments(Map<String, Object> arguments) {
        return new ValidityRequest(
            SmtRequest.variables(arguments),
            SmtRequest.ghosts(arguments),
            ((List<?>) arguments.get("assumptions")).stream().map(String.class::cast).toList(),
            (String) arguments.get("conclusion")
        );
    }
}
