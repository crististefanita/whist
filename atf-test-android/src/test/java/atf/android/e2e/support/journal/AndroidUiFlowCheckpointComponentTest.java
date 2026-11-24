package atf.android.e2e.support.journal;

import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.replay.CapturedAndroidFlow;
import atf.android.evidence.component.ComponentInputs;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.jsoup.Jsoup;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class AndroidUiFlowCheckpointComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();

    @Test void rejectedInputsPreservePendingActionsExistingPhasesAndOriginalManifest(
            @TempDir Path root, ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "An existing checkpoint and a failed pending dispatch; reject incomplete new inputs before changing ownership or journal bytes.");
        for (String field : List.of("keyword", "title", "target", "assertions", "assertion-element", "action-element", "status", "image", "artifact", "new-phase")) {
            var trace = trace(Files.createDirectory(root.resolve(field)));
            checkpoint(trace);
            trace.recordAction(dispatch(AtfReportStatus.FAIL));
            byte[] original = Files.readAllBytes(trace.manifest());
            var pending = trace.pendingActions();
            var phases = trace.phaseSnapshots();
            RuntimeException rejected = assertThrows(RuntimeException.class, () -> trace.checkpoint(
                    field.equals("new-phase") ? "UNADMITTED" : "BODY", field.equals("keyword") ? " " : "THEN",
                    field.equals("title") || field.equals("new-phase") ? " " : "Candidate", field.equals("target") ? "" : "score",
                    field.equals("image") ? root.resolve("foreign.png") : null,
                    field.equals("action-element") ? Arrays.asList((AtfUiActionRecord) null) : List.of(),
                    field.equals("assertions") ? null : field.equals("assertion-element")
                            ? Arrays.asList((AtfAssertion) null) : List.of(comparison()),
                    field.equals("status") ? null : AtfReportStatus.PASS,
                    field.equals("artifact") ? List.of(root.resolve("foreign.xml")) : List.of()));
            inputs.add("Controlled refusal: " + field, rejected.getClass().getSimpleName() + ": " + rejected.getMessage());
            assertEquals(pending, trace.pendingActions(), "Rejected " + field + " retains the original dispatch");
            assertEquals(phases, trace.phaseSnapshots(), "Rejected " + field + " cannot add or reorder a phase");
            assertEquals(Base64.getEncoder().encodeToString(original), Base64.getEncoder().encodeToString(Files.readAllBytes(trace.manifest())),
                    "Rejected " + field + " leaves the last journal unchanged");
            checkpoint(trace);
            trace.finish(AtfReportStatus.PASS);
            assertEquals(AtfReportStatus.FAIL, trace.status(), "Retry cannot erase the original failed dispatch");
            assertEquals(1L, trace.phaseSnapshots().stream().flatMap(p -> p.checkpoints().stream())
                    .flatMap(c -> c.actions().stream()).filter(a -> a.source().name().equals("ACTION")).count(),
                    "The retained dispatch receives exactly one checkpoint owner");
        }
    }

    @Test void failedJournalWriteRollsBackOnlyTheUncommittedCheckpointAndRetriesOnce(
            @TempDir Path root, ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "Block the real journal temporary path, preserve pending dispatch and retry after removing the owned obstruction; no Android commands.");
        for (boolean existingPhase : List.of(false, true)) {
            var trace = trace(Files.createDirectory(root.resolve(Boolean.toString(existingPhase))));
            if (existingPhase) checkpoint(trace);
            trace.recordAction(dispatch(AtfReportStatus.PASS));
            var pending = trace.pendingActions(); var phases = trace.phaseSnapshots();
            byte[] original = Files.readAllBytes(trace.manifest());
            Path temporary = trace.manifest().resolveSibling(trace.manifest().getFileName() + ".tmp");
            Files.createDirectory(temporary);
            IOException failure = assertThrows(IOException.class, () -> checkpoint(trace));
            inputs.add("Original filesystem failure, existing phase=" + existingPhase, failure.toString());
            assertEquals(pending, trace.pendingActions(), "Uncommitted journal failure preserves every pending action");
            assertEquals(phases, trace.phaseSnapshots(), "Rollback removes only the candidate checkpoint and its new phase");
            assertEquals(Base64.getEncoder().encodeToString(original), Base64.getEncoder().encodeToString(Files.readAllBytes(trace.manifest())),
                    "The prior journal survives an interrupted write");
            Files.delete(temporary);
            checkpoint(trace);
            var journal = read(trace.manifest());
            assertEquals("0", journal.getProperty("flow.pendingActionCount"), "Retry transfers pending ownership exactly once");
            assertEquals(existingPhase ? "2" : "1", journal.getProperty("phase.0.stepCount"));
            assertEquals(1L, trace.phaseSnapshots().stream().flatMap(p -> p.checkpoints().stream())
                    .flatMap(c -> c.actions().stream()).filter(a -> a.source().name().equals("ACTION")).count());
        }
    }

    @Test void timingFailureAfterReplacementCannotRollBackCommittedEvidence(
            @TempDir Path root, ComponentInputs inputs) throws Exception {
        var calls = new AtomicInteger();
        var failAt = new AtomicInteger(Integer.MAX_VALUE);
        var trace = new AndroidUiFlowTrace(root, "timing-boundary", "Committed checkpoint and timing failure",
                "Fixture", "atf.android.journal", "ro.whistlab.app", List.of("component"), () -> {
                    int call = calls.incrementAndGet(); return call == failAt.get() ? 0L : call * 1_000L;
                });
        trace.beginPhase("BODY");
        trace.recordAction(dispatch(AtfReportStatus.FAIL));
        failAt.set(calls.get() + 2);
        var failure = assertThrows(IllegalStateException.class, () -> checkpoint(trace));
        inputs.add("Original post-commit failure", failure.toString());
        assertTrue(failure.getMessage().contains("clock moved backwards"), "Timing keeps its original cause");
        assertEquals(1, trace.phaseSnapshots().getFirst().checkpoints().size(), "A committed checkpoint retains its owner");
        assertTrue(trace.pendingActions().isEmpty(), "A committed dispatch cannot become pending again");
        var journal = read(trace.manifest());
        assertEquals("1", journal.getProperty("phase.0.stepCount"), "Disk and memory agree after timing refusal");
        assertEquals("1", journal.getProperty("phase.0.step.0.actionCount"));
        trace.fail(failure);
        trace.finish(AtfReportStatus.PASS);
        assertEquals(AtfReportStatus.FAIL, trace.status(), "Later completion cannot erase the timing or dispatch failure");
    }

    @Test void originalDispatchAndFailureSurviveReplayAndDetachedHtml(
            @TempDir Path root, ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "A failed dispatch precedes an invalid checkpoint. Preserve the cause, then publish the next real controlled observation and inspect HTML with its source files removed.");
        var trace = trace(root);
        trace.beginPhase("BODY");
        trace.recordAction(dispatch(AtfReportStatus.FAIL));
        var original = assertThrows(IllegalArgumentException.class, () -> trace.checkpoint("BODY", " ",
                "Rejected candidate", "score", null, List.of(), List.of(comparison()), AtfReportStatus.PASS, List.of()));
        trace.fail(original);
        Path png = root.resolve("controlled-publisher-input.png");
        Files.write(png, Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
        trace.checkpoint("BODY", "THEN", "Observed state after rejection", "score", png,
                List.of(new AtfUiActionRecord(9, "Inspect", "score", "ready", "ready", 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(comparison()), AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);
        var replay = CapturedAndroidFlow.read(trace.manifest());
        assertEquals(trace.pendingActions(), replay.pendingActions(), "Replay preserves the failed dispatch and its original sequence");
        assertEquals(trace.failure(), replay.failure(), "Replay preserves the original checkpoint refusal");
        Path report = AndroidUiFlowReportPublisher.publish(root.resolve("published.html"), "package:/controlled.apk",
                "mock-device", "28", "Controlled publisher acceptance; no Android interaction", List.of(png), replay);
        Path detached = Files.createDirectory(root.resolve("detached")).resolve("audit.html");
        Files.copy(report, detached);
        Files.delete(png); Files.delete(trace.manifest()); Files.delete(report);
        var document = Jsoup.parse(Files.readString(detached));
        inputs.add("Original failure retained in detached HTML", replay.failure());
        assertEquals(AtfReportStatus.FAIL, replay.status(), "Successful final observation cannot relabel the failed execution");
        assertTrue(document.text().contains("Original dispatch"), "Detached HTML retains the failed command's observed value");
        assertTrue(document.text().contains(original.getMessage()), "Detached HTML retains the original refusal cause");
        assertTrue(document.text().contains("Captured state"), "Detached HTML retains its evaluated comparison");
        assertTrue(Files.readString(detached).contains("data:image/png;base64,"), "Publisher fixture image is self-contained");
        assertFalse(document.select("a[href]").stream().anyMatch(a -> a.attr("href").startsWith("file:") || a.attr("href").endsWith(".properties")),
                "The accepted HTML requires neither its manifest nor workstation file links");
    }

    private static AndroidUiFlowTrace trace(Path root) throws IOException {
        return new AndroidUiFlowTrace(root, "checkpoint", "Controlled checkpoint transaction", "Fixture",
                "atf.android.journal", "ro.whistlab.app", List.of("component", "offline"));
    }
    private static AtfUiActionRecord dispatch(AtfReportStatus status) {
        return new AtfUiActionRecord(7, "Tap", "Continue", "accepted", "Original dispatch", 11, status);
    }
    private static AtfAssertion comparison() { return new AtfAssertion("Captured state", "ready", "ready", true); }
    private static void checkpoint(AndroidUiFlowTrace trace) throws IOException {
        trace.checkpoint("BODY", "THEN", "Accepted observation", "score", null, List.of(), List.of(comparison()), AtfReportStatus.PASS, List.of());
    }
    private static Properties read(Path path) throws IOException {
        var values = new Properties(); try (var input = Files.newInputStream(path)) { values.load(input); } return values;
    }
}
