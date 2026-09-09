package liquidjava.mcp.tools.context;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import liquidjava.mcp.utils.PathUtils;
import liquidjava.mcp.utils.Utils;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.ContextHistory;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.context.Variable;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import spoon.reflect.cu.SourcePosition;

final class ContextMapper {
    private ContextMapper() {}

    static Map<String, Object> locals(ContextRequest request) {
        ContextHistory history = ContextHistory.getInstance();
        List<Range> scopes = history.getFileScopes().entrySet().stream()
            .filter(entry -> PathUtils.samePath(entry.getKey(), request.file()))
            .flatMap(entry -> entry.getValue().stream())
            .map(Range::parse)
            .toList();
        Range cursor = new Range(request.line(), request.column(), request.line(), request.column());
        List<Map<String, Object>> variables = history.getLocalVars().stream()
            .filter(variable -> visible(variable, request.file(), cursor, scopes))
            .sorted(Comparator.comparingInt((RefinedVariable variable) -> variable.getPlacementInCode().getPosition().getSourceStart())
            .thenComparing(RefinedVariable::getName))
            .map(ContextMapper::variable)
            .distinct()
            .toList();
        return Map.of("variables", variables);
    }

    static Map<String, Object> globals(ContextRequest request) {
        Context context = Context.getInstance();
        return Map.of(
            "aliases", aliases(context),
            "ghosts", ghosts(context, request.file()),
            "states", states(context, request.file())
        );
    }

    static Map<String, Object> contracts(ContractRequest request) {
        Context context = Context.getInstance();
        List<Map<String, Object>> contracts = context.getCtxFunctions().stream()
            .filter(function -> request.className() == null || request.className().equals(function.getTargetClass()))
            .filter(function -> request.signature() == null || request.signature().equals(function.getSignature()))
            .sorted(Comparator
                .comparing(RefinedFunction::getSignature, Comparator.nullsFirst(String::compareTo))
                .thenComparing(ContextMapper::contractLocation)
            )
            .map(ContextMapper::contract)
            .toList();
        return Map.of("contracts", contracts);
    }

    private static List<Map<String, Object>> aliases(Context context) {
        return context.getAliases().stream()
            .sorted(Comparator.comparing(AliasWrapper::getName))
            .map(ContextMapper::alias)
            .distinct()
            .toList();
    }

    private static List<Map<String, Object>> ghosts(Context context, String file) {
        return context.getGhostStates().stream()
            .filter(state -> state.getParent() == null)
            .filter(state -> file == null || state.getFile() != null && PathUtils.samePath(state.getFile(), file))
            .sorted(Comparator.comparing(GhostFunction::getQualifiedName))
            .map(ContextMapper::ghost)
            .distinct()
            .toList();
    }

    private static List<Map<String, Object>> states(Context context, String file) {
        return context.getGhostStates().stream()
            .filter(state -> state.getParent() != null)
            .filter(state -> file == null || state.getFile() != null && PathUtils.samePath(state.getFile(), file))
            .sorted(Comparator.comparing(GhostState::getQualifiedName))
            .map(ContextMapper::ghost)
            .toList();
    }

    private static boolean visible(RefinedVariable variable, String file, Range cursor, List<Range> scopes) {
        if (variable.getPlacementInCode() == null) return false;
        SourcePosition position = variable.getPlacementInCode().getPosition();
        if (position == null || !position.isValidPosition() || position.getFile() == null) return false;
        if (!PathUtils.samePath(position.getFile().toString(), file)) return false;

        Range declaration = Range.from(position);
        if (!declaration.startsBefore(cursor)) return false;

        boolean hasEnclosingScope = false;
        for (Range scope : scopes) {
            if (!scope.contains(declaration)) continue;
            if (!scope.contains(cursor)) return false;
            hasEnclosingScope = true;
        }
        return hasEnclosingScope;
    }

    private static Map<String, Object> alias(AliasWrapper alias) {
        return Map.of(
            "name", alias.getName(),
            "parameters", List.copyOf(alias.getVarNames()),
            "parameterTypes", alias.getTypes().stream().map(Object::toString).toList(),
            "predicate", alias.getClonedPredicate().toString()
        );
    }

    private static Map<String, Object> variable(RefinedVariable variable) {
        return Map.of(
            "name", VariableFormatter.withoutInstance(variable.getName()),
            "internalName", variable.getName(),
            "type", variable.getType().toString(),
            "refinement", variable.getMainRefinement().toString(),
            "location", Utils.mapPosition(variable.getPlacementInCode().getPosition())
        );
    }

    private static Map<String, Object> ghost(GhostFunction ghost) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", ghost.getName());
        result.put("qualifiedName", ghost.getQualifiedName());
        result.put("returnType", ghost.getReturnType().toString());
        result.put("parameterTypes", ghost.getParametersTypes().stream().map(Object::toString).toList());
        if (ghost instanceof GhostState state) {
            if (state.getRefinement() != null) result.put("refinement", state.getRefinement().toString());
            if (state.getFile() != null) result.put("file", state.getFile());
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> contract(RefinedFunction function) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("className", function.getTargetClass());
        result.put("signature", function.getSignature());
        result.put("returnType", function.getType().toString());
        result.put("parameters", function.getArguments().stream().map(ContextMapper::contractParameter).toList());
        result.put("returnRefinement", function.getRefReturn().toString());
        result.put("stateTransitions", function.getAllStates().stream().map(ContextMapper::stateTransition).toList());
        result.put("location", function.getPlacementInCode() == null ? null : Utils.mapPosition(function.getPlacementInCode().getPosition()));
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Object> contractParameter(Variable parameter) {
        return Map.of(
            "name", parameter.getName(),
            "type", parameter.getType().toString(),
            "refinement", parameter.getMainRefinement().toString()
        );
    }

    private static Map<String, Object> stateTransition(liquidjava.processor.context.ObjectState state) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", state.hasFrom() ? state.getFrom().toString() : null);
        result.put("to", state.hasTo() ? state.getTo().toString() : null);
        return Collections.unmodifiableMap(result);
    }

    private static String contractLocation(RefinedFunction function) {
        if (function.getPlacementInCode() == null || function.getPlacementInCode().getPosition() == null)
            return "";
        return function.getPlacementInCode().getPosition().toString();
    }

}
