package liquidjava.mcp.utils;

import java.util.regex.Pattern;

public final class Regex {
    public static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[0-9;]*m");
    public static final Pattern RANGE_SEPARATOR = Pattern.compile("[:-]");
    public static final Pattern TYPE = Pattern.compile("(?:[a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*");
    public static final Pattern NAME = Pattern.compile("#*[a-zA-Z_][a-zA-Z0-9_#]*");

    private Regex() {}
}
