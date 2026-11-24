package atf.tests.governance.documentation.release.rag.scope.launcher;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class LauncherFixtureComponentTest {
    @Test void javaTimeoutRetainsCombinedOutputBeforeFixtureCleanup(@TempDir Path root, ComponentInputs inputs) throws Exception {
        Path log = root.resolve("launcher.log");
        String output = "captured stdout\ncaptured stderr\n";
        Files.writeString(log, output);
        String actual = LauncherFailureLog.timeoutMessage(log, Duration.ofSeconds(60));
        Files.delete(log);
        inputs.add("Original redirected output and retained timeout", output + "\n" + actual);
        AuditedAssertions.assertTrue(actual.contains("Launcher timed out:"), "Timeout identity survives cleanup");
        AuditedAssertions.assertTrue(actual.endsWith(output), "The retained combined output survives log deletion");
    }

    @Test void missingJavaLogDoesNotMaskTheOriginalTimeout(@TempDir Path root, ComponentInputs inputs) {
        String actual = LauncherFailureLog.timeoutMessage(root.resolve("missing.log"), Duration.ofSeconds(60));
        inputs.add("Missing diagnostic observation", actual);
        AuditedAssertions.assertTrue(actual.contains("Launcher timed out:"), "Missing output does not replace timeout identity");
        AuditedAssertions.assertTrue(actual.contains("Diagnostic capture failed:"), "Capture failure remains explicit");
    }

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
