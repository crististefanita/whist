package atf.tests.governance.source.gradle;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.jvm.JavaSourceParser;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Source ownership and coverage gates for the JUnit 4 fixture variant; no reporting-library exception. */
@AtfComponentTest
public final class GradleAuditGovernanceTest {
    private static final Pattern JUNIT4_TEST_IMPORT = Pattern.compile("\\bimport\\s+org\\.junit\\.(?:Test|\\*)\\s*;");

    @Test void everyGradleTestClassHasOneExplicitLayerAndAnAuditedBase() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var found = new TreeMap<String, String>();
        var sources = new TreeMap<String, String>();
        for (var file : GovernanceProject.sources(root)) {
            if (!file.test() || !file.module().startsWith("whist-lab-app/")) continue;
            String name = file.file().getFileName().toString();
            if (!name.endsWith("Test.java")) continue;
            String source = Files.readString(file.file());
            if (!usesJUnit4Test(source)) continue;
            var declaration = java.util.regex.Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(source);
            assertTrue(declaration.find(), "Each test has an explicit package: " + file.file());
            String owner = declaration.group(1) + "." + name.substring(0, name.length() - 5);
            boolean isolatedUnit = root.relativize(file.file()).toString().replace('\\', '/').contains("/src/unitTest/");
            assertTrue(isolatedUnit || source.matches("(?s).*\\bextends\\s+(?:atf\\.gradle\\.audit\\.)?GradleAuditedTest\\b.*"),
                    "Every component must capture inputs and assertions; isolated unit tests use the declared unit source set: " + owner);
            found.put(owner, isolatedUnit ? "unit" : "audited");
            sources.put(owner, source);
        }
        assertFalse(found.isEmpty(), "The Gradle source perimeter cannot be empty");
        var catalogRows = Files.readAllLines(root.resolve("config/gradle-audit-collections.properties"));
        assertEquals(List.of(), gradleOwnershipViolations(found.keySet(), catalogRows),
                "Every class has one known layer, without missing, duplicate or obsolete owners");
        found.forEach((owner, layer) -> {
            String declaredLayer = catalogRows.stream().filter(row -> row.startsWith(owner + "="))
                    .map(row -> row.substring(row.indexOf('=') + 1)).findFirst().orElse(null);
            assertEquals(List.of(), unitBoundaryViolations(layer.equals("unit"), sources.get(owner), declaredLayer),
                    "Source-set and reporting boundaries for " + owner);
        });
    }

    @Test void unitDeclarationsCannotHideAnAuditedComponentOrReportingDependency() {
        assertEquals(List.of("unit outside isolated source set"),
                unitBoundaryViolations(false, "class Case extends GradleAuditedTest {}", "unit"));
        assertEquals(List.of("isolated source must declare unit"),
                unitBoundaryViolations(true, "import org.junit.Test; class Case {}", "component"));
        for (String source : List.of("import static atf.gradle.audit.assertions.GradleAssertions.*;",
                "class Case extends GradleAuditedTest {}", "class Case { void check() { atf.gradle.audit.GradleInputs.fixture(\"data\"); } }",
                "import atf.reporting.extent.session.ExtentReportSession;")) {
            assertEquals(List.of("isolated unit uses component reporting"), unitBoundaryViolations(true, source, "unit"), source);
        }
    }

    @Test void rawJunitUnitsAndAuditedComponentsRetainTheirDeclaredBoundaries() {
        assertEquals(List.of(), unitBoundaryViolations(true, "import org.junit.Test; import static org.junit.Assert.*;", "unit"));
        assertEquals(List.of(), unitBoundaryViolations(true,
                "// atf.reporting is absent\nclass Case { String message = \"GradleAuditedTest and atf.gradle.audit are unavailable\"; }", "unit"));
        assertEquals(List.of(), unitBoundaryViolations(false, "class Case extends GradleAuditedTest {}", "component"));
        assertEquals(List.of(), unitBoundaryViolations(false, "class Case extends GradleAuditedTest {}", "governance"));
    }

    private static List<String> unitBoundaryViolations(boolean isolated, String source, String declared) {
        var violations = new java.util.ArrayList<String>();
        if (!isolated && "unit".equals(declared)) violations.add("unit outside isolated source set");
        if (isolated && !"unit".equals(declared)) violations.add("isolated source must declare unit");
        if (isolated && usesComponentReporting(source))
            violations.add("isolated unit uses component reporting");
        return violations;
    }

    private static boolean usesComponentReporting(String source) {
        boolean[] found = {false};
        try (var parser = new JavaSourceParser()) {
            new TreeScanner<Void, Void>() {
                @Override public Void visitMemberSelect(MemberSelectTree node, Void ignored) {
                    String name = node.toString();
                    if (name.startsWith("atf.gradle.audit.") || name.startsWith("atf.reporting.")) found[0] = true;
                    return super.visitMemberSelect(node, ignored);
                }
                @Override public Void visitIdentifier(IdentifierTree node, Void ignored) {
                    if (node.getName().contentEquals("GradleAuditedTest")) found[0] = true;
                    return super.visitIdentifier(node, ignored);
                }
            }.scan(parser.parse(source), null);
        }
        return found[0];
    }
    @Test void inventoryRecognizesExplicitAndWildcardJUnit4ImportsWithoutTreatingJupiterAsJUnit4() {
        assertTrue(usesJUnit4Test("import org.junit.Test; class Explicit {}"));
        assertTrue(usesJUnit4Test("import org.junit.*; class Wildcard {}"));
        assertFalse(usesJUnit4Test("import org.junit.jupiter.api.Test; class Jupiter {}"));
    }

    @Test void gradleInventoryRejectsMissingDuplicateAndUnknownLayerOwners() {
        var live = java.util.Set.of("alpha.ComponentTest", "beta.UnitTest");
        assertEquals(List.of(), gradleOwnershipViolations(live,
                List.of("alpha.ComponentTest=component", "beta.UnitTest=unit")));
        assertEquals(List.of("missing Gradle owner: beta.UnitTest"), gradleOwnershipViolations(live,
                List.of("alpha.ComponentTest=component")));
        assertEquals(List.of("duplicate Gradle owner: alpha.ComponentTest"), gradleOwnershipViolations(live,
                List.of("alpha.ComponentTest=component", "alpha.ComponentTest=component", "beta.UnitTest=unit")));
        assertEquals(List.of("unknown Gradle layer: alpha.ComponentTest=integration"), gradleOwnershipViolations(live,
                List.of("alpha.ComponentTest=integration", "beta.UnitTest=unit")));
        assertEquals(List.of("unknown Gradle owner: obsolete.Test"), gradleOwnershipViolations(live,
                List.of("alpha.ComponentTest=component", "beta.UnitTest=unit", "obsolete.Test=unit")));
    }

    private static List<String> gradleOwnershipViolations(java.util.Set<String> live, List<String> rows) {
        var owners = new java.util.LinkedHashMap<String, String>();
        var violations = new java.util.ArrayList<String>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] pair = row.split("=", -1);
            if (pair.length != 2 || pair[0].isBlank()) {
                violations.add("invalid Gradle owner row: " + row);
                continue;
            }
            if (owners.putIfAbsent(pair[0], pair[1]) != null) {
                violations.add("duplicate Gradle owner: " + pair[0]);
                continue;
            }
            if (!List.of("unit", "component", "governance", "learning").contains(pair[1]))
                violations.add("unknown Gradle layer: " + row);
        }
        for (String owner : live) if (!owners.containsKey(owner)) violations.add("missing Gradle owner: " + owner);
        for (String owner : owners.keySet()) if (!live.contains(owner)) violations.add("unknown Gradle owner: " + owner);
        return List.copyOf(violations);
    }

    private static boolean usesJUnit4Test(String source) {
        return JUNIT4_TEST_IMPORT.matcher(source).find();
    }
    @Test void productTestsDoNotKeepRawJUnitAssertionsBesideTheAuditFacade() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var violations = new java.util.ArrayList<String>();
        for (var file : GovernanceProject.sources(root)) {
            if (!file.test() || !file.module().startsWith("whist-lab-app/") || file.module().endsWith("/test-audit")) continue;
            String source = Files.readString(file.file());
            String relative = root.relativize(file.file()).toString().replace('\\', '/');
            if (!relative.contains("/src/unitTest/") && source.contains("org.junit.Assert")) violations.add(relative);
        }
        assertEquals(List.of(), violations, "Helpers and parametrized cases use the same evaluated-assertion capture");
    }
    @Test void auditDependenciesRemainTestOnlyAndTheInventoryGateIsMandatory() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String bridge = Files.readString(root.resolve("whist-lab-app/gradle/component-audit.gradle"));
        assertTrue(bridge.contains("dependencies.add('testImplementation'"), "The fixture variant is attached only to test compilation/runtime");
        assertFalse(bridge.contains("dependencies.add('implementation'"), "Android runtime variants cannot consume reporting or JUnit fixtures");
        assertTrue(bridge.contains("GradleReportVerifier"), "Every passing task must reconcile original JUnit with actual HTML");
        assertTrue(bridge.contains("reports.html.required = false"), "Gradle's duplicate presentation is disabled");
        assertTrue(bridge.contains("maxParallelForks = 1"), "The shared report owner does not claim unqualified parallel execution");
    }
    @Test void thePureJvmEntryUsesTheSameAuditWithoutConfiguringAndroid() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String settings = Files.readString(root.resolve("whist-lab-app/algorithm-build/settings.gradle"));
        String build = Files.readString(root.resolve("whist-lab-app/algorithm-build/build.gradle"));
        assertTrue(settings.contains("include(\":test-audit\")"), "The JVM-only reactor resolves the same test fixture variant");
        assertTrue(build.contains("component-audit.gradle"), "There is one audit configuration for both Gradle entrypoints");
        assertFalse(settings.contains("com.android"), "Offline algorithm tests do not configure the Android plugin");
    }

    @Test void coreBatchesHaveOneDeclaredBudgetAndSeparatePolicyExperiments() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String bridge = Files.readString(root.resolve("whist-lab-app/gradle/component-audit.gradle"));
        String core = Files.readString(root.resolve("whist-lab-app/game-core/build.gradle"));
        assertEquals(List.of(), coreBatchViolations(bridge, core), "Shared worker budget and isolated policy experiments");
    }

    @Test void coreBatchPolicyDetectsTheOldHeapOverrideAndEarlyExit() {
        String shared = "maxHeapSize = '512m'\nmaxParallelForks = 1\nfailFast = true";
        String core = "tasks.withType(Test).configureEach { maxHeapSize = \"256m\" }";
        assertEquals(List.of("keep later invocations", "keep the shared heap", "select policy experiments separately"),
                coreBatchViolations(shared, core), "Report every violation before running the offline batch");
    }

    private static List<String> coreBatchViolations(String bridge, String core) {
        var violations = new java.util.ArrayList<String>();
        if (!Pattern.compile("\\bmaxHeapSize\\s*=\\s*'512m'").matcher(bridge).find())
            violations.add("declare the worker heap");
        if (!Pattern.compile("\\bfailFast\\s*=\\s*false\\b").matcher(bridge).find())
            violations.add("keep later invocations");
        if (Pattern.compile("\\bmaxHeapSize\\b").matcher(core).find()) violations.add("keep the shared heap");
        if (!core.contains("exclude \"**/OfflineStrategyProfileMatchTest.class\"")
                || !core.contains("exclude \"**/OfflinePerspectiveMatchTest.class\""))
            violations.add("select policy experiments separately");
        return violations;
    }

    @Test void everyApplicationPackageHasExactlyOneNonemptyPrimaryCollection() throws Exception {
        var root = GovernanceProject.locateProjectRoot(); var mapping = new TreeMap<String, String>();
        for (String line : Files.readAllLines(root.resolve("config/gradle-app-collections.properties"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] pair = line.split("=", -1);
            assertEquals(2, pair.length, "One package has one primary collection");
            assertNull(mapping.put(pair[0], pair[1]), "Duplicate application package ownership");
        }
        var found = new java.util.TreeSet<String>();
        for (var file : GovernanceProject.sources(root)) {
            String path = root.relativize(file.file()).toString().replace('\\', '/');
            if (!path.startsWith("whist-lab-app/app/src/test/java/ro/whistlab/app/") || !path.endsWith("Test.java")) continue;
            found.add(path.substring("whist-lab-app/app/src/test/java/ro/whistlab/app/".length()).split("/")[0]);
        }
        assertEquals(found, mapping.keySet(), "New or obsolete package ownership cannot silently omit or duplicate tests");
        var collections = new java.util.TreeSet<>(mapping.values());
        assertFalse(collections.isEmpty(), "The application collection catalog cannot be empty");
        assertTrue(collections.stream().allMatch(collection -> collection.matches("[a-z][a-z0-9-]*")),
                "Every primary collection has a valid stable identifier");
        String build = Files.readString(root.resolve("whist-lab-app/gradle/app-component-collections.gradle"));
        assertTrue(build.contains("enabled = false") && build.contains("dependsOn collections"), "The original task delegates and does not execute a duplicate population");
        assertTrue(build.contains("if (collection == 'history')") && build.contains("files(base.testClassesDirs, rasterClasses)")
                && build.contains("files(rasterClasses, base.classpath)"), "History owns both state and rendered JVM components");
        assertTrue(build.contains("tasks.named('historyRasterTest', Test).configure")
                && build.contains("dependsOn 'testDebugHistoryComponents'"), "Raster compatibility must delegate to its primary owner");
    }
}
