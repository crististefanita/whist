package atf.tests.governance.documentation.release.rag.scope.metrics;

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

/** Actual PowerShell functions and filesystem; only journal-storage refusal is controlled. */
@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class QualificationCostComponentTest {
    @Test void timingRetainsActionResultsAndExistingJournalOwnership(ComponentInputs inputs) throws Exception {
        String observed = exercise("timing keeps action", inputs);
        AuditedAssertions.assertTrue(observed.contains("Timing nominal observations:"),
                "Real atomic journal writes preserve action result, native refusal and original command cause");
    }

    @Test void nestedJournalFailureCannotReplaceTheOriginalCancellation(ComponentInputs inputs) throws Exception {
        String observed = exercise("timing retains primary", inputs);
        AuditedAssertions.assertTrue(observed.contains("Timing failure observations:"),
                "Controlled storage refusal retains both original causes, phase ownership and the previous published journal");
    }

    @Test void measuredSealAndReceiptPreserveIndependentByteIdentity(ComponentInputs inputs) throws Exception {
        String observed = exercise("seal measurements preserve", inputs);
        AuditedAssertions.assertTrue(observed.contains("Seal cost observations:"),
                "Independent SHA-256 oracle, counted files/bytes and nonoverlapping cost partitions reconcile exactly");
    }

    @Test void costsCannotAuthorizeMissingOrCorruptedCompilerOutput(ComponentInputs inputs) throws Exception {
        String observed = exercise("measured seals still reject", inputs);
        AuditedAssertions.assertTrue(observed.contains("Seal rejection observations:"),
                "Same-metadata drift, missing bytecode, foreign output and identity reject actual Maven reuse");
    }

    private static String exercise(String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Real timing, sealing and receipt functions in an isolated external directory. No Maven build, device, screenshots or wall-clock performance threshold; journal refusal alone uses an injected writer.");
        return runNode(List.of("node", "--test", "--test-name-pattern", selection,
                root.resolve("atf-tests/tools/qualification-costs.test.mjs").toString()), root,
                Duration.ofSeconds(70), value -> inputs.add("Original process comparisons and measured partitions", value));
    }
}
