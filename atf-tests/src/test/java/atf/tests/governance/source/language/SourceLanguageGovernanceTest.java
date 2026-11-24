package atf.tests.governance.source.language;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class SourceLanguageGovernanceTest {
    @Test void implementationCommentsAndDiagnosticsUseEnglish(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var checkout = Path.of(System.getProperty("atf.governance.gitRoot", root.toString()));
        String output = runNode(List.of("node", root.resolve("atf-tests/tools/source-language.mjs").toString(), checkout.toString()),
                root, Duration.ofSeconds(30), value -> inputs.add("Inspected implementation files and language diagnostics", value));
        var actual = new ObjectMapper().readTree(output);
        AuditedAssertions.assertEquals("PASS", actual.path("status").asText());
        AuditedAssertions.assertTrue(actual.path("files").size() > 0, "The admitted source population is not empty");
        AuditedAssertions.assertEquals(0, actual.path("diagnostics").size(), "No reviewed source-language violations remain");
    }
}
