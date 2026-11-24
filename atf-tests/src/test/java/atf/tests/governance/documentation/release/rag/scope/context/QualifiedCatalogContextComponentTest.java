package atf.tests.governance.documentation.release.rag.scope.context;

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
class QualifiedCatalogContextComponentTest {
    @Test void sourceAndCatalogIdentitySurvivesRestartWithoutRelabelingHistoricalReports(ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "A modern execution pins source, full release inventory and catalog index before execution. A historical receipt remains immutable; publication is a later observation.");
        String output = contracts("qualified source|historical|PowerShell", inputs);
        AuditedAssertions.assertTrue(output.contains("qualified source and catalog context reconstructs exactly"),
                "The original source/catalog identity is reconstructed from the bound manifests");
        AuditedAssertions.assertTrue(output.contains("historical receipts stay explicit and immutable"),
                "Old reports never acquire a newly invented qualification context");
        AuditedAssertions.assertTrue(output.contains("restart reconstruct the same context"),
                "The real PowerShell restart admits the same context and rejects altered workspace identity");
    }

    @Test void missingForeignContextAndLiveCatalogCorruptionCannotQualify(ComponentInputs inputs) throws Exception {
        inputs.add("Negative controls", "Independently alter source, catalog, inventory, context identity, schema and asset bytes; remove catalog files and modern declarations.");
        String output = contracts("missing foreign|live catalog drift", inputs);
        AuditedAssertions.assertTrue(output.contains("cannot bind a modern origin"),
                "Missing, foreign, downgraded and malformed context fails source admission");
        AuditedAssertions.assertTrue(output.contains("corrupt APK bytes are rejected independently"),
                "Live delivery drift is rejected without rewriting original qualified reports");
    }

    private static String contracts(String pattern, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", pattern,
                root.resolve("atf-tests/tools/component-qualification-context.test.mjs").toString()), root,
                Duration.ofSeconds(45), output -> inputs.add("Captured inputs, assertions and outcomes", output));
    }
}
