package atf.tests.governance.documentation.release.rag.scope.launcher;

import atf.testkit.AtfUnitTest;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runContracts;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class LauncherFixtureUnitTest {
    @Test void pureCompilerMatricesKeepPayloadAndMalformedInputCoverageWithoutExtent() throws Exception {
        assertTrue(runContracts(Duration.ofSeconds(15), "component-compiler-inputs.test.mjs").contains("invalid compiler scopes"));
    }

    @Test void boundedStreamsAndFailureCausesUseMocksWithoutRepeatingRealLaunches() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        assertTrue(runNode(List.of("node", "--test", "--test-name-pattern", "pure stream|mocked timeout",
                root.resolve("atf-tests/tools/fixture-process.test.mjs").toString()), root, Duration.ofSeconds(15))
                .contains("mocked timeout and exit failures"));
    }
}
