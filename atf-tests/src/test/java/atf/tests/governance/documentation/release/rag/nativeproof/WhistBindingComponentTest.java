package atf.tests.governance.documentation.release.rag.nativeproof;

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
class WhistBindingComponentTest {
    @Test void mountedWhistViewsSupplyCurrentHeaderTrumpAndCardBindings(ComponentInputs inputs) throws Exception {
        String output = run("current content", inputs);
        assertTrue(output.contains("regions=10; current=8; rejected=0"), "The real consumer binder reads header, fact rows, trump and current card/index Views; detached cards are not cached");
    }
    @Test void missingDuplicateClippedAndDisposedWhistBindingsAreRejected(ComponentInputs inputs) throws Exception {
        String output = run("reject missing", inputs);
        assertTrue(output.contains("regions=10; current=8; rejected=6"), "Wrong-thread reads, missing or duplicate roles, clipped text and disposed/during-capture owners fail closed");
    }
    private static String run(String pattern, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Production Whist binding and common native measurement API; controlled View/Layout responses and screen composition guards. No APK, native fonts or glyph acceptance.");
        return NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=" + pattern,
                root.resolve("atf-ui-browser/tests/whist-native-bindings.test.mjs").toString()), root,
                Duration.ofSeconds(60), value -> inputs.add("Original binding observations and refusal causes", value));
    }
}
