package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class AchievementOwnerComponentTest {
    @Test void activityOwnedStatePreservesTheSharedSelectionAndScroll(ComponentInputs inputs) throws Exception {
        assertTrue(run("restores the shared", inputs).contains("roundTrips=2; refusals=0"), "The shared State codec restores the row, selected stable goal and visible route without rewriting game state");
    }
    @Test void hiddenAbsentAndObsoleteStateCannotReopenAnotherRoute(ComponentInputs inputs) throws Exception {
        assertTrue(run("refuses hidden", inputs).contains("roundTrips=2; refusals=6"), "Hidden/missing routes, negative offsets and later old-owner mutation cannot overwrite the recreated owner");
    }
    private static String run(String pattern, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Actual Whist owner and common AchievementScreen.State, controlled Bundle port, real SDK API compilation and activity composition guards. No native recreation, pixels, translation or APK acceptance.");
        return NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=" + pattern,
                root.resolve("atf-ui-browser/tests/achievement-owner.test.mjs").toString()), root, Duration.ofSeconds(90),
                value -> inputs.add("Original owner round trips and rejection observations", value));
    }
}
