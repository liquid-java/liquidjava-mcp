# LiquidJava MCP

A local Java MCP server that exposes LiquidJava verification capabilities to LLM agents.

It allows agents to verify Java code, inspect diagnostics and refinement contexts, retrieve verification conditions, and query LiquidJava's solver through structured MCP tools.

## Tools

### `verify`

Runs LiquidJava verification over the provided paths and returns the same representation normally shown to developers.

**Input:** One or more file or folder paths, with optional `debug` flag.
**Output:** Verification status and the standard LiquidJava verification output.

### `get_diagnostics`

Runs verification and exposes diagnostics in a machine-readable format, avoiding the need for agents to parse terminal output.

**Input:** One or more file or folder paths.
**Output:** Separate `errors` and `warnings` arrays of structured diagnostics, including type, severity, location, message, refinements, hints, and counterexamples when available.

### `get_locals`

Exposes the verification context available at a specific point in the program.

**Input:** A `path` to the Java source file or directory to verify, plus a `file`, `line`, and `column` identifying the source position to inspect. Lines and columns are one-based.
**Output:** `success` and `variables`. Each variable includes its source name, verifier `internalName`, Java type, refinement predicate, and source location (inclusive ends). Entries include declarations and refinement instances recorded before the position in enclosing scopes. Internal names preserve relationships between predicates. Positions outside recorded scopes return an empty array. This exposes source-filtered verifier history; synthesized branch-merge instances can carry the original declaration location, so it does not reconstruct the exact solver state at the cursor.

### `get_globals`

Allows agents to inspect global definitions available in the program.

**Input:** A `path` to the Java source file or directory to verify, optionally with a `file` identifying which source file's ghosts and states to return.
**Output:** `success`, `aliases`, `ghosts`, and `states`. Aliases include parameter names, parameter types, and predicates; ghosts and states include qualified names, return types, parameter types, and their defining refinements when available.

### `get_vc`

Exposes the logical obligation responsible for a verification result or failure, allowing agents to reason directly about why verification succeeded or failed.

**Input:** A file path and source position, source range, or diagnostic identifier.
**Output:** The verification condition associated with that program location, including its assumptions, expected and inferred refinements, and optionally its simplified form.

### `check_vc`

Allows agents to test logical hypotheses, candidate specifications, preconditions, repairs, and other generated verification conditions using LiquidJava's solver.

**Input:** Custom assumptions and a conclusion, or a complete custom verification condition.
**Output:** Whether the verification condition is valid and, when invalid, a counterexample if one is available.
