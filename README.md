# LiquidJava MCP

A local Java MCP server that exposes LiquidJava verification capabilities to LLM agents.

It allows agents to verify Java code, inspect diagnostics and refinement contexts, and query LiquidJava's solver through structured MCP tools.

## Tools

### `verify`

Runs LiquidJava verification over the provided paths and returns the same representation normally shown to developers.
Allows agents to inspect verification conditions, their simplifications, and solver results using the `debug` flag.

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

### `check_validity`

Checks whether custom assumptions imply one conclusion using LiquidJava's solver, without verifying Java files.
Ghost functions, aliases, source constants, and implicit receiver/return/old-state bindings are not supported.

**Input:** `variables` (map of names to types), `assumptions` (array of boolean predicate strings), and `conclusion` (boolean predicate string).

```json
{
  "variables": {"x": "int"},
  "assumptions": ["x > 0"],
  "conclusion": "x >= 0"
}
```

**Output:** `success: true` with `status` equal to `valid`, `invalid`, or `unknown`. Invalid results include a `counterexample` array (possibly empty); unknown results include the solver's `reason`. Invalid claims are normal tool results. Input and execution failures return `success: false` with an `error` containing `code` (`INVALID_INPUT` or `VERIFIER_ERROR`) and `message`, and set MCP `isError: true`.

With assumptions `["x >= 0"]` and conclusion `"x > 0"`, it returns:

```json
{
  "success": true,
  "status": "invalid",
  "counterexample": [{"variable": "x", "value": "0"}]
}
```
