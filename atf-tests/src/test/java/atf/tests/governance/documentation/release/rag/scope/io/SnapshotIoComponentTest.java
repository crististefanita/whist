package atf.tests.governance.documentation.release.rag.scope.io;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

/** Real filesystem boundaries; only the neighbouring artifact preflight is controlled. */
@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class SnapshotIoComponentTest {
    @Test void byteInventoriesAndNamedSnapshotPhasesRemainEquivalent(ComponentInputs inputs) throws Exception {
        String output = exercise("preserves independent", inputs);
        AuditedAssertions.assertTrue(output.contains("Snapshot I/O parity observations:"),
                "Independent byte hashes, hidden files, Unicode resources and exact inventory populations pass");
    }

    @Test void sameMetadataDriftCorruptionAndIndirectPathsCannotQualify(ComponentInputs inputs) throws Exception {
        String output = exercise("rejects same-metadata", inputs);
        AuditedAssertions.assertTrue(output.contains("Snapshot I/O rejection observations:"),
                "Source and stage drift, path escape, private material, links and failed preflight retain original refusals");
    }

    private static String exercise(String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", selection,
                root.resolve("atf-tests/tools/snapshot-io.test.mjs").toString()), root, Duration.ofSeconds(70),
                value -> inputs.add("Actual filesystem, byte comparison and phase observations", value));
    }
}
