package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import atf.android.evidence.component.ComponentInputs;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class WhistSurfaceAdapterComponentTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final GameReportAudit AUDIT = new GameReportAudit();
    @Test void webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                "--test-concurrency=1", root.resolve("atf-ui-browser/tests/whist-surface-contract.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/automation-handoff.test.mjs").toString()), root, Duration.ofSeconds(160),
                value -> inputs.add("Original Web/native adapter observations", value));
        assertTrue(output.contains("Whist adapter observations:"), output);
        assertTrue(output.contains("AutoGame handoff observations:"), "Production command ownership preserves automation intent without duplicate decisions");
    }

    @Test void mountedCardsAndLayoutOwnersSurvivePhaseAndActorChanges(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test", "--test-concurrency=1",
                root.resolve("atf-ui-browser/tests/seat-paint.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/layout-delivery.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/asset-revision.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/hand-lifecycle.test.mjs").toString()), root, Duration.ofSeconds(90),
                value -> inputs.add("Original mounted-card and delayed-owner observations", value));
        assertTrue(output.contains("Seat paint observations:"), "Frames, hand faces and public cards retain their identities");
        assertTrue(output.contains("Layout delivery observations:"), "Delayed or reordered layout replies cannot overwrite committed frame and card positions");
        assertTrue(output.contains("Hand lifecycle observations:"), "A retained hand has one live input owner and rejects disposed callbacks");
        assertTrue(output.contains("Asset revision observations:"), "The live shell cannot load an obsolete delegated-hand adapter");
    }

    @Test void adaptiveWebMenusKeepTextIndependentOfTheUsableViewport(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/responsive-surface.test.mjs").toString()), root, Duration.ofSeconds(120),
                value -> inputs.add("Actual Chromium menus with a controlled API boundary", value));
        assertTrue(output.contains("Adaptive policy observations:"), "Model boundaries and invalid dimensions are exercised");
        assertTrue(output.contains("Responsive renderer observations:"), "Actual renderer measurements accompany the comparisons");
    }

    @Test void dropdownAndScoreTypographyPreserveNativeFactsAndCommandBindings(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/score-typography.test.mjs").toString()), root, Duration.ofSeconds(120),
                value -> inputs.add("Measured dropdown and score typography, original cells and command bindings", value));
        assertTrue(output.contains("Dropdown and score typography observations:"), "Measured role proportions and unchanged score cells accompany the verdict");
    }

    @Test void surfaceLifecycleRejectsInvalidPagesAndCallbacksAfterDisposal(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/surface-lifecycle.test.mjs").toString()), root, Duration.ofSeconds(30),
                value -> inputs.add("Controlled observer, animation-frame and pagination deliveries", value));
        assertTrue(output.contains("disposal rejects late animation frames"), "Late callback and observer reconnection regressions execute");
        assertTrue(output.contains("invalid page populations fail"), "Rejected page populations preserve existing content");
    }
}
