package atf.tests.governance.documentation.release.rag.scope.launcher;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class LauncherFixtureComponentTest {
    @Test void completedChildrenRetainIndependentTimingsAndResourceIdentity(ComponentInputs inputs) throws Exception {
        String actual = exercise("successful child observations", inputs);
        AuditedAssertions.assertTrue(actual.contains("second-child"), "The actual process observations retain their identity");
        AuditedAssertions.assertTrue(actual.contains("elapsedMs"), "Each invocation retains its measured elapsed time");
    }

    @Test void interruptedChildPreservesBothStreamsBeforeOwnedFixtureCleanup(ComponentInputs inputs) throws Exception {
        String actual = exercise("real interrupted child", inputs);
        AuditedAssertions.assertTrue(actual.contains("prefix-out") && actual.contains("prefix-error"),
                "Both child streams survive removal of the fixture directory");
        AuditedAssertions.assertTrue(actual.contains("ETIMEDOUT"), "The original timeout cause remains identifiable");
    }

    private static String exercise(String scenario, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", scenario,
                root.resolve("atf-tests/tools/fixture-process.test.mjs").toString()), root, Duration.ofSeconds(15),
                value -> inputs.add("Original child streams, resource limits and timings", value));
    }
}
