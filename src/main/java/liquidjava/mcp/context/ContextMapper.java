package liquidjava.mcp.context;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.mcp.utils.Utils;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.ContextHistory;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.rj_language.ast.formatter.VariableFormatter;
import spoon.reflect.cu.SourcePosition;

final class ContextMapper {
    private ContextMapper() {}

    static Map<String, Object> locals(ContextRequest request) {
        ContextHistory history = ContextHistory.getInstance();
        List<Range> scopes = history.getFileScopes().entrySet().stream()
                .filter(e -> sameFile(e.getKey(), request.file()))
                .flatMap(e -> e.getValue().stream()).map(Range::parse).toList();
        Range cursor = new Range(request.line(), request.column(), request.line(), request.column());
        var variables = history.getLocalVars().stream()
                .filter(v -> visible(v, request.file(), cursor, scopes))
                .sorted(Comparator.comparingInt((RefinedVariable v) -> v.getPlacementInCode().getPosition().getSourceStart())
                        .thenComparing(RefinedVariable::getName))
                .map(ContextMapper::variable).distinct().toList();
        return Map.of("variables", variables);
    }

    static Map<String, Object> globals() {
        Context context = Context.getInstance();
        var aliases = context.getAliases().stream().map(alias -> Map.<String, Object>of(
                "name", alias.getName(), "parameters", List.copyOf(alias.getVarNames()),
                "parameterTypes", alias.getTypes().stream().map(Object::toString).toList(),
                "predicate", alias.getClonedPredicate().toString()))
                .distinct().sorted(Comparator.comparing(a -> a.get("name").toString())).toList();
        var ghosts = context.getGhostStates().stream().sorted(Comparator.comparing(GhostState::getQualifiedName)).toList();
        var stateFunctions = ghosts.stream().filter(g -> g.getParent() != null)
                .map(g -> g.getParent().getQualifiedName()).collect(Collectors.toSet());
        var functions = Stream.concat(ghosts.stream().filter(g -> g.getParent() == null),
                context.getGhosts().stream().filter(g -> !stateFunctions.contains(g.getQualifiedName())))
                .sorted(Comparator.comparing(GhostFunction::getQualifiedName))
                .map(ContextMapper::ghost).distinct().toList();
        return Map.of("aliases", aliases,
                "ghosts", functions,
                "states", ghosts.stream().filter(g -> g.getParent() != null).map(ContextMapper::ghost).toList());
    }

    private static boolean visible(RefinedVariable variable, String file, Range cursor, List<Range> scopes) {
        if (variable.getPlacementInCode() == null) return false;
        SourcePosition position = variable.getPlacementInCode().getPosition();
        if (position == null || !position.isValidPosition() || position.getFile() == null
                || !sameFile(position.getFile().toString(), file)) return false;
        Range range = new Range(position.getLine(), position.getColumn(), position.getEndLine(), position.getEndColumn());
        return Range.before(range.startLine, range.startColumn, cursor.startLine, cursor.startColumn)
                && scopes.stream().anyMatch(s -> s.contains(range) && s.contains(cursor))
                && scopes.stream().noneMatch(s -> s.contains(range) && !s.contains(cursor));
    }

    private static Map<String, Object> variable(RefinedVariable variable) {
        return Map.of("name", VariableFormatter.withoutInstance(variable.getName()),
                "internalName", variable.getName(), "type", variable.getType().toString(),
                "refinement", variable.getMainRefinement().toString(),
                "location", Utils.mapPosition(variable.getPlacementInCode().getPosition()));
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

    private static boolean sameFile(String left, String right) {
        return Path.of(left).toAbsolutePath().normalize().equals(Path.of(right).toAbsolutePath().normalize());
    }

    private record Range(int startLine, int startColumn, int endLine, int endColumn) {
        static Range parse(String scope) {
            String[] parts = scope.split("[:-]");
            return new Range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }

        static boolean before(int line, int column, int otherLine, int otherColumn) {
            return line < otherLine || (line == otherLine && column < otherColumn);
        }

        boolean contains(Range other) {
            return !before(other.startLine, other.startColumn, startLine, startColumn)
                    && !before(endLine, endColumn, other.endLine, other.endColumn);
        }
    }
}
