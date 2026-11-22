package atf.tests.governance.suites;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.source.TestLayerRules;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

/** Source ownership and executable selection must agree before independent receipts can be reused. */
@AtfComponentTest
class AtfCollectionOwnershipComponentTest {
    @Test void positiveInventoriesPartitionEveryAtfComponentWithoutChangingLeafNames() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var mapper = new ObjectMapper();
        var owners = mapper.readTree(Files.readString(root.resolve("config/atf-component-collections.json")));
        var catalog = mapper.readTree(Files.readString(root.resolve("config/atf-test-suites.json")));
        var sources = TestLayerRules.executableSources(root);
        var known = new HashMap<String, String>();
        sources.forEach(s -> known.put(s.name(), s.source()));
        var expected = new TreeSet<String>();
        sources.stream().filter(s -> s.module().equals("atf-tests") && !s.suite()
                && s.layers().equals(List.of("@AtfComponentTest"))).forEach(s -> expected.add(s.name()));
        var observed = new TreeSet<String>();
        var groups = new TreeSet<String>();
        for (var owner : owners.required("collections")) {
            String group = owner.required("group").asText(), suite = owner.required("suite").asText();
            AuditedAssertions.assertTrue(groups.add(group), "Distinct component group: " + group);
            var rows = new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
            catalog.required("suites").forEach(row -> { if (row.path("suite").asText().equals(suite)) rows.add(row); });
            AuditedAssertions.assertEquals(1, rows.size(), "One primary descriptor per collection");
            AuditedAssertions.assertEquals(group, rows.getFirst().path("group").asText());
            AuditedAssertions.assertEquals("primary", rows.getFirst().path("role").asText());
            var declared = new TreeSet<String>();
            owner.required("classes").forEach(c -> declared.add(c.asText()));
            var gate = new SuiteOwnershipRules.Gate("atf-tests", suite, "primary", "component", List.of(),
                    TestLayerRules.suiteSelectors(known.get(suite), known));
            var selected = new TreeSet<String>();
            for (String test : expected) if (gate.includes(new SuiteOwnershipRules.TestSource("atf-tests", test, "component"))) selected.add(test);
            AuditedAssertions.assertEquals(declared, selected, "Executable selector matches its owner: " + group);
            for (String test : declared) AuditedAssertions.assertTrue(observed.add(test), "No overlapping primary class: " + test);
        }
        AuditedAssertions.assertEquals(expected, observed, "Every original leaf remains owned exactly once");
    }

    @Test void documentationChangesReuseIndependentOriginalReceipts() throws Exception {
        String output = contracts("documentation|unchanged|payload");
        AuditedAssertions.assertTrue(output.contains("documentation-only selection"), "The planner selects editorial checks only");
        AuditedAssertions.assertTrue(output.contains("unchanged receipts preserve"), "Reuse keeps original report, JUnit IDs and hash bytes");
    }

    @Test void sharedUnknownAndHistoricalInputsCannotHideAffectedCollections() throws Exception {
        String output = contracts("shared|unknown|transition|forged");
        AuditedAssertions.assertTrue(output.contains("transition"), "Legacy monolithic evidence cannot become partition evidence");
        AuditedAssertions.assertTrue(output.contains("forged"), "Wrong suites and changed evidence remain rejected");
    }

    private static String contracts(String pattern) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", pattern,
                root.resolve("atf-tests/tools/atf-component-ownership.test.mjs").toString()), root, Duration.ofSeconds(45));
    }
}
