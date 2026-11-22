package atf.tests.governance.documentation.language;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.documentation.policy.DocumentationLanguageRules;
import org.junit.jupiter.api.Test;
import java.util.List;

@AtfComponentTest
class DocumentationLanguageComponentTest {
    @Test void anchorAttributesRemainTechnicalDataInBothEditions() {
        for (String anchor : List.of("<a id=\"jocul-și-regulile\"></a>",
                "<A name='jocul' title='Overview'></A>", "<a id=overview></a>",
                "<a href=\"guide.md?before=after&title=>\" id='Overview'></a>")) {
            AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(anchor, "en"), anchor);
            AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(anchor, "ro"), anchor);
        }
    }

    @Test void anchorMarkupCannotHideForeignVisibleTextOrItsLine() {
        for (String foreign : List.of("<a id=\"overview\">Regulile jocului</a>",
                "<a href='guide.md'>Verificare și publicare</a>", "<a id='safe'></a>Verificare")) {
            AuditedAssertions.assertEquals(2,
                    DocumentationLanguageRules.firstForeignLine("# Guide\n" + foreign, "en"), foreign);
        }
        AuditedAssertions.assertEquals(2, DocumentationLanguageRules.firstForeignLine(
                "# Ghid\n<a id='pe-scurt'>Overview</a>", "ro"));
        AuditedAssertions.assertEquals(1, DocumentationLanguageRules.firstForeignLine(
                "&lt;a id=Overview&gt;", "ro"));
    }

    @Test void englishBranchRejectsShortForeignLabelsAndDiagramCaptions() {
        for (String prose : List.of("| Intrare | Contract |", "[Contractul bibliotecii](docs/guide.md)", "Modelul and snapshot-urile",
                "## Pe scurt", "```mermaid\nflowchart LR\n A[Scenariu] -->|trimite| B[Output]\n```",
                "```yaml\ntestChanges:\n  - reason: Păstrează contractul verificat\n```")) {
            AuditedAssertions.assertTrue(DocumentationLanguageRules.firstForeignLine(prose, "en") > 0, prose);
        }
    }

    @Test void romanianBranchRejectsAnEnglishGuideOrOwnerHeading() {
        for (String prose : List.of("## Overview", "[English guide](guide.md)",
                "This module must retain the source and its contract before publication.")) {
            AuditedAssertions.assertTrue(DocumentationLanguageRules.firstForeignLine(prose, "ro") > 0, prose);
        }
    }

    @Test void codeAndLinkTargetsRemainTechnicalDataWhileVisibleLabelsAreChecked() {
        String english = "Use `Pe scurt` in the Romanian edition. [Rules](rules.md#verificare)\n"
                + "~~~text\nÎnainte și după, fiecare modul păstrează dovezile.\n~~~";
        AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(english, "en"));
        String romanian = "Contractul păstrează `Overview` și [regulile](rules.md#overview).\n"
                + "````java\nThis and that must remain before the source.\n````";
        AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(romanian, "ro"));
        AuditedAssertions.assertEquals(3, DocumentationLanguageRules.firstForeignLine("# Guide\n\n| Intrare | Role |", "en"));
        AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(
                "```mermaid\nflowchart LR\n verificari[Audit] -->|checks| produs[APK]\n```", "en"));
        AuditedAssertions.assertEquals(0, DocumentationLanguageRules.firstForeignLine(
                "Validate useful, stateful components and handle a non-existent resource-URI carefully during an overhaul.", "en"));
    }
}
