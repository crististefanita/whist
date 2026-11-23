package atf.tests.governance.documentation.release.web;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class WebReleaseDossierComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();

    @Test void portableWebInventoryChecksHashesPathsAndEvidenceOwnership(ComponentInputs inputs) throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(30),
                value -> inputs.add("Synthetic positive and negative dossier controls", value), "web-release-dossier.test.mjs");
        assertTrue(output.contains("Web dossier observations:"), "Portable links and original bytes are checked alongside deliberate corruptions");
    }

    @Test void currentWebDossierHasOneIllustratedFlowAndNoComponentCaptures(ComponentInputs inputs) throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", root.resolve("atf-tests/tools/web-release-dossier.mjs").toString(), root.toString()),
                root, Duration.ofSeconds(30), value -> inputs.add("Current Web report inventory and immutable index digest", value));
        assertTrue(output.contains("\"status\":\"PASS\""), "Current relative report inventory is complete and hash-bound");
        assertTrue(output.contains("\"primaryE2e\":1"), "One illustrated journey complements the screenshot-free component contracts");
    }
}
