package atf.tests.governance.source;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.jvm.AtfJavaSourceInventory;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TestLayerGovernanceTest {
    @Test void executableMavenClassesHaveOneLayerAndTheGuideMatchesTheirDeclarations() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var annotations = TestLayerRules.annotationSources(root);
        var matrix = new TreeMap<String, Set<String>>();
        var violations = new ArrayList<String>();
        try (var parser = JavaTestDeclarations.open()) {
            for (var sourceRoot : AtfJavaSourceInventory.roots(root)) {
                if (!sourceRoot.test()) continue;
                String module = GovernanceProject.relative(root, sourceRoot.module());
                for (var file : AtfJavaSourceInventory.files(sourceRoot.directory())) {
                    String source = Files.readString(file);
                    for (var declaration : TestLayerRules.inspect(source, annotations, parser)) {
                        if (!declaration.executable()) continue;
                        var layers = declaration.layers();
                        if (layers.size() != 1) {
                            violations.add(file + "#" + declaration.type() + ": expected one layer, found " + layers);
                            continue;
                        }
                        matrix.computeIfAbsent(module, ignored -> new TreeSet<>()).add(layers.getFirst());
                    }
                }
            }
        }
        AuditedAssertions.assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
        AuditedAssertions.assertFalse(matrix.isEmpty());
        AuditedAssertions.assertEquals(matrix, documentedMatrix(Files.readString(root.resolve("docs/testing/ATF_TestingGuide.md"))),
                "Documented layers must be derived from live executable declarations");
    }

    @Test void theGuideCannotHideMissingDuplicateOrIncorrectLevels() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String guide = Files.readString(root.resolve("docs/testing/ATF_TestingGuide.md"));
        var original = documentedMatrix(guide);
        String row = guide.lines().filter(line -> line.startsWith("| `atf-validation`")).findFirst().orElseThrow();
        AuditedAssertions.assertNotEquals(original, documentedMatrix(guide.replace(row, "")), "A missing module changes the matrix");
        AuditedAssertions.assertNotEquals(original, documentedMatrix(guide.replace(row, row.replaceFirst("✅", "—"))),
                "A changed level changes the matrix");
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> documentedMatrix(guide.replace(row, row + "\n" + row)));
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> documentedMatrix(guide.replace(row, row.replaceFirst("✅", "yes"))));
    }

    @Test void theGuideMapsEveryGovernedSourceModuleWithoutClaimingBinarySourceSuites() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var expected = new TreeSet<String>();
        for (var source : AtfJavaSourceInventory.roots(root)) expected.add(GovernanceProject.relative(root, source.module()));
        for (var source : GovernanceProject.policy(root).path("additionalSourceRoots")) expected.add(source.path("module").asText());
        var documented = new TreeSet<String>();
        for (String row : Files.readAllLines(root.resolve("docs/testing/ATF_TestingGuide.md"))) {
            if (!row.startsWith("| `")) continue;
            String module = row.split("\\|", -1)[1].trim().replace("`", "");
            AuditedAssertions.assertTrue(documented.add(module), "Duplicate source-module owner: " + module);
        }
        AuditedAssertions.assertEquals(expected, documented, "Every governed source module has a bounded testing owner");
    }

    private static Map<String, Set<String>> documentedMatrix(String guide) {
        var block = java.util.regex.Pattern.compile("(?s)<!-- atf-maven-level-matrix -->(.*?)<!-- /atf-maven-level-matrix -->").matcher(guide);
        if (!block.find()) throw new IllegalArgumentException("Missing Maven layer matrix");
        String table = block.group(1);
        if (block.find()) throw new IllegalArgumentException("Duplicate Maven layer matrix");
        var documented = new TreeMap<String, Set<String>>();
        var columns = java.util.List.of("@AtfUnitTest", "@AtfComponentTest", "@AtfIntegrationTest", "@AtfE2eTest");
        for (String row : table.lines().toList()) {
            if (!row.startsWith("| `atf-")) continue;
            String[] cells = row.split("\\|", -1);
            if (cells.length != 8) throw new IllegalArgumentException("Invalid module matrix row: " + row);
            String module = cells[1].trim().replace("`", "");
            var layers = new TreeSet<String>();
            for (int i = 0; i < columns.size(); i++) {
                String marker = cells[i + 2].trim();
                if (!Set.of("✅", "—").contains(marker)) throw new IllegalArgumentException("Unknown layer marker: " + marker);
                if (marker.equals("✅")) layers.add(columns.get(i));
            }
            if (documented.put(module, layers) != null) throw new IllegalArgumentException("Duplicate matrix module: " + module);
        }
        return documented;
    }

    @Test void composedAnnotationsResolveCyclesAndIgnoreNestedFixtureLayers() {
        var annotations = Map.of("fixture.Outer", "import fixture.Inner;\n@Inner\npublic @interface Outer {}",
                "fixture.Inner", "@AtfComponentTest\npublic @interface Inner {}",
                "fixture.Loop", "import fixture.Loop;\n@Loop\npublic @interface Loop {}");
        AuditedAssertions.assertEquals(java.util.List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(
                "import fixture.Outer;\n@Outer\nclass Example {\n @AtfUnitTest static class Fixture {} }", annotations));
        AuditedAssertions.assertTrue(TestLayerRules.declaredLayers("import fixture.Loop;\n@Loop\nclass Example {}", annotations).isEmpty());
    }
}
