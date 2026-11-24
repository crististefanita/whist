package atf.android.component;

import atf.android.evidence.component.AndroidComponentReports;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.lifecycle.ScopedAcceptanceRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static atf.testkit.assertions.AuditedAssertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Consumer acceptance of real Jupiter capture and both public renderers; fixtures are deliberate failure controls. */
@AtfComponentTest
class AndroidComponentReportContractTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @TempDir Path temporary;

    @Test void bothRenderersRetainOutcomesInvocationsAndOriginalComparisons(ComponentInputs inputs) throws Exception {
        inputs.add("Producer controls", "Real Jupiter PASS, assertion failure, execution ERROR, SKIP, two parameter invocations and a body without audited assertions; Extent and fallback.");
        for (String renderer : List.of("extent", "fallback")) qualifyRenderer(renderer);
    }

    private void qualifyRenderer(String renderer) throws Exception {
        Path output = Files.createDirectories(temporary.resolve(renderer));
        var result = ScopedAcceptanceRun.execute(AndroidComponentReports.DIRECTORY, output.toString(), output,
                () -> {
                    try {
                        return ScopedAcceptanceRun.execute(AndroidComponentReports.RENDERER, renderer, output, () -> {
                            var listener = new SummaryGeneratingListener();
                            var request = LauncherDiscoveryRequestBuilder.request()
                                    .selectors(selectClass(Passing.class), selectClass(Failing.class), selectClass(Empty.class), selectClass(Aborted.class), selectClass(Errored.class), selectClass(Parameters.class))
                                    .configurationParameter("atf.test.run.scope", "suite")
                                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
                            try (var session = LauncherFactory.openSession()) { session.getLauncher().execute(request, listener); }
                            return listener.getSummary();
                        }, path -> path.toString().endsWith(".html")).outcome();
                    } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }, path -> path.toString().endsWith(".html"));
        var summary = result.outcome();
        assertEquals(7, summary.getTestsFoundCount());
        assertEquals(3, summary.getTestsSucceededCount());
        assertEquals(3, summary.getTestsFailedCount());
        assertEquals(1, summary.getTestsAbortedCount());
        assertTrue(summary.getFailures().stream().anyMatch(row -> row.getException() == Failing.observed.get()));
        assertTrue(summary.getFailures().stream().anyMatch(row -> row.getException() == Errored.observed.get()), "Execution ERROR keeps its original throwable instance");
        assertEquals(1, result.artifacts().size(), "One owner closes one report for seven invocations");
        String html = Files.readString(result.artifacts().getFirst());
        assertTrue(html.contains("Original controlled ERROR"), "Execution error is retained independently of assertion failure");
        assertTrue(html.contains("PARAMETER_FIRST") && html.contains("PARAMETER_LAST"), "Each parameter retains its original body");
        assertTrue(html.contains("parameterValue [1]") && html.contains("parameterValue [2]"), "Parameterized identities remain independent");
        assertTrue(html.contains("Component assertions"));
        assertTrue(html.contains("TEST BODY"));
        assertTrue(html.contains("Independent contract value"));
        assertTrue(html.contains("expected-value") && html.contains("actual-value"));
        assertTrue(html.contains("Audited assertion coverage"));
        assertTrue(html.contains("controlled assumption"));
        var inputParts = org.jsoup.Jsoup.parse(html).select("pre").stream().filter(node -> node.parents().stream().anyMatch(owner ->
                owner.tagName().equals("details") && owner.children().stream().filter(child -> child.tagName().equals("summary"))
                        .anyMatch(summaryNode -> summaryNode.text().equals("Component input: Full controlled input"))))
                .map(org.jsoup.nodes.Element::wholeText).toList();
        assertEquals(Passing.INPUT, String.join("", inputParts), "Complete ordered fixture survives both body and assertion preview budgets");
        var completeValues = org.jsoup.Jsoup.parse(html).select("details").stream()
                .filter(node -> node.children().stream().anyMatch(child -> child.tagName().equals("summary")
                        && child.text().startsWith("Component input: Full comparison value —")))
                .map(node -> node.select("pre").stream().map(part -> part.wholeText()).collect(java.util.stream.Collectors.joining())).toList();
        assertEquals(List.of(Passing.LONG_VALUE, Passing.LONG_VALUE), completeValues,
                "Expected and actual preserve their complete ordered parts beside bounded previews");
        assertTrue(org.jsoup.Jsoup.parse(html).text().contains("Full comparison value — EXPECTED")
                && org.jsoup.Jsoup.parse(html).text().contains("Full comparison value — ACTUAL"));
        assertFalse(org.jsoup.Jsoup.parse(html).select("script").stream()
                .anyMatch(script -> script.data().trim().equals("window.COMPONENT_RAW_INPUT_CANARY = 1;")), "Fixture markup stays text");
        assertTrue(summary.getContainersFailedCount() == 0, () -> {
            var detail = new java.io.StringWriter();
            for (var failure : summary.getFailures()) failure.getException().printStackTrace(new java.io.PrintWriter(detail));
            return "Publication must close successfully\n" + detail;
        });
    }

    static class Passing {
        static final String LONG_VALUE = "Complete compared value: " + "x".repeat(300);
        static final String INPUT = "<fixture>" + "<node text=\"controlled-value\"/>".repeat(1500)
                + "<script>window.COMPONENT_RAW_INPUT_CANARY = 1;</script></fixture>";
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @Test void observedValue(ComponentInputs inputs) {
            inputs.add("Full controlled input", INPUT);
            atf.testkit.assertions.AuditedAssertions.assertEquals(7, 7, "Independent contract value");
            atf.testkit.assertions.AuditedAssertions.assertEquals(LONG_VALUE, LONG_VALUE, "Compared content survives preview truncation");
        }
    }
    static class Failing {
        static final AtomicReference<Throwable> observed = new AtomicReference<>();
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @Test void originalFailure() {
            try { atf.testkit.assertions.AuditedAssertions.assertEquals("expected-value", "actual-value", "Original mismatch"); }
            catch (AssertionError failure) { observed.set(failure); throw failure; }
        }
    }
    static class Empty {
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @Test void unauditedBody() { org.junit.jupiter.api.Assertions.assertEquals(7, 7); }
    }
    static class Errored {
        static final AtomicReference<Throwable> observed = new AtomicReference<>();
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @Test void originalError() {
            var failure = new IllegalStateException("Original controlled ERROR"); observed.set(failure); throw failure;
        }
    }
    static class Parameters {
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @ParameterizedTest @ValueSource(strings = {"PARAMETER_FIRST", "PARAMETER_LAST"})
        void parameterValue(String value, ComponentInputs inputs) {
            inputs.add("Original parameter", value); assertEquals(value, value, "Parameterized original comparison");
        }
    }
    static class Aborted {
        @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
        @Test void originalAbort() { org.junit.jupiter.api.Assumptions.assumeTrue(false, "controlled assumption"); }
    }
}
