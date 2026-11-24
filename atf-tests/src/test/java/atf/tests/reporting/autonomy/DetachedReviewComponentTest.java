package atf.tests.reporting.autonomy;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DetachedReviewComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @Test void producerInventoryAndOfflineObservationsRejectMissingOrForeignQualification(ComponentInputs inputs) throws Exception {
        var script = GovernanceProject.locateProjectRoot().resolve("atf-tests/tools/report-autonomy.test.mjs");
        var process = new ProcessBuilder("node", "--test", script.toString()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "Offline reader contracts terminate within their budget");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        inputs.add("Preconditions", "Synthetic review receipts; no browser, images or historical report is required by this component.");
        inputs.add("Original contract output", output);
        assertEquals(0, process.exitValue(), "Positive identity and negative producer/interaction controls pass");
        assertTrue(output.contains("tests 3"), "The complete declared reader control population executes");
    }
}
