package atf.android.e2e.support;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.testkit.AtfComponentTest;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class AndroidUiFlowTraceTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @ParameterizedTest
    @ValueSource(strings = {"exception", "checkpoint", "assertion", "action", "pending"})
    void successfulFinishCannotEraseRecordedFailure(String source, @TempDir Path artifacts) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "failed-trace", "Failure precedence",
                "AndroidUiFlowTraceTest", "atf.android.e2e.support", "ro.whistlab.app", List.of("component"));
        if (source.equals("exception")) trace.fail(new IllegalStateException("Original diagnostic"));
        var failedAction = new AtfUiActionRecord(1, "Tap", "Place bid", "accepted", "rejected", 5, AtfReportStatus.FAIL);
        if (source.equals("pending")) trace.recordAction(failedAction);
        else trace.checkpoint("TEST BODY", "THEN", "Keep the observed verdict", "Bidding table", null,
                source.equals("action") ? List.of(failedAction) : List.of(),
                List.of(new AtfAssertion("Bid remains pending", "pending", "observed", !source.equals("assertion"))),
                source.equals("checkpoint") ? AtfReportStatus.FAIL : AtfReportStatus.PASS, List.of());

        trace.finish(AtfReportStatus.PASS);

        assertEquals(AtfReportStatus.FAIL, trace.status());
        if (source.equals("exception")) assertTrue(trace.failure().contains("Original diagnostic"));
        Properties manifest = new Properties();
        try (var input = Files.newInputStream(trace.manifest())) { manifest.load(input); }
        assertEquals("FAIL", manifest.getProperty("flow.status"));
    }

    @Test
    void assertionsKeepCheckpointOrderContextAndVerdicts(@TempDir Path artifacts) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "trace-contract",
                "Trace assertion contract", "AndroidUiFlowTraceTest", "atf.android.e2e.support",
                "ro.whistlab.app", List.of("android", "component"));
        trace.checkpoint("SETUP", "GIVEN", "Open the bidding screen", "New game", null,
                List.of(), List.of(new AtfAssertion("Game setup is visible", "New game", "New game", true)),
                AtfReportStatus.PASS, List.of());
        trace.checkpoint("TEST BODY", "THEN", "Keep the selected bid", "Bidding table", null,
                List.of(), List.of(new AtfAssertion("Selection remains pending", "Bid not placed", "Bid placed", false)),
                AtfReportStatus.FAIL, List.of());
        trace.finish(AtfReportStatus.FAIL);

        List<AtfAssertion> assertions = trace.assertions();
        assertEquals(2, assertions.size());
        assertEquals("SETUP · GIVEN · Open the bidding screen · Game setup is visible",
                assertions.get(0).name());
        assertTrue(assertions.get(0).passed());
        assertEquals(AtfReportStatus.PASS, assertions.get(0).resultStatus());
        assertEquals("TEST BODY · THEN · Keep the selected bid · Selection remains pending",
                assertions.get(1).name());
        assertFalse(assertions.get(1).passed());
        assertEquals("Bid placed", assertions.get(1).actual());
        assertEquals(AtfReportStatus.FAIL, assertions.get(1).resultStatus());

        Properties manifest = new Properties();
        try (var input = Files.newInputStream(trace.manifest())) { manifest.load(input); }
        assertEquals("FAIL", manifest.getProperty("flow.status"));
        assertEquals("false", manifest.getProperty("phase.1.step.0.assertion.0.passed"));
        assertEquals("Bid placed", manifest.getProperty("phase.1.step.0.assertion.0.actual"));
    }

    @Test
    void overlongAssertionIsRejectedBeforeOwnershipAndJournalChange(@TempDir Path artifacts,
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        for (String field : List.of("expected", "actual")) {
            var settings = new atf.reporting.configuration.ReportingSettings(5);
            var trace = new AndroidUiFlowTrace(artifacts.resolve(field), "early-policy", "Early checkpoint policy",
                    "AndroidUiFlowTraceTest", "atf.android.e2e.support", "ro.whistlab.app",
                    List.of("component"), System::nanoTime, settings);
            trace.recordAction(new AtfUiActionRecord(1, "Tap", "Start game", "accepted", "accepted", 1, AtfReportStatus.PASS));
            byte[] before = Files.readAllBytes(trace.manifest());
            var pending = trace.pendingActions();
            String boundary = "😀😀A", overlong = boundary + "B";
            var assertion = new AtfAssertion("Full diagnostic remains owned", field.equals("expected") ? overlong : boundary,
                    field.equals("actual") ? overlong : boundary, false);
            inputs.add("Controlled public Reporting policy", settings.toString());
            inputs.add("Original Unicode comparison", assertion.toString());

            var failure = assertThrows(IllegalStateException.class, () -> trace.checkpoint("BODY", "THEN",
                    "Reject preview overflow", "Table", null, List.of(), List.of(assertion), AtfReportStatus.FAIL, List.of()));

            assertTrue(failure.getMessage().contains("Assertion " + field + " value exceeds"));
            assertTrue(failure.getMessage().contains("configured 5 character preview"));
            assertTrue(failure.getMessage().contains("observed 6 UTF-16 units"));
            assertEquals(java.util.HexFormat.of().formatHex(before),
                    java.util.HexFormat.of().formatHex(Files.readAllBytes(trace.manifest())));
            assertEquals(pending, trace.pendingActions());
            assertEquals(List.of(), trace.phaseSnapshots());
            assertEquals(overlong, field.equals("expected") ? assertion.expected() : assertion.actual());

            trace.checkpoint("BODY", "THEN", "Retry with complete bounded summary", "Table", null, List.of(),
                    List.of(new AtfAssertion("Unicode boundary", boundary, boundary, true)), AtfReportStatus.PASS, List.of());
            var accepted = trace.phaseSnapshots().getFirst().checkpoints();
            assertEquals(1, accepted.size());
            assertEquals(pending, accepted.getFirst().actions());
            assertEquals(List.of(), trace.pendingActions());
            assertEquals(boundary, accepted.getFirst().assertions().getFirst().actual());
            assertEquals(settings, trace.publicationSettings().orElseThrow());
        }
    }

    @Test
    void liveCaptureKeepsItsPublicPolicyWhileLegacyReplayUsesPublicationPolicy(@TempDir Path artifacts,
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        var settings = new atf.reporting.configuration.ReportingSettings(5);
        var trace = new AndroidUiFlowTrace(artifacts, "frozen-policy", "Captured policy",
                "AndroidUiFlowTraceTest", "atf.android.e2e.support", "ro.whistlab.app",
                List.of("component"), System::nanoTime, settings);
        trace.checkpoint("BODY", "THEN", "Preserve original values", "Table", null, List.of(),
                List.of(new AtfAssertion("Boundary", "12345", "12345", true)), AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);
        var replay = atf.android.e2e.support.replay.CapturedAndroidFlow.read(trace.manifest());
        inputs.add("Live Reporting policy", settings.toString());
        inputs.add("Replay policy", replay.publicationSettings().toString());
        assertEquals(settings, trace.publicationSettings().orElseThrow());
        assertTrue(replay.publicationSettings().isEmpty(), "Legacy captures do not invent an original policy");
        assertEquals(trace.phaseSnapshots(), replay.phaseSnapshots());
    }
}
