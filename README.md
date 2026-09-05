# LiquidJava MCP

A local Java MCP server exposing LiquidJava's normal verification through the `verify` tool.
It uses `io.github.liquid-java:liquidjava-verifier:0.0.32` from Maven Central and the
official MCP Java SDK 2.0.1. A LiquidJava checkout or separately installed CLI is not required.

## Build and run

Use JDK 21 and Maven 3.6.3 or newer. Ensure `mvn -version` reports the intended JDK.

```sh
mvn verify
java -jar target/liquidjava-mcp.jar
```

The executable JAR includes its dependencies and LiquidJava's Z3 native resources.
The server reads MCP messages from stdin and writes only MCP messages to stdout.
Application logs and exception stack traces go to stderr. Closing stdin stops the server.

Example MCP client configuration (replace the JAR path with your absolute path):

```json
{
  "mcpServers": {
    "liquidjava": {
      "command": "java",
      "args": ["-jar", "/absolute/path/liquidjava-mcp/target/liquidjava-mcp.jar"]
    }
  }
}
```

Use a Java 21 executable in `command` if the client's default Java is older.
Relative verification paths are resolved from the server process's startup working
directory, which the MCP client chooses. Absolute paths avoid ambiguity.

## `verify`

Input:

```json
{"paths": ["/project/src/Main.java", "/project/src/support"]}
```

`paths` must be a nonempty array of nonblank strings with valid filesystem path syntax.
Unknown arguments are rejected. All paths are passed unchanged, in order, to **one**
LiquidJava invocation so related files can be analyzed together. LiquidJava handles
directory traversal, missing paths, duplicate paths, and overlapping inputs. Paths
beginning with `-` are treated as literal paths; shell expansion is not performed.

Result in MCP `structuredContent`:

```json
{
  "success": true,
  "output": "Running LiquidJava on: /project/src/Main.java\nCorrect! Passed Verification.\n"
}
```

The result also contains one text block with the same object serialized as JSON.
`output` preserves LiquidJava's wording, diagnostic ordering, source excerpts,
locations, and line breaks, with ANSI color codes removed. Output schemas are
advertised through `tools/list`.

| Outcome | `success` | MCP `isError` | `error` |
| --- | --- | --- | --- |
| Verification passes, including warnings alone | `true` | `false` | Absent |
| LiquidJava reports an error, including a missing path | `false` | `false` | Absent |
| Invalid tool arguments | `false` | `true` | `INVALID_INPUT` |
| Verifier throws an exception | `false` | `true` | `VERIFIER_ERROR` |

Execution errors include an `error` object with `code` and `message`. Any verification
output captured before an exception remains in `output`; stack traces stay on stderr.
Malformed MCP messages and unknown tool names use the SDK's protocol errors.

`success` means LiquidJava completed without error diagnostics. It does **not** mean
the Java sources compiled successfully: LiquidJava 0.0.32 can report compilation
issues as warnings and still print `Correct! Passed Verification.` For example, the
malformed-Java test fixture has this behavior. The server preserves that behavior
and the warning rather than redefining the verifier's result.

## Architecture

- `Main` owns stdio transport, tool registration, and shutdown. The transport retains
  the original stdout stream; ordinary application stdout is redirected to stderr
  before dependency initialization.
- `VerifyTool` loads its input/output schemas from `src/main/resources/schemas/verify.json`
  and owns input validation and MCP result mapping. It validates
  arguments itself so all tool validation failures have the same structured error
  shape; SDK automatic tool-input validation is disabled for this reason.
- `LiquidJavaVerifier` invokes `CommandLineLauncher.main`, captures stdout, and reads
  `Diagnostics.foundError()` for success. CLI exit codes and output text are not used
  to infer success. The adapter and immutable request/result records have no MCP types.

LiquidJava uses process-wide mutable options, diagnostics, and verification context.
A process-wide lock serializes invocation, output capture, and result extraction
across adapter instances. Each call resets command-line options before invoking the
launcher, disabling debug, LSP, help, and version modes and clearing previous paths.
Stdout is restored even after an exception. LiquidJava initializes diagnostics and
context for each invocation.

There is no verification cache or worker process. Running solver work cannot be
forcibly cancelled in-process; a client timeout does not guarantee verification has
stopped. Later calls wait for the current invocation to finish.

The verifier artifact embeds older SLF4J and Jackson classes. Explicit SLF4J and
Jackson-annotations dependencies precede those copies on the test classpath, and the
shade configuration removes their embedded duplicates from the executable JAR.
The MCP JSON implementation uses Jackson 3. Service descriptors and native resources
are retained during packaging.

## Tests

`mvn test` runs focused tool and adapter tests. `mvn verify` also runs `McpServerTest`
against the actual packaged JAR using the SDK's stdio client. It checks tool discovery,
a successful verification with matching structured/text results, a refinement failure,
and a structured input error. Maven runs this class after packaging, through Failsafe.

Adapter tests cover valid and invalid refinements, warnings, malformed Java, missing
paths, directories, cooperating files, Unicode/spaces, option-like paths, CLI output
parity, repeated/concurrent calls, option resets, and stdout restoration after an exception.
All fixtures are in this repository; tests do not depend on sibling checkouts.

If you have locally installed LiquidJava under the same release version, verify
against Maven Central with a fresh cache:

```sh
mvn -Dmaven.repo.local="$(mktemp -d)" verify
```

## Future work

Only `verify` is registered. Follow-up tools are `get_diagnostics`, `get_local_context`,
`get_global_context`, `get_refinement`, `get_vc`, and `check_vc`. A separate debug tool
is also deferred; `verify` intentionally accepts only paths.
