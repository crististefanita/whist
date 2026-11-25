package atf.tests.governance.source.changes;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.testchange.TestChangeReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static atf.tests.governance.source.changes.TestChangeGitFixture.BASELINE;
import static atf.tests.governance.source.changes.TestChangeGitFixture.FEEDBACK;
import static atf.tests.governance.source.changes.TestChangeGitFixture.IGNORED;
import static atf.tests.governance.source.changes.TestChangeGitFixture.RESOURCE;
import static atf.tests.governance.source.changes.TestChangeGitFixture.TEST;

@AtfComponentTest
class TestChangeReviewComponentTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final String VALID = TODAY.toString();
    private static final String OLD = TODAY.minusDays(1).toString();
    private static final String NEW_TEST = "nested/module/src/test/java/demo/Test nou ș.java";
    private static final String GATE = "config/atf-test-suites.json";

    @Test void theSharedCompilerWorkspaceContractRequiresAReviewedControl(@TempDir Path root) throws Exception {
        var repo=new TestChangeGitFixture(root);String path="scripts/AtfComponentWorkspace.ps1";
        repo.put(path,"// controlled workspace contract\n");
        AuditedAssertions.assertEquals(List.of(uncovered("A",path,repo.base)),check(repo).diagnostics(),"A shared compiler admission contract cannot bypass change review");
        repo.put(FEEDBACK,repo.control("WORKSPACE-CONTEXT","ACTIVE",repo.base,VALID,false,path));
        AuditedAssertions.assertEquals(List.of(),check(repo).diagnostics(),"The exact reviewed path admits this controlled change");
    }

    @Test void sourceReadmesRemainEditorialAcrossGitStates(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        String changed = "nested/module/src/test/java/demo/README.md";
        String deleted = "whist-lab-app/app/src/test/README.md";
        repo.put(changed, "# Original guide\n");
        repo.put(deleted, "# Removed guide\n");
        repo.commit("prepare source guides");
        String base = repo.git("rev-parse", "HEAD");
        repo.put("atf-tests/src/test/README.md", "# Added guide\n");
        repo.put("src/test/README.md", "# Root guide\n");
        repo.put("atf-ui-browser/tests/README.md", "# Browser guide\n");
        repo.put(changed, "# Revised guide\n");
        repo.git("add", changed);
        repo.git("rm", deleted);
        var result = TestChangeReview.inspect(root, base, Map.of(), TODAY);
        AuditedAssertions.assertEquals(0, result.protectedPaths(), "Source guides select editorial governance, not test-code controls");
        AuditedAssertions.assertEquals(List.of(), result.diagnostics());
    }

    @Test void markdownFixturesAndSelectionContractsRemainProtected(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var paths = List.of("src/test/resources/README.md", "module/src/test/resources/case.md", "module/src/androidTest/assets/case.md",
                "module/src/testFixtures/resources/case.md", "module/src/test/java/OtherTest.java",
                "config/atf-component-collections.json", "atf-tests/tools/atf-component-ownership.mjs",
                "config/atf-component-dependencies.json", "atf-tests/tools/atf-component-dependencies.mjs",
                "atf-tests/tools/atf-dependency-references.mjs", "atf-tests/tools/atf-script-references.mjs",
                "atf-tests/tools/atf-dependency-cli.mjs", "atf-tests/tools/component-source-check.mjs",
                "scripts/AtfComponentSelection.ps1",
                "atf-ui-browser/tests/flow-budget.mjs", "atf-ui-browser/tests/flow-budget.test.mjs",
                "atf-ui-browser/build.gradle", "module/src/unitTest/java/UnitTest.java");
        for (String path : paths) repo.put(path, "controlled payload\n");
        AuditedAssertions.assertEquals(paths.stream().sorted().map(path -> uncovered("A", path, repo.base)).toList(),
                check(repo).diagnostics(), "Fixture payloads, source code and selector contracts still require review");
    }

    @Test void nativeSourceAdmissionAndAdapterOraclesCannotBypassReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var paths = List.of("atf-tests/tools/android-e2e-provenance.mjs", "atf-tests/tools/android-e2e-evidence.mjs",
                "atf-tests/tools/source-input-identity.mjs",
                "atf-tests/tools/prepare-android-e2e.mjs", "atf-tests/tools/apply-android-e2e.mjs",
                "atf-ui-browser/tests/ro/cardtable/browser/WhistSurfaceContract.java",
                "atf-ui-browser/tests/whist-surface-contract.test.mjs",
                "atf-ui-browser/tests/shared-presentation.test.mjs", "atf-ui-browser/tests/trick-playback.mjs",
                "atf-ui-browser/tests/fixtures.mjs");
        for (String path : paths) repo.put(path, "// controlled native source boundary\n");
        AuditedAssertions.assertEquals(paths.stream().sorted().map(path -> uncovered("A", path, repo.base)).toList(), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("NATIVE-SOURCE", "ACTIVE", repo.base, VALID, false, paths.toArray(String[]::new)));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void userRecoveryAndEnglishVocabularyRequireReviewedControls(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var paths = List.of("scripts/AtfAndroidUserOwnership.ps1", "scripts/Invoke-AtfAndroidDisposableUser.ps1",
                "atf-tests/src/main/java/atf/tests/documentation/policy/DocumentationTerminologyRules.java",
                "atf-tests/src/main/java/atf/tests/documentation/DocumentationGovernance.java");
        for (String path : paths) repo.put(path, "// controlled boundary\n");
        AuditedAssertions.assertEquals(paths.stream().sorted().map(path -> uncovered("A", path, repo.base)).toList(),
                check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("RECOVERY-DOC", "ACTIVE", repo.base, VALID, false, paths.toArray(String[]::new)));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void visualScopeProofsCannotChangeWithoutReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var tools = List.of("atf-tests/tools/visual-change-contracts.mjs", "atf-tests/tools/visual-contract-sources.mjs");
        for (String tool : tools) repo.put(tool, "// controlled visual selection proof\n");
        AuditedAssertions.assertEquals(tools.stream().map(tool -> uncovered("A", tool, repo.base)).toList(), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("VISUAL-SCOPE", "ACTIVE", repo.base, VALID, false, tools.toArray(String[]::new)));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void publicationSealAndReconstructionRequireReviewedControls(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var tools = List.of("atf-tests/tools/collect-github-release-retention.mjs",
                "atf-tests/tools/github-retention-seal.mjs", "atf-tests/tools/verify-release-git-snapshot.mjs");
        for (String tool : tools) repo.put(tool, "// controlled publication boundary\n");
        AuditedAssertions.assertEquals(tools.stream().map(tool -> uncovered("A", tool, repo.base)).toList(), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("PUBLICATION-SEAL", "ACTIVE", repo.base, VALID, false, tools.toArray(String[]::new)));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void theSigningExportPolicyRequiresAnExplicitReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        String policy = "scripts/AtfSigningSourcePolicy.ps1";
        repo.put(policy, "function Test-AtfPrivateSigningInput { $true }\n");
        AuditedAssertions.assertEquals(List.of(uncovered("A", policy, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("SIGNING-EXPORT", "ACTIVE", repo.base, VALID, false, policy));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void contentDigestsCannotBypassSourceChangeReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        String path = "scripts/AtfFileDigest.ps1";
        repo.put(path, "# controlled content and copy boundary\n");
        AuditedAssertions.assertEquals(List.of(uncovered("A", path, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("CONTENT-DIGEST", "ACTIVE", repo.base, VALID, false, path));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void readerSealsAndCatalogContextCannotBypassSourceChangeReview(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        for (String path : List.of("config/atf-component-reader-seals.json", "atf-tests/tools/component-qualification-context.mjs")) {
            repo.put(path, "controlled selection and provenance boundary\n");
        }
        AuditedAssertions.assertEquals(2, check(repo).diagnostics().size(), "Each new selection boundary requires explicit review");
        repo.put(FEEDBACK, repo.control("READER-SEALS", "ACTIVE", repo.base, VALID, false,
                "config/atf-component-reader-seals.json", "atf-tests/tools/component-qualification-context.mjs"));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics(), "Both boundaries are covered by the reviewed Git-diff control");
    }

    @Test void theComponentJvmProfileCannotChangeWithoutAReviewedControl(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var launchers = List.of("scripts/AtfComponentJvm.ps1", "scripts/AtfStandaloneJava.ps1", "scripts/AtfTestChangeAdmission.ps1");
        for (String launcher : launchers) repo.put(launcher, "# controlled JVM admission boundary\n");
        AuditedAssertions.assertEquals(launchers.stream().map(path -> uncovered("A", path, repo.base)).toList(), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("JVM-PROFILE", "ACTIVE", repo.base, VALID, false, launchers.toArray(String[]::new)));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void fullDiffSeesCommittedStagedUnstagedUntrackedAndIgnoredTestPaths(@TempDir Path folder) throws Exception {
        var repo = new TestChangeGitFixture(Files.createDirectory(folder.resolve("checkout de probă")));
        repo.put("pom.xml", "<project><name>committed change</name></project>\n");
        repo.commit("committed build edit");
        repo.git("mv", TEST, NEW_TEST);
        repo.put(RESOURCE, "{\"changed\":true}\n");
        repo.put(IGNORED, "ignored source\n");
        String fixture = "whist-lab-app/test-audit/src/testFixtures/resources/replay.json";
        repo.put(fixture, "{}\n");
        String unit = "whist-lab-app/card-table-core/src/unitTest/java/UnitTest.java";
        repo.put(unit, "class UnitTest {}\n");
        repo.put("docs/guide.md", "# Editorial change\n");
        repo.put("target/generated/src/test/java/Debug.java", "generated output\n");
        var missing = check(repo);
        AuditedAssertions.assertEquals(7, missing.protectedPaths(), missing.diagnostics().toString());
        AuditedAssertions.assertEquals(List.of(
                uncovered("A", NEW_TEST, repo.base), uncovered("A", IGNORED, repo.base),
                uncovered("A", unit, repo.base), uncovered("A", fixture, repo.base), uncovered("D", TEST, repo.base),
                uncovered("M", RESOURCE, repo.base), uncovered("M", "pom.xml", repo.base)), missing.diagnostics());
        repo.put(FEEDBACK, repo.control("REVIEW-DIFF", "ACTIVE", repo.base, VALID, true,
                "pom.xml", TEST, NEW_TEST, RESOURCE, IGNORED, fixture, unit));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics(), "An exact control covers every Git change");
    }

    @Test void reviewRefMustExistAndBeAnAncestorAndCiNeedsAnEarlierBase(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        var missing = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, "e".repeat(40), Map.of(), TODAY));
        AuditedAssertions.assertTrue(missing.getMessage().contains("TEST-CHANGE-BASE: cannot resolve commit"));
        var noCiBase = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, "", Map.of("CI", "true"), TODAY));
        AuditedAssertions.assertTrue(noCiBase.getMessage().contains("CI requires"));
        var sameAsHead = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, repo.base, Map.of("CI", "true"), TODAY));
        AuditedAssertions.assertTrue(sameAsHead.getMessage().contains("earlier than HEAD"));

        repo.put("README.md", "# Reviewed branch\n");
        repo.commit("main");
        repo.git("switch", "-q", "--detach", repo.base);
        repo.put("README.md", "# Side branch\n");
        repo.commit("side");
        String side = repo.git("rev-parse", "HEAD");
        repo.git("switch", "-q", "main");
        var unrelated = AuditedAssertions.assertThrows(IOException.class,
                () -> TestChangeReview.inspect(root, side, Map.of(), TODAY));
        AuditedAssertions.assertTrue(unrelated.getMessage().contains("not an ancestor"));
        AuditedAssertions.assertEquals(repo.base, TestChangeReview.inspect(root, null,
                Map.of("CI", "true", TestChangeReview.BASE_ENV, repo.base), TODAY).base());
    }

    @Test void currentDayIsInclusiveButExpiredClosedAndWrongBaseCannotCoverAGate(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.put(GATE, "{}\n");
        repo.put(FEEDBACK, repo.control("CLOSED-GATE", "CLOSED", repo.base, VALID, false, GATE)
                + repo.control("WRONG-BASE", "ACTIVE", "e".repeat(40), VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of(uncovered("A", GATE, repo.base)), check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("EXPIRED-GATE", "ACTIVE", repo.base, OLD, false, GATE));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-EXPIRED: A " + GATE + " [EXPIRED-GATE]"),
                check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("ACTIVE-GATE", "ACTIVE", repo.base, VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
        repo.put(BASELINE, repo.control("VERIFIED-GATE", "VERIFIED", repo.base, VALID, false, GATE));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-AMBIGUOUS: A " + GATE + " [ACTIVE-GATE, VERIFIED-GATE]"),
                check(repo).diagnostics());
        Files.delete(root.resolve(FEEDBACK));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void stagedDeletionCannotBeHiddenByRestoringTheWorkingFile(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.git("rm", "-q", TEST);
        repo.put(TEST, "package demo;\nclass Old {}\n");
        repo.put(FEEDBACK, repo.control("REVIEW-DELETE", "ACTIVE", repo.base, VALID, false, TEST));
        AuditedAssertions.assertEquals(List.of("TEST-CHANGE-DELETE: D " + TEST + " needs allowDelete: true"),
                check(repo).diagnostics());
        repo.put(FEEDBACK, repo.control("REVIEW-DELETE", "ACTIVE", repo.base, VALID, true, TEST));
        AuditedAssertions.assertEquals(List.of(), check(repo).diagnostics());
    }

    @Test void rejectsMalformedMisplacedRepeatedAndIgnoredControlDocuments(@TempDir Path root) throws Exception {
        var repo = new TestChangeGitFixture(root);
        repo.put(GATE, "{}\n");
        String valid = repo.control("REVIEW-SCHEMA", "ACTIVE", repo.base, VALID, false, GATE);
        repo.put("docs/feedback/ignored-owner.md", valid);
        AuditedAssertions.assertEquals(List.of(uncovered("A", GATE, repo.base)), check(repo).diagnostics());
        for (String invalid : List.of(valid.replace("```\n", ""),
                valid.replace("    allowDelete:", "    state: TEMP\n    allowDelete:"),
                valid.replace("    status: ACTIVE", "    status: VERIFIED"),
                valid.replace(GATE, "config/*.json"), valid.replace(GATE, "config/../pom.xml"))) {
            repo.put(FEEDBACK, invalid);
            var rejected = AuditedAssertions.assertThrows(IOException.class, () -> check(repo));
            AuditedAssertions.assertTrue(rejected.getMessage().contains("TEST-CHANGE-SCHEMA"), rejected.getMessage());
        }
        repo.put(FEEDBACK, valid);
        repo.put(BASELINE, valid.replace("status: ACTIVE", "status: VERIFIED"));
        var duplicate = AuditedAssertions.assertThrows(IOException.class, () -> check(repo));
        AuditedAssertions.assertTrue(duplicate.getMessage().contains("TEST-CHANGE-ID: duplicate REVIEW-SCHEMA"));
    }

    private static TestChangeReview.Result check(TestChangeGitFixture repo) throws Exception {
        return TestChangeReview.inspect(repo.root, repo.base, Map.of(), TODAY);
    }

    private static String uncovered(String kind, String path, String base) {
        return "TEST-CHANGE-UNCOVERED: " + kind + " " + path + " (base " + base + ")";
    }
}
