---
name: liquidjava-mcp
description: Use the LiquidJava MCP to verify Java refinements and typestates, diagnose failures, inspect contracts and verification context, and query the solver with custom assumptions and conclusions.
---

# LiquidJava MCP

LiquidJava is an additional compile-time Java type checker based on refinement types and typestates. Refinements constrain values and typestates constrain object states and call sequences. Verification checks whether the code satisfies the established refinements and states.

## Syntax

- `@Refinement("predicate")` refines a variable, field, parameter, or return type. Predicates support comparisons, boolean/arithmetic operators, and conditional expressions; `_` means the refined value (such as in return/shorthand refinements).
- `@RefinementAlias("Name(type x) { predicate }")` defines a reusable predicate alias.
- `@StateSet({"state1", "state2"})` declares named object states for typestate protocols, represented as uninterpreted functions.
- `@StateRefinement(from="predicate", to="predicate")` specifies method pre/postconditions; predicates may refer to parameters, object states, and ghost variables.
- `@Ghost("type name")` declares a ghost variable, an uninterpreted function whose first parameter is `this`. `ghost(this)` is equivalent to `this.ghost()` and `ghost()`.
- `old(this)` refers to the receiver state before a call; e.g. `size(this) == size(old(this)) + 1` specifies a size increment.

## Workflow

1. Start with `verify(path)` for a quick pass/fail check, or use `get_diagnostics(path)` when structured failure details are useful.
2. Verify the smallest source file or directory that covers the relevant code.
3. Inspect only the context needed to explain a failure using the tools below.
4. Fix code or annotations to match the intended contract, then rerun verification after edits.

## Tools

| Tool | When to use |
|---|---|
| `verify(path)` | Quick verification run, CLI-style output, lower token usage. |
| `verify(path, debug=true)` | Detailed verification trace: verification conditions, simplifications, and solver results to explain a failure at the cost of higher token usage. |
| `get_diagnostics(path)` | Reason about individual errors and warnings in a structured format with more details. |
| `get_locals(path, line, column, file?)` | Inspect variables and their refinements at a specific source position. |
| `get_globals(path, file?)` | Inspect global definitions available in the program: aliases, ghosts, and states. |
| `get_contracts(path, className?, signature?)` | Inspect method and constructor contracts, including parameter refinements, return refinements, and state transitions |
| `get_state_machine(path)` | Understand the allowed transitions for a typestate protocol. |
| `check_validity(variables, assumptions, conclusion)` | Check whether assumptions prove a specified conclusion. |
| `check_satisfiability(variables, constraints)` | Check whether a set of constraints are satisfiable or detect contradictions.

## Behavior

- Diagnostic and context tools accept a Java file or directory. Directories recursively verify `.java` files.
- `verify`, `get_diagnostics`, `get_locals`, `get_globals`, and `get_contracts` reuse cached analysis when path, source hash, and debug option match. The runner clears the cache when source changes, a run fails, or a run is cancelled.
- Requests time out after 60 seconds, including time waiting for another analysis, and return a verifier error with captured output.
- Context tools verify before taking their snapshot.
- Context information comes from recorded verifier history, not a reconstructed solver state.
- Contracts include source and external refinement contracts.
- `get_state_machine` parses the typestate protocol into a more readable format. It does not run the verification. Run it to check whether the actual code follows the protocol.
- `check_validity` and `check_satisfiability` query the solver directly and do not verify Java source.
- `line` and `column` are one-based source coordinates.

## Instructions

- Prefer absolute source paths.
- Do not weaken requirements just to pass verification.
- If the solver returns unknown or a tool returns an error, treat the result as inconclusive and not successful verification.
- Use `check_validity` for implication questions and `check_satisfiability` for consistency/model questions.
- If an implication passes unexpectedly, check the assumptions for contradictions using `check_satisfiability` before trusting the proof.
- Preserve the intended contract when diagnosing failures.

## Resources

- [LiquidJava Website](https://liquid-java.github.io)
- [LiquidJava Documentation](https://liquid-java.github.io/liquidjava-docs)
- [LiquidJava Repository](https://github.com/liquid-java/liquidjava)
- [LiquidJava MCP Repository](https://github.com/liquid-java/liquidjava-mcp)
