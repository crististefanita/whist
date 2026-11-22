# Reporting - evidence and auditable reports

## Overview

Reporting publishes the caller's data, comparisons, and verdict to Extent/fallback, without Core or Kit.

| Need | API |
|---|---|
| Individual/class/suite run | `AssertionAuditRun` |
| Invocation | `run.open(Identity)` → `AssertionAuditReport` |
| Ports / fallback | `AtfReportPorts` / `HtmlReportSession` |
| Observed HTTP | `request.execution(ReportExecutionEvidence)`, with `ReportResponse` |
| Screenshots | `UiExtentReportSession`; full PNG |

## Minimal example

Individual run: `try (var run = open(output)) { publish(run, actual); }`. A suite keeps the run open and opens one scenario per invocation:

```java
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import java.nio.file.Path;
import java.util.List;

public static AssertionAuditRun open(Path output) {
    var settings = ReportingSettings.defaults();
    var redactor = new SensitiveDataRedactor();
    return new AssertionAuditRun(new ExtentReportSession(output, settings, redactor), redactor, settings);
}

public static void publish(AssertionAuditRun run, String actual) {
    boolean passed = "ready".equals(actual);
    var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
    var identity = new AssertionAuditReport.Identity("HealthContract", "example", "state is ready", "state", List.of("component"));
    try (var report = run.open(identity)) {
        report.phase("TEST BODY", List.of(new AtfAssertion("State", "ready", String.valueOf(actual), passed)), status, "");
        report.complete(status);
    }
    if (!passed) throw new AssertionError("State differs; inspect the report");
}
```

## Purpose and integration

| Owner | Closing and boundary |
|---|---|
| `AssertionAuditRun` | Owns the session; closing it publishes the final HTML. |
| `run.open(identity)` | Borrows the session; closing it closes the invocation's scenario. |
| `AssertionAuditReport(session, ...)` constructor | Owns and closes its own session. |

- Use the same settings/redactor in the run and renderer; the default preview is **240**, read through `assertionMaxDisplayCharacters()`. Full values precede the preview.
- The example follows the session thread; `AssertionAuditRun.extent(...)` serializes publication for concurrent callers.
- Kit collects the JUnit comparison, evaluated once; Reporting publishes it. The caller preserves the verdict, cause, and timing through `phase(..., timing)`.
- Fails indexes the terminal cause on the failed ACTION or OBSERVE row. The Tests view also retains the collapsible diagnostic, while independent comparisons remain distinct.
- The PNG retains the full viewport/display; capture and zoom belong to the adapter. Identical screenshots from the same step retain one piece of evidence and the first caption.
- After close, `ReportArtifactInspector.inspect(path, profile)` qualifies the inventory and embedded resources; diagnostics identify external dependencies. Text paths remain valid. The inspector is static; rendering and interactions are verified separately in a browser.
- The browser belongs to the caller: separate JUnit discovery diagnostics from execution, close the context before the browser, and preserve the original cause during cleanup.

## Verification of use

| Input to the example method | Observable result |
|---|---|
| `actual` is `ready`. | The method returns normally, and the HTML file exists after the session closes. |
| `actual` differs or is `null`. | The report retains the failed comparison, then the method throws `AssertionError`. |
| Compile the example source against another JAR. | Constructor or method incompatibilities are detected before publication. |

The example verifies API usage and verdict propagation. HTML autonomy, interactive expansion and native screenshots require their own acceptance; file existence alone does not prove them.

HTTP duration comes from `ReportResponse.duration()`; `detail(...)` preserves the caller's text without turning it into a transport measurement.

---

The Reporting pair is an external binary pinned in the POM and provenance register. This guide verifies the consumed API and application report result; identify an observed limitation by version, hash and a reproducible example. [Local procedure](README.md#maintenance-and-feedback)

<a id="shared-library-source"></a>

> **Library source:** [Reporting · ATF J0](https://github.com/crististefanita/atf_j0/blob/HEAD/lib/README.Reporting.md).
> This project’s POM, provenance and tests define the consumed version and local acceptance.
