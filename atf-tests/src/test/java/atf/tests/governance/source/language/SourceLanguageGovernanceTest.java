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
        var profile=new ObjectMapper().readTree(root.resolve("config/source-language-inventory-profile.json").toFile());
        AuditedAssertions.assertEquals(1,profile.path("schemaVersion").asInt());
        AuditedAssertions.assertEquals("WHIST_SOURCE_LANGUAGE_INVENTORY",profile.path("kind").asText());
        long timeout=profile.path("timeoutMs").asLong();
        AuditedAssertions.assertTrue(timeout>0&&timeout<=180000,"The complete source inventory has its own bounded execution budget");
        inputs.add("Declared complete inventory profile",profile.toPrettyString());
        String output = runNode(List.of("node", root.resolve("atf-tests/tools/source-language.mjs").toString(), checkout.toString()),
                root, Duration.ofMillis(timeout), value -> inputs.add("Inspected implementation files and language diagnostics", value));
        var actual = new ObjectMapper().readTree(output);
        AuditedAssertions.assertEquals("PASS", actual.path("status").asText());
        AuditedAssertions.assertTrue(actual.path("files").size() > 0, "The admitted source population is not empty");
        AuditedAssertions.assertEquals(0, actual.path("diagnostics").size(), "No reviewed source-language violations remain");
    }
}
