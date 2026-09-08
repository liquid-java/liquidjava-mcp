# LiquidJava MCP

A Java MCP server that exposes LiquidJava verification tools to LLM agents over stdio.

It allows agents to run the LiquidJava verification, retrieve structured diagnostics, inspect the verification context, and query LiquidJava's solver with custom assumptions and conclusions.

## Installation

Build the project with `mvn package` and then point your MCP client at the resulting jar:

```json
{
  "servers": {
    "liquidjava-mcp": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/path/to/liquidjava-mcp/target/liquidjava-mcp.jar"
      ]
    }
  }
}
```

## Tools

### Overview

| Tool | Purpose | Input | Output |
|---|---|---|---|
| `verify` | Run the verification, get human-readable output (same as CLI) | `path`, `debug?` | standard LiquidJava output |
| `get_diagnostics` | Run verification, get structured/machine-readable diagnostics | `path` | `errors` and `warnings` arrays (type, severity, location, message, refinements, hints, counterexamples) |
| `get_locals` | Inspect verification context (variables in scope) at a specific source position | `path`, `file?`, `line`, `column` | `variables` (name, internal name, type, refinement, location) |
| `get_globals` | Inspect global definitions (aliases, ghosts, states) available in the program | `path`, `file?` | `aliases`, `ghosts`, `states` |
| `get_contracts` | Inspect method and constructor contracts | `path`, `className?`, `signature?` | `contracts` (qualified signature, parameters, return refinement, state transitions, location) |
| `check_validity` | Check if assumptions imply a conclusion via the solver | `variables`, `assumptions`, `conclusion` | `status` (`valid` or `invalid`), `counterexample` |
| `get_state_machine` | Parse a Java file's LiquidJava typestate protocol | `path` | `stateMachine` with states and transitions |

The `verify`, `get_diagnostics`, `get_locals`, `get_globals`, and `get_contracts` tools reuse cached analysis when the input path, source hash, and debug option match. Requests time out after 60 seconds, including time waiting for another analysis, and return a verifier error with any captured output. Cancellation requests an interrupt; if the verifier ignores it, subsequent analyses wait until it exits to protect shared state. Analyses cancelled before completion are not cached.

### `verify`

Runs the LiquidJava verification and returns the same representation normally shown to developers.
Allows agents to inspect verification conditions, their simplifications, and solver results using the `debug` flag.

**Input:** File or directory path, with optional `debug` flag.

**Output:** Verification status and the standard LiquidJava verification output.

```json
{
  "path": ".../Example.java",
  "debug": false
}
```

```json
{
  "success": false,
  "output": "Running LiquidJava on: .../Example.java\n\nRefinement Error: input >= 0 && #ret¹ == input is not a subtype of #ret¹ > 0\n..."
}
```

### `get_diagnostics`

Runs the LiquidJava verification and exposes diagnostics in a machine-readable format, avoiding the need for agents to parse terminal output.

**Input:** File or directory path.

**Output:** Verification status and `errors` and `warnings` arrays of structured diagnostics, including type, severity, location, message, refinements, hints, and counterexamples when available.

```json
{
  "path": ".../Example.java"
}
```

```json
{
  "success": false,
  "errors": [
    {
      "refinements": {
        "expected": "#ret_1 > 0",
        "found": "input >= 0 && #ret_1 == input"
      },
      "vc": {
        "simplified": "∀input:int, input >= 0 => \n∀#ret_1:int, #ret_1 == input => \n#ret_1 > 0",
        "original": "∀input:int, input >= 0 => \n∀#value_0:int, #value_0 == input => \n∀#ret_1:int, #ret_1 == #value_0 => \n#ret_1 > 0"
      },
      "type": "RefinementError",
      "severity": "error",
      "counterexample": [
        {"variable": "input", "value": "0"},
        {"variable": "#ret_1", "value": "0"}
      ],
      "declarationLocation": {
        "file": ".../Example.java",
        "startColumn": 9,
        "endLine": 14,
        "endColumn": 5,
        "startLine": 11
      },
      "location": {
        "file": ".../Example.java",
        "startColumn": 9,
        "endLine": 13,
        "endColumn": 21,
        "startLine": 13
      },
      "message": "input >= 0 && #ret¹ == input is not a subtype of #ret¹ > 0"
    }
  ],
  "warnings": []
}
```

### `get_locals`

Exposes the verification context available at a specific point in the program.

**Input:** A `path` to the Java source file or directory to verify, plus `line` and `column` identifying the source position to inspect. An optional `file` parameter specifies a different file for filtering variables. When omitted, `path` is used instead. Lines and columns are one-based.

**Output:** `variables`. Each variable includes its source name, verifier `internalName`, Java type, refinement predicate, and source location (inclusive ends). Entries include declarations and refinement instances recorded before the position in enclosing scopes. Internal names preserve relationships between predicates. Positions outside recorded scopes return an empty array. This exposes source-filtered verifier history; synthesized branch-merge instances can carry the original declaration location, so it does not reconstruct the exact solver state at the cursor.

```json
{
  "path": ".../Example.java",
  "line": 13,
  "column": 20
}
```

```json
{
  "variables": [
    {
      "name": "input",
      "internalName": "input",
      "location": {
        "file": ".../Example.java",
        "startColumn": 41,
        "endLine": 11,
        "endColumn": 45,
        "startLine": 11
      },
      "type": "int",
      "refinement": "input >= 0"
    },
    {
      "name": "value",
      "internalName": "#value_0",
      "location": {
        "file": ".../Example.java",
        "startColumn": 13,
        "endLine": 12,
        "endColumn": 26,
        "startLine": 12
      },
      "type": "int",
      "refinement": "#value_0 == input"
    },
    {
      "name": "value",
      "internalName": "value",
      "location": {
        "file": ".../Example.java",
        "startColumn": 13,
        "endLine": 12,
        "endColumn": 26,
        "startLine": 12
      },
      "type": "int",
      "refinement": "true"
    },
    {
      "name": "ret",
      "internalName": "#ret_1",
      "location": {
        "file": ".../Example.java",
        "startColumn": 9,
        "endLine": 13,
        "endColumn": 21,
        "startLine": 13
      },
      "type": "int",
      "refinement": "#ret_1 == #value_0"
    },
    {
      "name": "this#Example",
      "internalName": "this#Example",
      "location": {
        "file": ".../Example.java",
        "startColumn": 9,
        "endLine": 13,
        "endColumn": 21,
        "startLine": 13
      },
      "type": "com.example.Example",
      "refinement": "true"
    }
  ]
}
```

### `get_globals`

Provides global definitions available in the program.

**Input:** A `path` to the Java source file or directory to verify, optionally with a `file` identifying which source file's ghosts and states to return.

**Output:** `aliases`, `ghosts`, and `states`. Aliases include parameter names, parameter types, and predicates; ghosts and states include qualified names, return types, parameter types, and their defining refinements when available.

```json
{
  "path": ".../Example.java"
}
```

```json
{
  "aliases": [
    {
      "predicate": "v >= 0",
      "parameterTypes": ["int"],
      "parameters": ["v"],
      "name": "Positive"
    }
  ],
  "ghosts": [
    {
      "qualifiedName": "com.example.Example.size",
      "file": ".../Example.java",
      "name": "size",
      "parameterTypes": ["com.example.Example"],
      "returnType": "int"
    }
  ],
  "states": [
    {
      "qualifiedName": "com.example.Example.closed",
      "refinement": "state1(_) == 1",
      "file": ".../Example.java",
      "parameterTypes": ["com.example.Example"],
      "returnType": "boolean",
      "name": "closed"
    },
    {
      "qualifiedName": "com.example.Example.open",
      "refinement": "state1(_) == 0",
      "file": ".../Example.java",
      "parameterTypes": ["com.example.Example"],
      "returnType": "boolean",
      "name": "open"
    }
  ]
}
```

### `get_contracts`

Returns the method and constructor contracts registered during verification.
This includes source contracts and external refinement contracts.

**Input:** A `path` to the Java source file or directory to verify. `className` optionally filters by an exact qualified target class name. `signature` optionally filters by an exact complete qualified method or constructor signature, including parameter types.

**Output:** Each contract includes its target class, signature, return type and refinement, refined parameters, paired state transitions, and declaration location.

```json
{
  "path": ".../Example.java",
  "className": "com.example.Example",
  "signature": "com.example.Example.requirePositive(int)"
}
```

```json
{
  "contracts": [
    {
      "className": "com.example.Example",
      "signature": "com.example.Example.requirePositive(int)",
      "returnType": "void",
      "parameters": [
        {"name": "input", "type": "int", "refinement": "input > 0"}
      ],
      "returnRefinement": "true",
      "stateTransitions": [],
      "location": {
        "file": ".../Example.java",
        "startLine": 10,
        "startColumn": 5,
        "endLine": 11,
        "endColumn": 6
      }
    }
  ]
}
```

### `check_validity`

Checks whether custom assumptions imply one conclusion using LiquidJava's solver, without verifying Java files.
Does not support ghost functions, aliases, source constants, and implicit receiver/return/old-state bindings.

**Input:** `variables` (map of names to types), `assumptions` (array of boolean predicate strings), and `conclusion` (boolean predicate string).

**Output:** `status` (`valid` or `invalid`). Invalid results include a `counterexample`.

```json
{
  "variables": {"x": "int"},
  "assumptions": ["x >= 0"],
  "conclusion": "x > 0"
}
```

```json
{
  "status": "invalid",
  "counterexample": [{"variable": "x", "value": "0"}]
}
```

### `get_state_machine`

Parses a Java source file and returns the LiquidJava typestate protocol declared by its `@StateSet` and `@StateRefinement` annotations. The result describes the possible states, initial states, and method transitions. A transition's `fromCondition` and `toCondition` contain any non-state conditions such as `cond ? state1 : state2` or `cond && state1`.

**Input:** `path`, an existing Java source file. Directories are not accepted because the parser returns the state machine for one source type at a time.

**Output:** An object containing a `stateMachine` value with the qualified class name, states, initial transitions, and method transitions. `stateMachine` is `null` when the file does not declare a state machine.

```json
{
  "path": ".../File.java"
}
```

```json
{
  "stateMachine": {
    "className": "example.File",
    "states": [
      "open",
      "closed"
    ],
    "transitions": [
      {
        "from": "open",
        "to": "closed",
        "label": "close",
        "fromCondition": null,
        "toCondition": null
      },
      {
        "from": "open",
        "to": "open",
        "label": "read",
        "fromCondition": null,
        "toCondition": null
      }
    ],
    "initialTransitions": [
      {
        "to": "open",
        "toCondition": null
      }
    ]
  }
}
```
