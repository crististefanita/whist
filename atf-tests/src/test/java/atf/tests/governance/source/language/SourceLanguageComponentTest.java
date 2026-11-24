package atf.tests.governance.source.language;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class SourceLanguageComponentTest {
    @Test void acceptsLocalizedDataWithOneOwnedPopulation(ComponentInputs inputs) throws Exception {
        String actual = exercise("accepts real checkout", inputs);
        AuditedAssertions.assertTrue(actual.contains("\"status\":\"PASS\""), "Localized data remains admissible");
        AuditedAssertions.assertTrue(actual.contains("Case.java"), "The actual source population is captured");
    }

    @Test void rejectsRomanianImplementationOnBothEditions(ComponentInputs inputs) throws Exception {
        String actual = exercise("rejects implementation", inputs);
        AuditedAssertions.assertTrue(actual.contains("SOURCE-LANGUAGE-EN"), "The real inventory produces source-language diagnostics");
        AuditedAssertions.assertTrue(actual.contains("\"line\":2"), "The diagnostic identifies the offending source line");
        AuditedAssertions.assertTrue(actual.contains("\"edition\":\"en\"") && actual.contains("\"edition\":\"ro\""),
                "Implementation language does not change with the documentation edition");
    }

    private static String exercise(String scenario, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", scenario,
                root.resolve("atf-tests/tools/source-language-admission.test.mjs").toString()), root, Duration.ofSeconds(30),
                value -> inputs.add("Actual source inventory and admission comparisons", value));
    }
}
