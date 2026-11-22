package atf.tests.governance.source.changes;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import atf.tests.testchange.TestChangeAdmissionCli;
import atf.tests.testchange.TestChangeProof;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/** Real Git histories; receipt and report are explicitly mocked verification-boundary inputs. */
@AtfComponentTest
class TestChangeEditorialProofComponentTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);
    @TempDir Path scratch;

    @Test @ExtendWith(GameReportAudit.class)
    void editorialDescendantsPreserveOriginalProofBytesAndExposeProvenance(ComponentInputs inputs) throws Exception {
        var f = fixture();
        byte[] receipt = Files.readAllBytes(f.directory().resolve(TestChangeProof.RECEIPT));
        byte[] report = Files.readAllBytes(f.directory().resolve(TestChangeProof.REPORT));
        for (String file : List.of("README.md", "docs/feedback/ATF_Notes.md", "module/src/test/README.md", "config/documentation-editions.json")) {
            put(f.source(), file, file.endsWith(".json") ? "{\"schemaVersion\":2}" : "# Editorial guidance\n");
            git(f, "add", "-A"); git(f, "commit", "-qm", "editorial continuation");
            var result = verify(f);
            inputs.add("Original and admitted proof provenance", new ObjectMapper().writeValueAsString(result.continuations()));
            AuditedAssertions.assertEquals(List.of(), result.diagnostics());
            AuditedAssertions.assertEquals(1, result.continuations().size());
            var continuation = result.continuations().getFirst();
            AuditedAssertions.assertEquals(f.original(), continuation.originalCommit());
            AuditedAssertions.assertEquals(git(f, "rev-parse", "HEAD"), continuation.admittedCommit());
            AuditedAssertions.assertTrue(continuation.editorialPaths().contains(file));
            AuditedAssertions.assertTrue(continuation.protectedDiffSha256().matches("[a-f0-9]{64}"));
            AuditedAssertions.assertTrue(Arrays.equals(receipt, Files.readAllBytes(f.directory().resolve(TestChangeProof.RECEIPT))), "Original receipt bytes are immutable");
            AuditedAssertions.assertTrue(Arrays.equals(report, Files.readAllBytes(f.directory().resolve(TestChangeProof.REPORT))), "Original Extent bytes are immutable");
        }
        var output = new ByteArrayOutputStream();
        AuditedAssertions.assertEquals(0, TestChangeAdmissionCli.run(new String[]{"--root", f.source().toString(), "--base", f.base()},
                Map.of(TestChangeProof.ROOT_ENV, f.directory().getParent().toString()), TODAY, new PrintStream(output)));
        var admission = new ObjectMapper().readTree(output.toByteArray());
        AuditedAssertions.assertEquals(f.original(), admission.path("continuations").get(0).path("originalCommit").asText());
        AuditedAssertions.assertEquals("PASS", admission.path("status").asText());
    }

    @Test @ExtendWith(GameReportAudit.class)
    void executableInputsControlsAndRevertedCodeCannotReuseEditorialProofs(ComponentInputs inputs) throws Exception {
        var f = fixture();
        String selected = Files.readString(f.source().resolve(TestChangeAdmissionFixture.SELECTOR_SOURCE));
        String control = Files.readString(f.source().resolve(TestChangeGitFixture.BASELINE));
        for (String defect : List.of("source", "unprotected-source", "resource", "root-resource", "rules", "policy", "control-reason", "control-command", "reverted-source")) {
            switch (defect) {
                case "source", "reverted-source" -> put(f.source(), TestChangeAdmissionFixture.SELECTOR_SOURCE,
                        selected.replace("void accepts() { }", "void accepts() { int changed = 1; }"));
                case "unprotected-source" -> put(f.source(), "src/main/java/Changed.java", "class Changed {}\n");
                case "resource" -> put(f.source(), "module/src/test/resources/README.md", "# Executable fixture\n");
                case "root-resource" -> put(f.source(), "src/test/resources/README.md", "# Executable fixture\n");
                case "rules" -> put(f.source(), "docs/game/ATF_RulesEn.md", "# Changed packaged rules\n");
                case "policy" -> put(f.source(), "config/runtime.json", "{\"changed\":true}");
                case "control-reason" -> put(f.source(), TestChangeGitFixture.BASELINE, control.replace("Isolate source", "Independently validate source"));
                case "control-command" -> put(f.source(), TestChangeGitFixture.BASELINE, control.replace("mvn -pl", "mvn -o -pl"));
                default -> throw new AssertionError(defect);
            }
            git(f, "add", "-A"); git(f, "commit", "-qm", defect);
            if (defect.equals("reverted-source")) {
                put(f.source(), TestChangeAdmissionFixture.SELECTOR_SOURCE, selected);
                git(f, "add", "-A"); git(f, "commit", "-qm", "revert executable input");
            }
            inputs.add("Rejected continuation defect", defect);
            refused(f);
            git(f, "reset", "--hard", f.original());
        }
    }

    @Test void unrelatedAndMergedHistoriesDoNotAcquireOriginalProofProvenance() throws Exception {
        var f = fixture();
        git(f, "checkout", "-qb", "divergent", f.original() + "^");
        put(f.source(), "README.md", "# Different ancestry\n");
        git(f, "add", "-A"); git(f, "commit", "-qm", "divergent editorial history");
        git(f, "cherry-pick", f.original());
        refused(f);
        git(f, "checkout", "-q", "main");
        git(f, "checkout", "-qb", "side");
        put(f.source(), "docs/side.md", "# Side guidance\n");
        git(f, "add", "-A"); git(f, "commit", "-qm", "side editorial commit");
        git(f, "checkout", "-q", "main");
        put(f.source(), "README.md", "# Main guidance\n");
        git(f, "add", "-A"); git(f, "commit", "-qm", "main editorial commit");
        git(f, "merge", "--no-ff", "--no-edit", "side");
        refused(f);
    }

    @Test void dirtyInputsExecutableModesAndTamperedOriginalEvidenceRemainBlocked() throws Exception {
        var f = fixture();
        put(f.source(), "README.md", "# Dirty editorial input\n");
        refused(f);
        git(f, "add", "-A"); git(f, "commit", "-qm", "clean editorial continuation");
        AuditedAssertions.assertEquals(1, verify(f).continuations().size());
        git(f, "update-index", "--chmod=+x", "README.md");
        git(f, "commit", "-qm", "executable document mode");
        refused(f);
        git(f, "reset", "--hard", f.original());
        put(f.source(), "README.md", "# Clean descendant\n");
        git(f, "add", "-A"); git(f, "commit", "-qm", "clean descendant");
        Path receipt = f.directory().resolve(TestChangeProof.RECEIPT);
        byte[] original = Files.readAllBytes(receipt);
        var json = new ObjectMapper();
        var modified = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(original);
        ((com.fasterxml.jackson.databind.node.ObjectNode) modified.get("identity")).put("diffSha256", "a".repeat(64));
        json.writeValue(receipt.toFile(), modified);
        refused(f);
        Files.write(receipt, original);
        Files.writeString(f.directory().resolve(TestChangeProof.REPORT), "altered original report");
        refused(f);
    }

    private Fixture fixture() throws Exception {
        Path root = Files.createDirectory(scratch.resolve("checkout"));
        put(root, "pom.xml", "<project/>");
        String base = TestChangeAdmissionFixture.prepareReview(root);
        Path directory = TestChangeAdmissionFixture.verifiedProof(root, base, scratch.resolve("proofs"), TODAY.plusDays(1));
        return new Fixture(root, base, directory, TestChangeAdmissionFixture.git(root, "rev-parse", "HEAD"));
    }

    private static TestChangeReview.Result verify(Fixture f) throws Exception {
        return TestChangeReview.verify(f.source(), f.base(), Map.of(TestChangeProof.ROOT_ENV, f.directory().getParent().toString()), TODAY);
    }

    private static void refused(Fixture f) throws Exception {
        try { AuditedAssertions.assertFalse(verify(f).diagnostics().isEmpty(), "Changed review cannot reuse original proof"); }
        catch (IOException rejected) { AuditedAssertions.assertTrue(rejected.getMessage().startsWith("TEST-CHANGE-"), rejected.getMessage()); }
    }

    private static String git(Fixture f, String... args) throws Exception { return TestChangeAdmissionFixture.git(f.source(), args); }
    private static void put(Path root, String file, String text) throws IOException {
        Path target = root.resolve(file); Files.createDirectories(target.getParent()); Files.writeString(target, text);
    }
    private record Fixture(Path source, String base, Path directory, String original) { }
}
