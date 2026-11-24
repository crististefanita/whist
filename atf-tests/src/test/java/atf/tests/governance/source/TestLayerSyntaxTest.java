package atf.tests.governance.source;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TestLayerSyntaxTest {
    @Test @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
    void syntaxInspectionCostUsesOneFrozenInventoryAndPreservesAstAcrossOptions(
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        var root = atf.tests.governance.support.GovernanceProject.locateProjectRoot();
        var inventory = ParserCostProbe.freeze(root);
        var report = ParserCostProbe.measure(inventory, 3);
        inputs.add("Paired parser experiment", "Declared Maven Java source roots; UTF-8 sources frozen once; configured=-proc:none with empty class/processor paths; reference=JDK default options; one warm-up per mode; three alternating measured pairs; no type analysis or speed threshold");
        inputs.add("Original parser observations", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(report));
        AuditedAssertions.assertFalse(inventory.isEmpty(), "The comparison must inspect actual source files");
        AuditedAssertions.assertEquals(6, report.trials().size(), "Three pairs retain six independent measurements");
        AuditedAssertions.assertEquals(64, report.inventorySha256().length(), "Frozen source inventory is fingerprinted");
        String expected = report.trials().getFirst().astSha256();
        for (var trial : report.trials()) {
            AuditedAssertions.assertEquals(inventory.size(), trial.units(), "Every trial inspects the same population");
            AuditedAssertions.assertEquals(expected, trial.astSha256(), "Compiler paths and processing options do not alter the parsed trees");
            AuditedAssertions.assertTrue(trial.parseNanos() >= 0 && trial.totalNanos() >= trial.parseNanos(),
                    "Parse cost is distinguished from ownership, normalization and hashing");
        }
    }

    @Test void parserCostProbeRejectsAmbiguousInputsAndMalformedSources() throws Exception {
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> ParserCostProbe.measure(List.of(), 1));
        var source = new ParserCostProbe.Input("fixture.java", "class Fixture {}");
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> ParserCostProbe.measure(List.of(source, source), 1));
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> ParserCostProbe.measure(List.of(source), 0));
        AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> ParserCostProbe.trial(List.of(source), "unknown", 0));
        for (String mode : List.of("configured", "jdk-default-paths"))
            AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> ParserCostProbe.trial(
                    List.of(new ParserCostProbe.Input("broken.java", "class Incomplete {")), mode, 0));
    }

    @Test void parserOwnershipSurvivesASyntaxErrorAndEndsAtClose() {
        var parser = JavaTestDeclarations.open();
        try (parser) {
            AuditedAssertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("class Incomplete {"));
            AuditedAssertions.assertEquals("Healthy", parser.parse("class Healthy {}").types().getFirst().name());
        }
        parser.close();
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> parser.parse("class AfterClose {}"));
    }

    @Test void everyTopLevelExecutableKeepsItsOwnLayer() {
        var classes = TestLayerRules.inspect("class Helper {} @AtfUnitTest class Actual { @Test void caseName() {} }", Map.of());
        AuditedAssertions.assertEquals(2, classes.size());
        AuditedAssertions.assertFalse(classes.getFirst().executable());
        AuditedAssertions.assertTrue(classes.get(1).executable());
        AuditedAssertions.assertEquals(List.of("@AtfUnitTest"), classes.get(1).layers());
    }

    @Test void wildcardImportsResolveOnlyAnUnambiguousComposedAnnotation() {
        var definitions = Map.of("alpha.Mark", "@AtfComponentTest @interface Mark {}",
                "beta.Mark", "@AtfUnitTest @interface Mark {}");
        AuditedAssertions.assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers("import alpha.*; @Mark class Example {}", definitions));
        AuditedAssertions.assertThrows(IllegalArgumentException.class,
                () -> TestLayerRules.declaredLayers("import alpha.*; import beta.*; @Mark class Example {}", definitions));
    }

    @Test void qualifiedAndInlineMethodAnnotationsIdentifyAnExecutableClass() {
        for (String annotation : List.of("@org.junit.jupiter.api.Test", "@org.junit.jupiter.params.ParameterizedTest", "@Test")) {
            String source = "@AtfComponentTest class Example { " + annotation + " void caseName() {} }";
            AuditedAssertions.assertTrue(TestLayerRules.isConcreteExecutable(source), annotation);
            AuditedAssertions.assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(source, Map.of()));
        }
    }

    @Test void annotationsBetweenModifiersBelongToTheDeclaredType() {
        String source = "public @AtfComponentTest final class Example { @Test void caseName() {} }";
        AuditedAssertions.assertTrue(TestLayerRules.isConcreteExecutable(source));
        AuditedAssertions.assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(source, Map.of()));
    }

    @Test void fixtureMethodsCannotTurnAnEnclosingHelperIntoAnExecutable() {
        String helper = """
                class Helper {
                    @AtfE2eTest static class Fixture {
                        @Test void controlledCase() {}
                    }
                }
                """;
        AuditedAssertions.assertFalse(TestLayerRules.isConcreteExecutable(helper));
        AuditedAssertions.assertEquals(List.of(), TestLayerRules.declaredLayers(helper, Map.of()));
        String container = """
                @AtfComponentTest class Container {
                    @Nested class Child {
                        @Test void actualCase() {}
                    }
                }
                """;
        AuditedAssertions.assertTrue(TestLayerRules.isConcreteExecutable(container));
        AuditedAssertions.assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(container, Map.of()));
    }

    @Test void anIdentifierContainingInterfaceIsStillAConcreteClass() {
        AuditedAssertions.assertTrue(TestLayerRules.isConcreteExecutable("class interfaceProbe {\n @Test void actualCase() {}\n}"));
        AuditedAssertions.assertFalse(TestLayerRules.isConcreteExecutable("abstract class Example {\n @Test void actualCase() {}\n}"));
    }

    @Test void syntaxErrorsCannotBecomeSuccessfulEmptyDiscovery() {
        AuditedAssertions.assertThrows(IllegalArgumentException.class,
                () -> TestLayerRules.isConcreteExecutable("class Example { @Test void incomplete("));
    }

    @Test void annotationDiscoveryIgnoresExamplesAndKeepsTheRealDeclaration(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path source = root.resolve("src/test/java/fixture/Real.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package fixture;
                @AtfComponentTest public @interface Real {}
                class Documentation {
                    String example = \"\"\"
                        @AtfE2eTest
                        public @interface Phantom {}
                        \"\"\";
                }
                """);
        var annotations = TestLayerRules.annotationSources(root);
        AuditedAssertions.assertEquals(Set.of("fixture.Real"), annotations.keySet());
        AuditedAssertions.assertEquals(List.of("@AtfComponentTest"), TestLayerRules.declaredLayers(
                "import fixture.Real; @Real class Example {}", annotations));
    }
}
