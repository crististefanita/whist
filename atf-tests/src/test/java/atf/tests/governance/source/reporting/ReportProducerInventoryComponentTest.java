package atf.tests.governance.source.reporting;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.jvm.JavaSourceParser;
import atf.tests.reporting.GameReportAudit;
import atf.tests.reporting.acceptance.CombinedAcceptanceReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Inventory prevents a new raw renderer boundary from escaping producer qualification. */
@AtfComponentTest
class ReportProducerInventoryComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();

    @Test void everyRendererBoundaryHasAnExplicitOwnerAndExistingQualification(ComponentInputs inputs) throws Exception {
        Path root = GovernanceProject.locateProjectRoot(); var sources = sources(root); var inventory = inventory(root);
        inputs.add("Producer inventory", inventory.toString());
        assertEquals(List.of(), inspect(sources, inventory), "Every raw renderer constructor is classified and every primary producer names executable qualification");
        assertEquals(5L, java.util.stream.StreamSupport.stream(inventory.path("producers").spliterator(), false)
                .filter(row -> row.path("role").asText().equals("primary")).count(), "Five active producer boundaries have independent qualification owners");
    }

    @Test void missingDuplicateOrMisclassifiedProducersAndLostContractsAreRejected(ComponentInputs inputs) throws Exception {
        Path root = GovernanceProject.locateProjectRoot(); var inventory = inventory(root);
        var only = ((com.fasterxml.jackson.databind.node.ObjectNode) inventory).putArray("producers");
        var original = inventory(root).get("producers").get(0).deepCopy(); only.add(original);
        Map<String, String> sources = Map.of(original.get("source").asText(), "class Emitter { Object emit(){ return new ExtentReportSession(null); } }",
                original.get("contracts").get(0).get("source").asText(), "class Contract {void bothRenderersRetainOutcomesInvocationsAndOriginalComparisons(){}}");
        inputs.add("Negative controls", "Drop one producer, duplicate ownership, pretend a live emitter is a fixture, remove its qualification, then introduce a new renderer constructor.");
        for (String mutation : List.of("missing", "duplicate", "fixture", "contract", "new")) {
            var changed = inventory.deepCopy(); var rows = (com.fasterxml.jackson.databind.node.ArrayNode) changed.get("producers");
            var code = new HashMap<>(sources);
            switch (mutation) {
                case "missing" -> rows.remove(0);
                case "duplicate" -> rows.add(rows.get(0).deepCopy());
                case "fixture" -> ((com.fasterxml.jackson.databind.node.ObjectNode) rows.get(0)).put("role", "fixture");
                case "contract" -> ((com.fasterxml.jackson.databind.node.ObjectNode) rows.get(0)).putArray("contracts");
                case "new" -> code.put("new/Unreviewed.java", "class Unreviewed {Object create(){return new ExtentReportSession(null);}}");
            }
            var diagnostics = inspect(code, changed); inputs.add("Refusal " + mutation, diagnostics.toString());
            assertFalse(diagnostics.isEmpty(), "Unreviewed producer metadata cannot authorize publication: " + mutation);
        }
    }

    @Test void legacyAggregateCannotQualifyMissingOriginalComparisons(ComponentInputs inputs) {
        inputs.add("Legacy boundary", "Old XML totals contain no original component comparisons; the compatibility entry must refuse qualification.");
        String previous = System.getProperty("atf.deep.publish");
        try {
            System.setProperty("atf.deep.publish", "true");
            var failure = assertThrows(IllegalStateException.class, () -> CombinedAcceptanceReport.publish(List.of("fixture")));
            assertTrue(failure.getMessage().contains("UNQUALIFIED_LEGACY_REPORT"), "The public compatibility entry identifies missing comparison evidence before reading external paths");
        } finally {
            if (previous == null) System.clearProperty("atf.deep.publish"); else System.setProperty("atf.deep.publish", previous);
        }
    }

    private static JsonNode inventory(Path root) throws Exception {
        return new ObjectMapper().readTree(root.resolve("config/report-producers.json").toFile());
    }
    private static Map<String, String> sources(Path root) throws Exception {
        var result = new HashMap<String, String>();
        for (var source : GovernanceProject.sources(root)) result.put(GovernanceProject.relative(root, source.file()), Files.readString(source.file()));
        Path browser = root.resolve("atf-ui-browser/tests/BrowserExtent.java");
        result.put(GovernanceProject.relative(root, browser), Files.readString(browser));
        return result;
    }
    private static List<String> inspect(Map<String, String> sources, JsonNode inventory) {
        var errors = new ArrayList<String>(); var actual = new HashSet<String>(); var declared = new HashSet<String>();
        var methods = new HashMap<String, Set<String>>(); var fixtures = new HashSet<String>();
        try (var parser = new JavaSourceParser()) {
            sources.forEach((path, text) -> {
                var tree = parser.parse(text); methods.put(path, new HashSet<>());
                new TreeScanner<Void, Void>() {
                    @Override public Void visitMethod(com.sun.source.tree.MethodTree node, Void unused) {
                        methods.get(path).add(node.getName().toString()); return super.visitMethod(node, unused);
                    }
                    @Override public Void visitAnnotation(com.sun.source.tree.AnnotationTree node, Void unused) {
                        if (Set.of("Test", "ParameterizedTest").contains(node.getAnnotationType().toString())) fixtures.add(path);
                        return super.visitAnnotation(node, unused);
                    }
                    @Override public Void visitNewClass(NewClassTree node, Void unused) {
                        String type = node.getIdentifier().toString();
                        if (Set.of("ExtentReportSession", "HtmlReportSession").contains(type.substring(type.lastIndexOf('.') + 1))) actual.add(path);
                        return super.visitNewClass(node, unused);
                    }
                }.scan(tree, null);
            });
        }
        if (inventory.path("schemaVersion").asInt() != 1 || !inventory.path("producers").isArray()) return List.of("Invalid producer inventory");
        for (var row : inventory.get("producers")) {
            String path = row.path("source").asText(), role = row.path("role").asText();
            if (!declared.add(path)) errors.add("Duplicate producer: " + path);
            if (!Set.of("primary", "fixture", "retired").contains(role)) errors.add("Unknown producer role: " + path);
            if (role.equals("fixture") && !fixtures.contains(path)) errors.add("Emitter is not a fixture: " + path);
            if (!role.equals("fixture") && row.path("contracts").isEmpty()) errors.add("Missing qualification: " + path);
            for (var contract : row.path("contracts")) {
                if (!methods.getOrDefault(contract.path("source").asText(), Set.of()).contains(contract.path("method").asText())) errors.add("Missing contract: " + contract);
            }
        }
        if (!actual.equals(declared)) { var missing = new HashSet<>(actual); missing.removeAll(declared); var stale = new HashSet<>(declared); stale.removeAll(actual); errors.add("Producer population mismatch: missing=" + missing + ", stale=" + stale); }
        return List.copyOf(errors);
    }
}
