---
name: liquidjava-mcp
description: Use the LiquidJava MCP to verify Java refinements and typestates, diagnose failures, inspect contracts, look up verification context, and query LiquidJava's solver with custom assumptions and conclusions.
---

# LiquidJava MCP

## Overview

LiquidJava is an additional compile-time type checker for Java, based on refinement types and typestates. Refinements constrain values with predicates and typestates constrain object states and method call sequences. Verification checks whether the facts established by the code imply the required refinements and state conditions.

- `@Refinement("predicate")` refines a variable, field, parameter, or return type
- Predicates support comparisons, boolean operators, arithmetic operators, and conditional expressions
- `_` refers to the value being refined, such as in return refinements and shorthand variable refinements
- `@RefinementAlias("Name(type x) { predicate }")` defines a reusable predicate alias
- `@StateSet({"state1", "state2"})` declares named object states for typestate protocols, represented as uninterpreted functions
- `@StateRefinement(from="predicate", to="predicate")` describes method pre- and post-conditions for typestate transitions; predicates can refer to parameters, object states, and ghost variables
- `@Ghost("type name")` declares a ghost variable, which is also an uninterpreted function with first parameter `this`
- `old(this)` can be used in state refinements to refer to the receiver state before the method call (e.g. the predicate `size(this) == size(old(this)) + 1` can be used to specify that a method increments size by one)

## Workflow

1. Start with `verify(path)` for a quick pass/fail check, or use `get_diagnostics(path)` when structured failure details are useful.
2. Verify the smallest source file or directory that covers the relevant code.
3. Inspect only the context needed to explain a failure using the tools below.
4. Fix code or annotations to match the intended contract, then rerun verification after edits.

## Tools

| Need | Tool | When to use |
|---|---|---|
| Quick verification run | `verify(path)` | Quick checks, CLI-style output, lower token usage. |
| Detailed verification trace | `verify(path, debug=true)` | Inspect verification conditions, simplifications, and solver results to explain a failure at the cost of higher token usage. |
| Get more detailed diagnostics | `get_diagnostics(path)` | Reason about individual errors and warnings in a structured format. |
| Variables and refinements near a failure | `get_locals(path, line, column, file?)` | Inspect the verification context at a specific source position. |
| Aliases, ghosts, states | `get_globals(path, file?)` | Inspect global definitions available in the program. |
| Method or constructor refinements | `get_contracts(path, className?, signature?)` | Inspect method and constructor contracts, including parameters, return refinements, and state transitions |
| Typestate protocol | `get_state_machine(path)` | Understand the allowed transitions for a typestate protocol. |
| Check if assumptions imply a conclusion | `check_validity(variables, assumptions, conclusion)` | Test whether custom assumptions prove a specified conclusion. |
| Check if constraints are satisfiable | `check_satisfiability(variables, constraints)` | Check if a set of constraints are satisfiable or detect contradictions.

## Tool Behavior

- Verification accepts a Java source file or a directory. Directory analysis recursively verifies `.java` files.
- The `verify`, `get_diagnostics`, `get_locals`, `get_globals`, and `get_contracts` tools reuse cached analysis when the input path, source hash, and debug option match.
- Requests time out after 60 seconds, including time waiting for another analysis, and return a verifier error with any captured output.
- The runner clears its cached analysis when a source changes, a run fails, or a run is cancelled.
- The context tools run the verification before taking their snapshot.
- `get_state_machine` parses the typestate protocol into a more readable format. It does not run the verification. Run it to check whether the actual code follows the protocol.
- Local variables are derived from recorded verifier history, not a reconstructed solver state.
- `get_contracts` includes both source contracts and external refinement contracts.
- The `check_validity` and `check_satisfiability` tools query the solver directly and do not verify Java source code.
- `line` and `column` parameters are one-based source coordinates.

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
