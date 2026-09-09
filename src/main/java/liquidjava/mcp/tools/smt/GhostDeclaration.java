package liquidjava.mcp.tools.smt;

import java.util.List;
import java.util.Map;

public record GhostDeclaration(List<String> parameterTypes, String returnType) {
    public GhostDeclaration {
        parameterTypes = List.copyOf(parameterTypes);
        parameterTypes.forEach(SmtRequest::validateType);
        SmtRequest.validateType(returnType);
    }

    static GhostDeclaration fromArguments(Map<?, ?> arguments) {
        return new GhostDeclaration(
            ((List<?>) arguments.get("parameterTypes")).stream().map(String.class::cast).toList(),
            (String) arguments.get("returnType")
        );
    }
}
