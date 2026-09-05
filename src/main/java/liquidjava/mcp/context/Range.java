package liquidjava.mcp.context;

import spoon.reflect.cu.SourcePosition;

public record Range(int startLine, int startColumn, int endLine, int endColumn) {
    static Range from(SourcePosition position) {
        return new Range(position.getLine(), position.getColumn(), position.getEndLine(), position.getEndColumn());
    }

    static Range parse(String scope) {
        String[] parts = scope.split("[:-]");
        return new Range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    static boolean before(int line, int column, int otherLine, int otherColumn) {
        return line < otherLine || (line == otherLine && column < otherColumn);
    }

    boolean startsBefore(Range other) {
        return before(startLine, startColumn, other.startLine, other.startColumn);
    }

    boolean contains(Range other) {
        return !before(other.startLine, other.startColumn, startLine, startColumn)
                && !before(endLine, endColumn, other.endLine, other.endColumn);
    }
}
