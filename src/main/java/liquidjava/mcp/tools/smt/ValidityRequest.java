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
        variables = SmtRequest.copyVariables(variables);
        ghosts = SmtRequest.copyGhosts(ghosts);
        assumptions = List.copyOf(assumptions);
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
