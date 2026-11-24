package atf.tests.governance.documentation.language;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.policy.DocumentationTerminologyRules;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DocumentationTerminologyComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"SHAA-256", "checksumes", "READMEM", "Caache", "processor charger",
            "classpathin", "Lacquer delimited", "source snack", "Unbug", "Quotts"})
    void rejectsKnownArtifactsInVisibleEnglish(String term) {
        var findings = inspect(List.of("# Guide", "", "Compare " + term + "."), "en");
        assertEquals(1, findings.size());
        assertEquals("DOC-EN-TERMINOLOGY", findings.getFirst().code());
        assertEquals(3, findings.getFirst().line());
    }

    @Test void technicalLiteralsAndRomanianAreOutsideTheEnglishProseRule() {
        var lines = List.of("`SHAA-256`", "[Checksum](https://example.invalid/READMEM)",
                "~~~text", "classpathin", "~~~", "```json", "{\"key\":\"Caache\"}", "```",
                "Directory, SHA-256, checksum, asset, receipt and annotation processor.");
        assertEquals(List.of(), inspect(lines, "en"));
        assertEquals(List.of(), inspect(List.of("SHAA-256"), "ro"));
    }

    @Test void linkLabelsAndTableCellsRemainVisible() {
        assertEquals(List.of(1, 2), inspect(List.of("[READMEM](https://example.invalid)",
                "| Source | source snack |"), "en").stream().map(Violation::line).toList());
    }
    @Test @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
    void domainTermsRetainMeaningWithoutRejectingCodeOrExceptionTerminology(atf.android.evidence.component.ComponentInputs inputs) {
        var terms=List.of("invoicing","parametritions","pointeres","Immunobable","republican descriptor",
                "old republican","GNP","NGPs","KSPE","Book incidence","Manifesto components","deplete the head");
        for(String term:terms) {
            var findings=inspect(List.of("# Guide","","| Contract | "+term+" |"),"en");
            assertEquals(1,findings.size(),"Reviewed mistranslation is rejected in visible prose: "+term);
            assertEquals("DOC-EN-TERMINOLOGY",findings.getFirst().code());
            assertEquals(3,findings.getFirst().line());
            assertEquals(List.of(),inspect(List.of("`"+term+"`","```text",term,"```"),"en"),"Technical literals remain literal");
            assertEquals(List.of(),inspect(List.of(term),"ro"),"English terminology rules do not rewrite the Romanian edition");
        }
        assertEquals(List.of(),inspect(List.of("A catch block preserves the original exception.",
                "The invocation retains its PNG capture and original comparisons.",
                "Republication preserves the receipt; heap bounds do not qualify native pixels."),"en"));
        inputs.add("Reviewed semantic distinctions","invocation/capture; republication/new execution; heap/report size; PNG/source; technical literals/visible prose");
        inputs.add("Rejected translation artifacts",String.join(", ",terms));
    }

    private static List<Violation> inspect(List<String> lines, String language) {
        Path root = Path.of(".").toAbsolutePath();
        var findings = new ArrayList<Violation>();
        DocumentationTerminologyRules.validate(root.resolve("docs/Guide.md"), root, lines, language, findings);
        return findings;
    }
}
