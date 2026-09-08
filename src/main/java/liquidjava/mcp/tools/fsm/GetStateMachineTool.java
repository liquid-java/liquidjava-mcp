package liquidjava.mcp.tools.fsm;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import liquidjava.fsm.StateMachine;
import liquidjava.fsm.StateMachineInitialTransition;
import liquidjava.fsm.StateMachineParser;
import liquidjava.fsm.StateMachineTransition;
import liquidjava.mcp.tools.AbstractMcpTool;
import liquidjava.mcp.tools.McpErrorCode;

/**
 * Parses a Java source file for its LiquidJava state machine.
 */
public final class GetStateMachineTool extends AbstractMcpTool {
    public GetStateMachineTool(McpJsonMapper jsonMapper) {
        super("get_state_machine", """
            Parses a Java source file and returns its LiquidJava state machine, including states, initial transitions, method transitions, and guards.
            Guards are any non-state conditions in the source and target refinements of a transition, such as `cond ? state1 : state2` or `cond && state1`, represented by `fromCondition` and `toCondition`.
            Requires files to declare a typestate protocol using @StateSet and @StateRefinement annotations, otherwise returns a null `stateMachine`.
        """, jsonMapper);
    }

    @Override
    public CallToolResult call(Map<String, Object> arguments) {
        String inputError = validateInput(arguments);
        if (inputError != null) {
            return errorResult(McpErrorCode.INVALID_INPUT, inputError);
        }

        StateMachineRequest request;
        try {
            request = StateMachineRequest.fromArguments(arguments);
        } catch (IllegalArgumentException e) {
            return errorResult(McpErrorCode.INVALID_INPUT, e.getMessage());
        }

        try {
            StateMachine stateMachine = StateMachineParser.parse(
                Path.of(request.path()).toAbsolutePath().normalize().toUri().toString());

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("stateMachine", map(stateMachine));
            return result(content, false);
        } catch (Exception e) {
            return errorResult(McpErrorCode.VERIFIER_ERROR, message(e));
        }
    }

    private CallToolResult errorResult(McpErrorCode code, String message) {
        Map<String, Object> content = new LinkedHashMap<>();
        addError(content, code, message);
        content.put("stateMachine", null);
        return result(content, true);
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static Map<String, Object> map(StateMachine stateMachine) {
        if (stateMachine == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("className", stateMachine.className());
        result.put("states", stateMachine.states());
        result.put("initialTransitions", stateMachine.initialTransitions().stream()
            .map(GetStateMachineTool::map)
            .toList());
        result.put("transitions", stateMachine.transitions().stream()
            .map(GetStateMachineTool::map)
            .toList());
        return result;
    }

    private static Map<String, Object> map(StateMachineInitialTransition transition) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("to", transition.to());
        result.put("toCondition", transition.toCondition());
        return result;
    }

    private static Map<String, Object> map(StateMachineTransition transition) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", transition.from());
        result.put("to", transition.to());
        result.put("label", transition.label());
        result.put("fromCondition", transition.fromCondition());
        result.put("toCondition", transition.toCondition());
        return result;
    }
}
