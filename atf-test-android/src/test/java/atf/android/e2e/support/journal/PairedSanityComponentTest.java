package atf.android.e2e.support.journal;

import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.flow.WhistLabScenario;
import atf.android.e2e.support.replay.CapturedAndroidFlow;
import atf.android.evidence.component.ComponentInputs;
import atf.android.evidence.flow.UiSanityPlan;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class PairedSanityComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @TempDir Path root;

    @Test void sharedPlanExecutesTheSameRolesInBothAdaptersAndPreservesFailure(ComponentInputs inputs) throws Exception {
        var plan = plan(); var nativeSteps = new ArrayList<String>(); plan.execute(nativeSteps::add);
        Path source = WhistLabScenario.checkoutRoot().resolve("atf-ui-browser/tests/sanity-plan.test.mjs");
        var process = new ProcessBuilder("node", "--test", source.toString()).redirectErrorStream(true).start();
        String observed = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "Offline Web contract terminates within its budget");
        inputs.add("Shared scenario", plan.json()); inputs.add("Web adapter observations", observed);
        assertEquals(0, process.exitValue(), "Web driver and rejection controls pass");
        assertTrue(observed.contains(String.join("|", nativeSteps)), "Both drivers execute identical ordered roles from the same source");
        assertEquals(List.of(1, 1), plan.deals(), "No UI matrix or long game enters the primary sanity");
        assertEquals(4, plan.captureSteps().size(), "Normal images are limited to four semantic checkpoints");
    }

    @Test void nativeSemanticStepsAndFourCapturesSurviveReplayAndDetachedPublication(ComponentInputs inputs) throws Exception {
        var trace = trace(); var plan = plan(); trace.useSanityPlan(plan);
        plan.execute(step -> checkpoint(trace, step, plan.captureSteps().contains(step), AtfReportStatus.PASS));
        trace.finish(AtfReportStatus.PASS);
        var replay = CapturedAndroidFlow.read(trace.manifest());
        inputs.add("Preconditions", "Synthetic native observations only; two one-card hands, seven ordered roles and four tiny publisher-input PNGs, not device acceptance.");
        assertEquals(plan, replay.sanityPlan().orElseThrow(), "Replay retains original plan identity and budgets");
        assertEquals(7, replay.phaseSnapshots().getFirst().checkpoints().size(), "Semantic steps are not lost when they have no image");
        Path report = root.resolve("paired.html");
        AndroidUiFlowReportPublisher.publish(report, "fixture APK", "no-device", "28", "Controlled publisher qualification", List.of(), replay);
        var html = Jsoup.parse(Files.readString(report));
        for (String step : plan.steps()) assertTrue(html.text().contains("Observed " + step), "Detached HTML retains step " + step);
        assertTrue(html.text().contains(plan.id()), "Shared scenario ID remains in HTML");
        assertEquals(4L, replay.phaseSnapshots().stream().flatMap(p -> p.checkpoints().stream()).filter(c -> c.screenshot() != null).count(), "Native captures obey the common budget");
        assertEquals(0, html.select("img[src]:not([src^=data:]),a[href^=file:]").size(), "Publication needs no external images or files");
    }

    @Test void missingExtraOrReorderedEvidenceIsRejectedBeforeChangingTheJournal(ComponentInputs inputs) throws Exception {
        var trace = trace(); trace.useSanityPlan(plan()); byte[] before = Files.readAllBytes(trace.manifest());
        inputs.add("Negative controls", "Missing bidding image, out-of-order role, then excess image at a semantic-only checkpoint; each refusal preserves the prior journal.");
        assertThrows(IllegalArgumentException.class, () -> checkpoint(trace, "bidding", false, AtfReportStatus.PASS));
        assertThrows(IllegalArgumentException.class, () -> checkpoint(trace, "auto-score", true, AtfReportStatus.PASS));
        assertEquals(Base64.getEncoder().encodeToString(before), Base64.getEncoder().encodeToString(Files.readAllBytes(trace.manifest())));
        checkpoint(trace, "bidding", true, AtfReportStatus.PASS);
        checkpoint(trace, "before-human", true, AtfReportStatus.PASS);
        checkpoint(trace, "after-human", true, AtfReportStatus.PASS);
        assertThrows(IllegalArgumentException.class, () -> checkpoint(trace, "manual-score", true, AtfReportStatus.PASS));
        checkpoint(trace, "failure", true, AtfReportStatus.FAIL);
        assertThrows(IllegalArgumentException.class, () -> checkpoint(trace, "failure", true, AtfReportStatus.FAIL));
        trace.fail(new IllegalStateException("Original controlled failure")); trace.finish(AtfReportStatus.FAIL);
        var replay = CapturedAndroidFlow.read(trace.manifest());
        AndroidUiFlowReportPublisher.publish(root.resolve("failed.html"), "fixture", "offline", "28", "Original failure", List.of(), replay);
        assertTrue(Jsoup.parse(Files.readString(root.resolve("failed.html"))).text().contains("Original controlled failure"), "Failure prefix publishes the original cause with one failure image");
    }

    private static UiSanityPlan plan() throws Exception { return UiSanityPlan.load(WhistLabScenario.checkoutRoot().resolve("config/table-sanity-plan.json")); }
    private AndroidUiFlowTrace trace() throws Exception {
        return new AndroidUiFlowTrace(root, "paired", "Paired sanity fixture", "Fixture", "atf.android.journal", "ro.whistlab.app", List.of("component"));
    }
    private void checkpoint(AndroidUiFlowTrace trace, String step, boolean captured, AtfReportStatus status) throws Exception {
        Path png = captured ? Files.write(root.resolve(step + ".png"), Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7XcAAAAASUVORK5CYII=")) : null;
        var action = new AtfUiActionRecord(1, "Observe " + step, step, "captured", "captured", 1, status, AtfUiEvidenceSource.OBSERVE);
        trace.checkpoint("BODY", "THEN", "Observed " + step, step, png, List.of(action),
                List.of(new AtfAssertion("State " + step, "ready", status == AtfReportStatus.PASS ? "ready" : "stopped", status == AtfReportStatus.PASS)), status, List.of());
    }
}
