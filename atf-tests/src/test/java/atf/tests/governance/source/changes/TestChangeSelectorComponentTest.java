package atf.tests.governance.source.changes;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.testchange.TestChangeAdmissionCli;
import atf.tests.testchange.TestChangeReview;
import atf.tests.testchange.selectors.StaticProofSelectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class TestChangeSelectorComponentTest {
    private static final String SELECTOR = "fixture.Proof#accepts";
    private static final String IMPORTS = "package fixture; import atf.testkit.AtfComponentTest; import org.junit.jupiter.api.*; ";
    @TempDir Path root;

    @Test @ExtendWith(GameReportAudit.class)
    void staticSourceDeclarationsAdmitWithoutLoadingApplicationClasses(ComponentInputs inputs) throws Exception {
        write("pom.xml", "<project/>");
        for (String method : List.of("@Test void accepts() {}",
                "@org.junit.jupiter.api.Test void accepts(@org.junit.jupiter.api.io.TempDir java.nio.file.Path p) {}",
                "@ProofCase void accepts() {}")) {
            String source = IMPORTS + "@AtfComponentTest class Proof { " + method + " } "
                    + "@Test @interface ProofCase {}";
            write("src/test/java/fixture/Proof.java", source);
            inputs.add("Static source declaration", source);
            AuditedAssertions.assertDoesNotThrow(() -> StaticProofSelectors.validate(root, List.of(SELECTOR)), SELECTOR);
        }
        write("src/test/java/fixture/Proof.java", IMPORTS + """
                @AtfComponentTest class Proof {
                  static { if (true) throw new IllegalStateException("Must never be loaded during source admission"); }
                  String decoy = "@TestFactory void accepts() {}";
                  // @ParameterizedTest void accepts(int value) {}
                  @Test void accepts() {}
                }
                """);
        AuditedAssertions.assertDoesNotThrow(() -> StaticProofSelectors.validate(root, List.of(SELECTOR)));
        AuditedAssertions.assertFalse(Files.exists(root.resolve("target")), "Syntax admission emits no compiled outputs");
    }

    @Test @ExtendWith(GameReportAudit.class)
    void dynamicAndAmbiguousDeclarationsFailBeforeProofExecution(ComponentInputs inputs) throws Exception {
        write("pom.xml", "<project/>");
        for (String annotation : List.of("TestFactory", "TestTemplate", "RepeatedTest(2)",
                "org.junit.jupiter.params.ParameterizedTest")) {
            String source = IMPORTS + "@AtfComponentTest class Proof { @" + annotation + " void accepts() {} }";
            write("src/test/java/fixture/Proof.java", source);
            inputs.add("Rejected dynamic declaration", source);
            refused("DYNAMIC");
        }
        write("src/test/java/fixture/Proof.java", IMPORTS
                + "@AtfComponentTest class Proof { @Dynamic void accepts() {} } @TestTemplate @interface Dynamic {}");
        refused("DYNAMIC");
        write("src/test/java/fixture/Proof.java", IMPORTS
                + "@AtfComponentTest class Proof { @Test void accepts() {} void accepts(int value) {} }");
        refused("AMBIGUOUS");
        write("src/test/java/fixture/Duplicate.java", IMPORTS + "class Proof {}");
        refused("AMBIGUOUS");
        Files.delete(root.resolve("src/test/java/fixture/Duplicate.java"));
        write("src/test/java/fixture/Proof.java", IMPORTS
                + "@AtfComponentTest class Proof { @Cycle void accepts() {} } @Cycle @interface Cycle {}");
        refused("AMBIGUOUS");
    }

    @Test void unsupportedShapesCannotBorrowAnInheritedOrMainSourceMethod() throws Exception {
        write("pom.xml", "<project/>");
        for (String method : List.of("void accepts() {}", "@Test static void accepts() {}", "@Test private void accepts() {}",
                "@Test int accepts() { return 1; }")) {
            write("src/test/java/fixture/Proof.java", IMPORTS + "@AtfComponentTest class Proof { " + method + " }");
            refused("SHAPE");
        }
        write("src/test/java/fixture/Proof.java", IMPORTS + "class Proof extends Base {} @AtfComponentTest class Base { @Test void accepts() {} }");
        refused("MISSING");
        write("src/test/java/fixture/Proof.java", IMPORTS + "class Proof { @Test void accepts() {} }");
        refused("SHAPE");
        Files.delete(root.resolve("src/test/java/fixture/Proof.java"));
        write("src/main/java/fixture/Proof.java", IMPORTS + "@AtfComponentTest class Proof { @Test void accepts() {} }");
        refused("SHAPE");
    }

    @Test void componentScopeFollowsLocalCompositionInheritanceAndNestedContainers() throws Exception {
        write("pom.xml", "<project/>");
        write("src/test/java/fixture/Proof.java", IMPORTS + """
                @ComponentScope class Base {}
                class Proof extends Base { @Test void accepts() {} }
                @AtfComponentTest @interface ComponentScope {}
                @AtfComponentTest class Container { @Nested class Child { @Test void accepts() {} } }
                """);
        AuditedAssertions.assertDoesNotThrow(() -> StaticProofSelectors.validate(root,
                List.of(SELECTOR, "fixture.Container$Child#accepts")));
    }

    @Test void importsOpaqueCompositionAndSyntaxErrorsFailClosed() throws Exception {
        write("pom.xml", "<project/>");
        write("src/test/java/a/Check.java", "package a; @org.junit.jupiter.api.Test public @interface Check {}");
        write("src/test/java/b/Check.java", "package b; @org.junit.jupiter.api.Test public @interface Check {}");
        write("src/test/java/fixture/Proof.java", IMPORTS + "import a.*; import b.*; @AtfComponentTest class Proof { @Check void accepts() {} }");
        refused("AMBIGUOUS");
        write("src/test/java/fixture/Proof.java", IMPORTS + "import external.ProofCase; @AtfComponentTest class Proof { @ProofCase void accepts() {} }");
        refused("ANNOTATION");
        write("src/test/java/fixture/Proof.java", IMPORTS + "@AtfComponentTest class Proof { @Test void accepts( }");
        refused("SOURCE");
    }

    @Test void admissionAndFinalReviewShareSelectorDiagnosticsAndRecheckChangedSource() throws Exception {
        var fixture = new TestChangeGitFixture(root);
        String control = fixture.control("STATIC-SELECTORS", "VERIFIED", fixture.base, "2026-12-21", false,
                TestChangeGitFixture.RESOURCE, "src/test/java/example/component/ValidReviewTest.java")
                .replace("    paths:", "    checks:\n      positive: ['example.component.ValidReviewTest#acceptsExactInputs']\n"
                        + "      negative: ['example.component.ValidReviewTest#rejectsStaleInput']\n    paths:");
        fixture.put(TestChangeGitFixture.BASELINE, control);
        fixture.put(TestChangeGitFixture.RESOURCE, "changed");
        fixture.commit("static candidate");
        String head = fixture.git("rev-parse", "HEAD");
        var today = LocalDate.of(2026, 11, 22);
        AuditedAssertions.assertEquals(List.of("STATIC-SELECTORS"), TestChangeReview.requiredProofs(root, fixture.base, head, today));
        Path source = root.resolve("src/test/java/example/component/ValidReviewTest.java");
        Files.writeString(source, Files.readString(source).replace("@Test void acceptsExactInputs", "@org.junit.jupiter.api.TestFactory void acceptsExactInputs"));
        fixture.commit("dynamic candidate");
        var rejected = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, fixture.base, Map.of(), today));
        AuditedAssertions.assertTrue(rejected.getMessage().startsWith("TEST-CHANGE-SELECTOR-DYNAMIC"));
        for (boolean generate : List.of(false, true)) {
            var output = new ByteArrayOutputStream();
            String[] args = generate ? new String[]{"--root", root.toString(), "--base", fixture.base, "--generate"}
                    : new String[]{"--root", root.toString(), "--base", fixture.base};
            AuditedAssertions.assertEquals(2, TestChangeAdmissionCli.run(args, Map.of(), today, new PrintStream(output)));
            var response = new ObjectMapper().readTree(output.toByteArray());
            AuditedAssertions.assertEquals("BLOCKED", response.path("status").asText());
            AuditedAssertions.assertEquals(rejected.getMessage(), response.path("diagnostics").get(0).asText());
        }
    }

    private void refused(String diagnostic) {
        var error = AuditedAssertions.assertThrows(IOException.class, () -> StaticProofSelectors.validate(root, List.of(SELECTOR)));
        AuditedAssertions.assertTrue(error.getMessage().startsWith("TEST-CHANGE-SELECTOR-" + diagnostic), error.getMessage());
        AuditedAssertions.assertTrue(error.getMessage().contains(SELECTOR), "Every rejection identifies its selector");
    }

    private void write(String relative, String source) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
