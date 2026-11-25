package atf.tests.governance.source.changes.domains;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class TestChangeDomainComponentTest {
    private static final String VALIDATOR = "atf-tests/src/test/java/atf/tests/governance/source/language/SourceLanguageGovernanceTest.java";
    private static final String UI = "atf-ui-browser/tests/native-observation.test.mjs";
    private static final String REPORT = "atf-tests/src/test/java/atf/tests/reporting/acceptance/ComponentPublicationComponentTest.java";

    @Test void reviewedDomainsPreserveChecksAndKeepTheirPathsDisjoint(ComponentInputs inputs) throws Exception {
        var domains = ReviewedDomains.load();
        inputs.add("Precondition", "Read the live reviewed declarations; historical evidence and its compound control remain immutable.");
        inputs.add("Expected domains", ReviewedDomains.IDS.toString());
        var paths = new HashSet<String>(); var checks = new HashSet<String>();
        var owners = new HashMap<String, HashSet<String>>();
        for (String id : domains.domainIds()) {
            inputs.add(id, domains.row(id).toPrettyString());
            assertFalse(domains.row(id).path("checks").path("negative").isEmpty(), "Each independent domain retains rejection coverage");
            var basePaths = owners.computeIfAbsent(domains.row(id).path("base").asText(), ignored -> new HashSet<>());
            for (String path : domains.paths(id)) assertTrue(basePaths.add(path), "Exactly one domain per Git base owns " + path);
            if (ReviewedDomains.IDS.contains(id)) { paths.addAll(domains.paths(id)); checks.addAll(domains.checks(id)); }
            for (String check : domains.checks(id)) assertTrue(belongs(id, check), "A reviewed domain cannot absorb an unrelated executable: " + id + " -> " + check);
        }
        assertTrue(checks.containsAll(domains.checks(ReviewedDomains.LEGACY)), "Partitioning retains every check of the original coupled control");
        assertTrue(paths.containsAll(domains.paths(ReviewedDomains.LEGACY)), "No originally reviewed path loses its current owner");
        assertTrue(domains.checks(ReviewedDomains.VALIDATOR).stream().allMatch(value -> value.contains(".source.language.")), "Validator controls contain no native UI, heap or publication bodies");
        assertTrue(domains.checks(ReviewedDomains.UI).stream().allMatch(value -> value.contains(".nativeproof.")), "Native surface acceptance has its own domain");
        assertFalse(domains.checks(ReviewedDomains.CATALOG).stream().anyMatch(value -> value.contains(".nativeproof.")), "Shared metadata selects ownership governance rather than UI bodies");
    }

    private static boolean belongs(String id, String check) {
        if (id.startsWith("RAG-DOMAIN-AI")) return check.contains(".governance.source.ai.");
        if (id.startsWith(ReviewedDomains.VALIDATOR)) return check.contains(".source.language.");
        if (id.startsWith(ReviewedDomains.UI)) return check.contains(".nativeproof.");
        if (id.startsWith(ReviewedDomains.REPORTING)) return check.contains(".reporting.") || check.startsWith("atf.android.component.");
        if (id.startsWith(ReviewedDomains.CATALOG)) return check.contains(".governance.suites.")
                || check.startsWith("atf.tests.governance.source.AtfSourceQualityGovernanceTest#");
        return id.startsWith(ReviewedDomains.SELECTION) && (check.contains(".changes.domains.")
                || check.startsWith("atf.tests.execution.components.TestChangeBatchComponentTest#"));
    }

    @Test void validatorAndEditorialDiffsExcludeUnaffectedUiAndReporting(@TempDir Path root, ComponentInputs inputs) throws Exception {
        var domains = ReviewedDomains.load(); var repo = new DomainGitFixture(root, domains);
        inputs.add("Fixture boundary", "Real committed Git diffs and production selector validation. Component symbols are nonexecuted stubs; no UI or renderer body is run.");
        repo.changes(VALIDATOR);
        var selected = repo.selected(); inputs.add("Actual validator-only control IDs", selected.toString());
        assertEquals(List.of(ReviewedDomains.VALIDATOR), selected, "Only the inventory validator requires fresh execution");
        assertEquals(domains.checks(ReviewedDomains.VALIDATOR), repo.checks(ReviewedDomains.VALIDATOR), "The admitted plan keeps the original positive and negative validator selectors");
        assertFalse(selected.contains(ReviewedDomains.UI), "Unchanged UI evidence is not scheduled again");
        assertFalse(selected.contains(ReviewedDomains.REPORTING), "Unchanged publication and heap fixtures are not scheduled again");
        repo.changes(ReviewedDomains.REGISTER);
        assertEquals(List.of(), repo.selected(), "Editorial prose alone adds no executable control");
    }

    @Test void mixedDiffsRestoreEachAffectedDomain(@TempDir Path root, ComponentInputs inputs) throws Exception {
        var repo = new DomainGitFixture(root, ReviewedDomains.load());
        inputs.add("Precondition", "Controlled selector symbols model the live domain declarations; actual UI and reporting implementations are not executed.");
        repo.changes(VALIDATOR, UI);
        var ui = repo.selected(); inputs.add("Actual validator and UI plan", ui.toString());
        assertEquals(List.of(ReviewedDomains.UI, ReviewedDomains.VALIDATOR), ui, "A real UI change cannot reuse the validator-only selection");
        repo.changes(VALIDATOR, REPORT);
        var reporting = repo.selected(); inputs.add("Actual validator and reporting plan", reporting.toString());
        assertEquals(List.of(ReviewedDomains.REPORTING, ReviewedDomains.VALIDATOR), reporting, "A renderer change restores reporting qualification without unrelated native UI");
    }

    @Test void uncoveredAndAmbiguousDomainChangesRemainBlocked(@TempDir Path root, ComponentInputs inputs) throws Exception {
        var repo = new DomainGitFixture(root, ReviewedDomains.load());
        inputs.add("Negative controls", "An unowned test and two current owners of a changed UI path must fail before test execution; no fallback to a cheaper validator is allowed.");
        java.nio.file.Files.createDirectories(root.resolve("src/test/java/unowned"));
        java.nio.file.Files.writeString(root.resolve("src/test/java/unowned/Unexpected.java"), "package unowned; class Unexpected {}\n");
        var unowned = assertThrows(IOException.class, repo::selected);
        assertTrue(unowned.getMessage().contains("TEST-CHANGE-UNCOVERED"), "Unknown readers cannot be silently skipped");
        java.nio.file.Files.delete(root.resolve("src/test/java/unowned/Unexpected.java"));
        repo.changes(UI); repo.duplicateUiOwner();
        var ambiguous = assertThrows(IOException.class, repo::selected);
        inputs.add("Original ambiguous-owner diagnostic", ambiguous.getMessage());
        assertTrue(ambiguous.getMessage().contains("TEST-CHANGE-AMBIGUOUS"), "Two eligible owners cannot select an arbitrary domain");
    }
}
