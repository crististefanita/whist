# Console - diagnostic for execution

## Overview

`atf-console` projects ATF events to stdout/stderr: lifecycle events, durations, and configurable details. It is useful for monitoring a Maven or IntelliJ run; the HTML artifact is produced by [Reporting](README.Reporting.md).

## Minimal example

Snippet inside a method; the policy is explicit, and the stream remains in the adapter:

```java
import atf.console.AtfConsoleReporter;
import atf.console.ConsoleSettings;

var console = AtfConsoleReporter.configuredStdout(
        new ConsoleSettings(true, false));
console.info("Catalogul de scenarii a fost încărcat");
```

`configuredStdout()` reads `atf.report.config` → `ATF_REPORT_CONFIG` → `atf.yaml/yml`; the options are:

```yaml
reporting:
  console:
    enabled: true
    detailsEnabled: false
```

## Integration and options

| Intent | Choice |
|---|---|
| Message independent of details | `info(...)` |
| Typed events with lifecycle and duration | `publish(AtfConsoleEvent)` through the Core `AtfConsoleSink` port |
| Console fully disabled | `enabled: false` or `ConsoleSettings.disabled()` |
| Final report path in the console | Constructor with `reportPathEnabled: true` and the `REPORT_PUBLISHED` event |
| Per-class JUnit policy | JUnit Console adapter and Test Kit annotations; declare Kit explicitly |

The path requires opt-in, an enabled console, and an event with a confirmed artifact; `detailsEnabled` controls details only.

Console depends on Core; Kit is optional for JUnit integration. The projection is diagnostic: the comparison and verdict belong to the test.

## Verification of use

| Action | Observable result |
|---|---|
| Run the example with its explicit policy. | The supplied message appears in stdout, including when details are disabled. |
| Disable the console through `ConsoleSettings.disabled()`. | A new `info(...)` call produces no message in the stream. |
| Enable only report-path display. | The path appears only after a confirmed publication event; the option does not create a report. |

Compiling the snippet verifies the API signature on the selected JAR. Inspecting the stream verifies message projection; the console changes neither the test result nor application rules.

---

The Console pair is an external binary pinned in the POM and provenance register. This guide verifies the output contract consumed by this project; attach the version, hash and reproducer to [local feedback](../docs/feedback/README.md).

<a id="shared-library-source"></a>

> **Library source:** [Console · ATF J0](https://github.com/crististefanita/atf_j0/blob/HEAD/lib/README.Console.md).
> This project’s POM, provenance and tests define the consumed version and local acceptance.
