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
                root.resolve("atf-ui-browser/tests/seat-measurements.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/table-height.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/table-continuity.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/layout-delivery.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/asset-revision.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/hand-lifecycle.test.mjs").toString()), root, Duration.ofSeconds(90),
                value -> inputs.add("Original mounted-card and delayed-owner observations", value));
        assertTrue(output.contains("Seat paint observations:"), "Frames, hand faces and public cards retain their identities");
        assertTrue(output.contains("Seat measurement observations:"), "Bid and trick values cannot change the native-style fitted line height");
        assertTrue(output.contains("Intrinsic table observations:"), "Allocated board slack cannot inflate the next hand");
        assertTrue(output.contains("Table continuity oracle observations:"), "Accepted fan recentering does not conceal frame or AI-turn drift");
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

    @Test void individualDepositsKeepOrderedBeforeAfterComparisons(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/table-checkpoints.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/table-continuity.test.mjs").toString()), root, Duration.ofSeconds(30),
                value -> inputs.add("Actual before/after trace-oracle outputs, including drift mutations", value));
        assertTrue(output.contains("Table checkpoint observations:"), "Individual deposits retain field assertions and observation ownership");
    }

    @Test void preparedAndLiveSeatsShareIntrinsicHeightWithoutBottomAnchorStretch(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test", "--test-concurrency=1",
                root.resolve("atf-ui-browser/tests/seat-handoff.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/layout-delivery.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/asset-revision.test.mjs").toString()), root, Duration.ofSeconds(60),
                value -> inputs.add("Controlled anchor, text, assigned-height and callback measurements without screenshots", value));
        assertTrue(output.contains("Seat handoff measurements:"), "Prepared south anchors and live playback retain one intrinsic demand");
        assertTrue(output.contains("Asset revision observations:"), "The published adapter does not retain the previous measurement implementation in cache");
        assertTrue(output.contains("Fractional viewport observations:"), "Mobile zoom heights cross the integer geometry boundary without a visible error across bidding and play");
        assertTrue(output.contains("Invalid viewport observations:"), "Malformed or non-positive dimensions retain their original rejection");
    }

    @Test void everySeatAndLegalDealShareOneNativeWebPhaseContract(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test", "--test-concurrency=1",
                "--test-name-pattern=all legal|production Web|CSS text", root.resolve("atf-ui-browser/tests/phase-contract.test.mjs").toString(),
                root.resolve("atf-ui-browser/tests/whist-surface-contract.test.mjs").toString()), root, Duration.ofSeconds(100),
                value -> inputs.add("Shared APK/Web matrix, observed plans and explicit renderer boundary", value));
        assertTrue(output.contains("Shared table phase observations:"), "All six schedules, legal hand sizes, actors and winners use one oracle");
    }

    @Test void sharedPhaseOracleRejectsEveryDisplacedOrMissingSeat(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=shifted",
                root.resolve("atf-ui-browser/tests/phase-contract.test.mjs").toString()), root, Duration.ofSeconds(60),
                value -> inputs.add("Controlled one-pixel drift and missing-seat mutations", value));
        assertTrue(output.contains("Shared table phase negative controls:"), "The oracle rejects all 76 deliberate mutations");
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

    @Test void portableMenuRolesPreserveBothAdapterBaselines(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/menu-roles.test.mjs").toString()), root, Duration.ofSeconds(60),
                value -> inputs.add("Original portable text roles and browser CSS conversion", value));
        assertTrue(output.contains("Shared menu role observations:"), "Java roles retain approved native defaults and browser proportions");
    }

    @Test void minimalFlowsReconcileHandsAndRejectMatricesOrExcessiveCaptures(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/flow-budget.test.mjs").toString()), root, Duration.ofSeconds(30),
                value -> inputs.add("Original flow plans, executions and rejection causes", value));
        assertTrue(output.contains("Flow budget observations:"), "A selected two-hand flow reconciles actual hands and captures");
        assertTrue(output.contains("Flow budget rejection observations:"), "Long plans, mismatched execution and extra captures are rejected");
    }

    @Test void deliveredSiteModulesAndFontsAreAdmittedBeforeBrowserStartup(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test", "--test-concurrency=1",
                root.resolve("atf-ui-browser/tests/static-host.test.mjs").toString()), root, Duration.ofSeconds(60),
                value -> inputs.add("Controlled static-site inputs, original HTTP bytes and admission failures", value));
        assertTrue(output.contains("Static site HTTP observations:"), "Every delivered module and font retains its bytes and MIME");
        assertTrue(output.contains("Static site admission observations:"), "Missing dependencies reject startup before the browser callback");
        assertTrue(output.contains("Static site rejection observations:"), "Unknown paths, methods and traversal remain rejected");
        assertTrue(output.contains("Static site snapshot observations:"), "Post-admission file changes cannot alter the active host");
    }
}
