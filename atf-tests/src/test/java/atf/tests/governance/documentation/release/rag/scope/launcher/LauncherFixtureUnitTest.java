package atf.tests.governance.documentation.release.rag.scope.launcher;

import atf.testkit.AtfUnitTest;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runContracts;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class LauncherFixtureUnitTest {
    @Test void javaDiagnosticTailKeepsItsByteBudgetForAsciiAndUnicode() {
        for (String source : List.of("x".repeat(100), "😀é".repeat(50))) {
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            for (int limit : List.of(1, 2, 3, 7, 64, 65536))
                assertTrue(LauncherFailureLog.tail(bytes, limit).getBytes(StandardCharsets.UTF_8).length <= limit);
            assertEquals(source, LauncherFailureLog.tail(bytes, 65536));
        }
        assertThrows(IllegalArgumentException.class, () -> LauncherFailureLog.tail(new byte[0], 0));
    }

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
