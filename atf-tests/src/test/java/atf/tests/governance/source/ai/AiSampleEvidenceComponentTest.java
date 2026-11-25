package atf.tests.governance.source.ai;

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
class AiSampleEvidenceComponentTest {
    @Test void finiteWeightedFrequenciesAndPointsMatchIndependentOracles(ComponentInputs inputs) throws Exception {
        String observed = run("finite weighted", inputs);
        assertTrue(observed.contains("contexts=224; scales=672"), "Two to eight cards, both decision objectives and representable weight scales retain independent frequency, points and effective-sample oracles");
    }
    @Test void invalidSamplesPreserveEvidenceAndTheOriginalDefectIsDetected(ComponentInputs inputs) throws Exception {
        String observed = run("invalid rows", inputs);
        assertTrue(observed.contains("rows=14; domains=3; continuation=unchanged"), "Every rejected row leaves the admitted evidence unchanged");
        assertTrue(observed.contains("Rejected row changed accumulated options"), "The same oracle catches a controlled mutant that restores the original partial-mutation defect");
    }
    private static String run(String pattern, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Actual weighted accumulator and Romanian score function, independent finite-population/decimal oracle; controlled outcomes are not new matches or posterior calibration.");
        return NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=" + pattern,
                root.resolve("atf-ui-browser/tests/bid-evidence.test.mjs").toString()), root, Duration.ofSeconds(90),
                value -> inputs.add("Original weighted observations and baseline refusal", value));
    }
}
