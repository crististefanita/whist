package atf.tests.governance.suites;

import atf.testkit.AtfComponentTest;
import atf.android.evidence.component.ComponentInputs;
import atf.tests.reporting.GameReportAudit;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.source.TestLayerRules;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

/** Source ownership and executable selection must agree before independent receipts can be reused. */
@AtfComponentTest
class AtfCollectionOwnershipComponentTest {
    @Test void realDependencyClosuresSelectOnlyTheirConsumers() throws Exception {
        String output = contracts("atf-component-dependencies.test.mjs", "dependency graph selects|shared dependencies|changed import|classifier|live dependency");
        AuditedAssertions.assertTrue(output.contains("dependency graph selects actual Java, Node and PowerShell consumers"),
                "Imports, package peers, process inputs and script providers retain every transitive consumer");
        AuditedAssertions.assertTrue(output.contains("classifier path labels and nested Java type names"),
                "A test-data path and an unrelated nested type do not become executable dependencies");
        AuditedAssertions.assertTrue(output.contains("live dependency graph admits the complete catalog"),
                "The real source population and reader contracts admit the expected documentation-rule owners");
    }

    @Test void missingOrUnreviewedDependenciesCannotAuthorizeReuse() throws Exception {
        String output = contracts("atf-component-dependencies.test.mjs", "unknown|missing|opaque|generated");
        AuditedAssertions.assertTrue(output.contains("unknown, unowned, historical and missing populations fail closed"),
                "Missing graph admission never reinterprets original collection evidence");
        AuditedAssertions.assertTrue(output.contains("new opaque readers require topology admission"),
                "New reader roles require topology admission and body seals before selective reuse");
        AuditedAssertions.assertTrue(output.contains("generated module providers retain their producer inputs"),
                "A missing provider is rejected unless the sealed loader declares its producer scope");
    }

    @Test @ExtendWith(GameReportAudit.class)
    void reviewedReaderSealsSelectOnlyTheirConsumersAndPreserveOriginalReports(ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "Two independent reader consumers share the same admitted topology; only reviewed body seals change. Original source/report receipts remain immutable.");
        String output = auditedContracts("atf-reader-seals.test.mjs", "reviewed seal-only|simultaneous|seal inventory observers", inputs);
        AuditedAssertions.assertTrue(output.contains("reviewed seal-only updates retain topology"),
                "A reviewed seal update selects the changed reader's transitive consumers only");
        output = auditedContracts("atf-component-ownership.test.mjs", "reviewed reader updates", inputs);
        AuditedAssertions.assertTrue(output.contains("originalReportBytesPreserved"),
                "Release planning and reuse preserve every unaffected original report, JUnit identity and receipt byte");
    }

    @Test @ExtendWith(GameReportAudit.class)
    void topologyMissingSealsAndUnknownReaderScopesCannotNarrowQualification(ComponentInputs inputs) throws Exception {
        inputs.add("Negative controls", "Alter topology, observer scope, provider population, seal hashes, historical admission and reader ownership independently.");
        String output = auditedContracts("atf-reader-seals.test.mjs", "changed executable|unreviewed|missing historical", inputs);
        AuditedAssertions.assertTrue(output.contains("cannot masquerade as a seal-only update"),
                "Changed dependency topology and declared scopes retain complete qualification");
        AuditedAssertions.assertTrue(output.contains("missing stale malformed or foreign reader seals fail closed"),
                "Unadmitted, absent, stale, malformed and foreign seals never authorize reuse");
        AuditedAssertions.assertTrue(output.contains("unknown consumers and unexplained seal changes"),
                "An unknown consumer or unexplained seal update keeps every owner selected");
    }

    private static String auditedContracts(String script, String pattern, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", pattern,
                root.resolve("atf-tests/tools").resolve(script).toString()), root, Duration.ofSeconds(45),
                output -> inputs.add("Captured assertions: " + script, output));
    }

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
        return contracts("atf-component-ownership.test.mjs", pattern);
    }

    private static String contracts(String script, String pattern) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return runNode(List.of("node", "--test", "--test-name-pattern", pattern,
                root.resolve("atf-tests/tools").resolve(script).toString()), root, Duration.ofSeconds(45));
    }
}
