# LiquidJava MCP

A local Java MCP server that exposes LiquidJava verification capabilities to LLM agents.

It allows agents to verify Java code, inspect diagnostics and refinement contexts, retrieve verification conditions, and query LiquidJava's solver through structured MCP tools.

## Tools

### `verify`

Runs LiquidJava verification over the provided paths and returns the same representation normally shown to developers.

**Input:** One or more file or folder paths.
**Output:** Verification status and the standard LiquidJava verification output.

### `get_diagnostics`

Runs verification and exposes diagnostics in a machine-readable format, avoiding the need for agents to parse terminal output.

**Input:** One or more file or folder paths.
**Output:** Separate `errors` and `warnings` arrays of structured diagnostics, including type, severity, location, message, refinements, hints, and counterexamples when available.

### `get_local_context`

Exposes the verification context available at a specific point in the program.

**Input:** A file path and source position.
**Output:** Variables and other local context elements visible at that position, together with their refinements.

### `get_global_context`

Allows agents to inspect LiquidJava-specific definitions available to the program.

**Input:** A file path and, optionally, a source position.
**Output:** Global aliases, ghost functions, states, and other globally available refinement definitions.

### `get_refinement`

Provides a targeted way to inspect what LiquidJava knows about a particular program element without retrieving the entire verification context.

**Input:** A file path and source position or source range.
**Output:** The declared and/or inferred refinement of the selected variable or expression.

### `get_vc`

Exposes the logical obligation responsible for a verification result or failure, allowing agents to reason directly about why verification succeeded or failed.

**Input:** A file path and source position, source range, or diagnostic identifier.
**Output:** The verification condition associated with that program location, including its assumptions, expected and inferred refinements, and optionally its simplified form.

### `check_vc`

Allows agents to test logical hypotheses, candidate specifications, preconditions, repairs, and other generated verification conditions using LiquidJava's solver.

**Input:** Custom assumptions and a conclusion, or a complete custom verification condition.
**Output:** Whether the verification condition is valid and, when invalid, a counterexample if one is available.
