package atf.tests.governance.documentation.release.rag.scope;

import atf.android.process.CommandProcess;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.governance.source.changes.TestChangeAdmissionFixture;
import atf.tests.governance.documentation.release.rag.scope.launcher.LauncherFailureLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class QualificationLauncherComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String VERSION = "1.2.3";
    private static final String APK = "WhistLab-" + VERSION + ".apk";
    private static final String RELEASE = "release/" + VERSION + "/";
    private static final String GOVERNANCE = "run-atf-governance.ps1";
    private static final String COMPONENTS = "run-component-suites.ps1";

    @TempDir Path scratch;
    private String reviewBase;
    private Path parserCache;
    private Path proofRoot;
    private Path launcherScripts;
    private boolean realAdmission;
    private boolean realEditorial;

    @Test @org.junit.jupiter.api.extension.ExtendWith(atf.tests.reporting.GameReportAudit.class)
    void metadataDefectsStopBothLaunchersBeforeCopyCacheOrMaven(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Both actual entry points, real metadata admission and the prepared canonical editorial CLI. Compiler and source-proof neighbours are independently controlled; Maven is an unentered local probe.");
        realEditorial = true;
        Path source = fixture();
        Path manifest = source.resolve(RELEASE + "release-validation.json");
        String original = Files.readString(manifest);
        Path guide = source.resolve("release/README.md");
        String originalGuide = Files.readString(guide);
        for (String defect : List.of("inventory", "editorial")) {
            Files.writeString(manifest, original);
            Files.writeString(guide, originalGuide);
            if (defect.equals("inventory")) {
                var changed = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(original);
                changed.withObject("/qualification").withObject("/e2eGroups").putObject("unselected-flow");
                JSON.writeValue(manifest.toFile(), changed);
            } else Files.writeString(guide, originalGuide.replace("## Overview", "## Pe scurt"));
            for (String runner : List.of(GOVERNANCE, COMPONENTS)) {
                Path output = scratch.resolve(defect + "-" + runner);
                var run = launch(runner, source, output, "", null);
                inputs.add("Observed " + runner + " / " + defect, run.log());
                AuditedAssertions.assertEquals("MOCK_TEST_CHANGE_ADMISSION", JSON.readTree(output.resolve("test-change-admission.json").toFile()).path("kind").asText(),
                        "The neighbouring source-proof boundary is explicitly controlled, not claimed as a fresh proof");
                AuditedAssertions.assertEquals(1, run.exit(), run.log());
                AuditedAssertions.assertTrue(run.log().contains(defect.equals("inventory") ? "RELEASE-METADATA-E2E" : "RELEASE-METADATA-EDITORIAL"), run.log());
                AuditedAssertions.assertFalse(Files.exists(output.resolve("source-inputs.json")), "Metadata rejection precedes source inventory and copying");
                AuditedAssertions.assertFalse(Files.exists(output.resolve("copy-cache.log")) || Files.exists(output.resolve("copy-maven-cache.log")), "No dependency cache is prepared");
                AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "A metadata diagnostic cannot run a primary suite");
                if (defect.equals("editorial")) {
                    String diagnostic = Files.readString(output.resolve("release-editorial.json"));
                    inputs.add("Canonical editorial diagnostic", diagnostic);
                    inputs.add("Prepared canonical CLI command", Files.readString(scratch.resolve("prepared-validator-calls.jsonl")));
                    AuditedAssertions.assertTrue(diagnostic.contains("DOC-"), "The canonical editorial diagnostics are retained");
                }
            }
        }
    }

    @Test void validTestChangeProofPrecedesSourceCopyAndMavenInBothLaunchers() throws Exception {
        realAdmission = true;
        Path source = fixture();
        proofRoot = scratch.resolve("mocked proof inputs");
        TestChangeAdmissionFixture.verifiedProof(source, reviewBase, proofRoot,
                java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        var governance = launch(GOVERNANCE, source, scratch.resolve("admitted-governance"), "", null);
        AuditedAssertions.assertEquals(0, governance.exit(), governance.log());
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("maven.entered")), "Maven observes successful second admission");
        Files.delete(scratch.resolve("maven.entered"));
        Path components = scratch.resolve("admitted-components");
        var component = launch(COMPONENTS, source, components, "component-boundary", null);
        AuditedAssertions.assertEquals(1, component.exit());
        AuditedAssertions.assertTrue(component.log().contains("Groups must"), component.log());
        AuditedAssertions.assertTrue(Files.isRegularFile(components.resolve("source-inputs.json")), "Valid proof permits the real snapshot");
        AuditedAssertions.assertEquals("PASS", JSON.readTree(components.resolve("test-change-admission-recheck.json").toFile())
                .path("status").asText());
        AuditedAssertions.assertEquals("ATF_TEST_CHANGE_ADMISSION", JSON.readTree(
                components.resolve("test-change-admission.json").toFile()).path("kind").asText(),
                "Admission integration must use the canonical validator, not the neighbouring-component mock");
        AuditedAssertions.assertEquals("MOCK_EDITORIAL_NEIGHBOUR", JSON.readTree(
                components.resolve("release-editorial.json").toFile()).path("kind").asText(),
                "Editorial acceptance is independently qualified by the metadata component");
        AuditedAssertions.assertEquals("MOCK_ARTIFACT_NEIGHBOUR", JSON.readTree(
                components.resolve("release-preflight.log").toFile()).path("kind").asText(),
                "Payload integrity has its own components; this fixture owns source admission and real snapshot ordering");
        AuditedAssertions.assertTrue(Files.readString(scratch.resolve("prepared-validator-calls.jsonl"))
                .contains("atf.tests.testchange.TestChangeAdmissionCli"), "The controlled compiler dispatches the prepared canonical source validator");
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "The deliberate invalid group stops after admission");
    }

    @Test void invalidTestChangeProofStopsBothLaunchersBeforeCopyOrMaven() throws Exception {
        realAdmission = true;
        Path source = fixture();
        proofRoot = scratch.resolve("mocked proof inputs");
        Path proof = TestChangeAdmissionFixture.verifiedProof(source, reviewBase, proofRoot,
                java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        String base = reviewBase;
        // Receipt corruption is covered in-process against final governance; these launches prove ordering at the process boundary.
        for (String defect : List.of("missing-root", "missing-base")) {
            proofRoot = defect.equals("missing-root") ? null : proof.getParent();
            reviewBase = defect.equals("missing-base") ? "f".repeat(40) : base;
            String diagnostic = defect.equals("missing-base") ? "TEST-CHANGE-BASE" : "TEST-CHANGE-PROOF-MISSING";
            for (String runner : List.of(GOVERNANCE, COMPONENTS)) {
                Path output = scratch.resolve(defect + "-" + runner);
                var result = launch(runner, source, output, "", null);
                AuditedAssertions.assertEquals(1, result.exit(), result.log());
                AuditedAssertions.assertTrue(result.log().contains(diagnostic), result.log());
                AuditedAssertions.assertFalse(Files.exists(output.resolve("source-inputs.json")), "Rejected proof stops before source copying");
                AuditedAssertions.assertFalse(Files.exists(output.resolve("copy-maven-cache.log"))
                        || Files.exists(output.resolve("copy-cache.log")), "Rejected proof stops before dependency cache copying");
                AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "Rejected proof stops before Maven");
                var phases = JSON.readTree(output.resolve("timing.json").toFile()).path("stages");
                AuditedAssertions.assertEquals(1, phases.size(), "Only the cheap admission phase may run");
                AuditedAssertions.assertEquals("FAIL", phases.get(0).path("status").asText());
            }
        }
    }

    @Test void unsupportedProofSelectorsStopBothLaunchersBeforeCopyCacheOrMaven() throws Exception {
        realAdmission = true;
        Path source = fixture();
        proofRoot = scratch.resolve("mocked proof inputs");
        TestChangeAdmissionFixture.verifiedProof(source, reviewBase, proofRoot,
                java.time.LocalDate.now(java.time.ZoneOffset.UTC));
        String original = Files.readString(source.resolve(TestChangeAdmissionFixture.SELECTOR_SOURCE));
        for (String diagnostic : List.of("DYNAMIC", "AMBIGUOUS")) {
            TestChangeAdmissionFixture.replaceSelectorSource(source, diagnostic.equals("DYNAMIC")
                    ? original.replace("@Test void accepts", "@org.junit.jupiter.api.TestFactory void accepts")
                    : original.replace("@Test void accepts() { }", "@Test void accepts() { } void accepts(int value) { }"));
            for (String runner : List.of(GOVERNANCE, COMPONENTS)) {
                Path output = scratch.resolve(diagnostic + "-" + runner);
                var result = launch(runner, source, output, "", null);
                AuditedAssertions.assertEquals(1, result.exit(), result.log());
                AuditedAssertions.assertTrue(result.log().contains("TEST-CHANGE-SELECTOR-" + diagnostic), result.log());
                AuditedAssertions.assertFalse(Files.exists(output.resolve("source-inputs.json")), "No source snapshot for a rejected selector");
                AuditedAssertions.assertFalse(Files.exists(output.resolve("copy-maven-cache.log"))
                        || Files.exists(output.resolve("copy-cache.log")), "No dependency cache copying for a rejected selector");
                AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "No Maven for a rejected selector");
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
        "run-atf-governance.ps1, missing-apk, ENOENT",
        "run-atf-governance.ps1, corrupt-apk, APK_BYTES_OR_HASH",
        "run-component-suites.ps1, corrupt-checksum, APK_CHECKSUM",
        "run-component-suites.ps1, wrong-sidecar-size, RELEASE-PREFLIGHT-INVENTORY",
        "run-atf-governance.ps1, wrong-apk-name, RELEASE-PREFLIGHT-IDENTITY"
    })
    void anInvalidArtifactStopsTheRealLauncherBeforeMaven(String runner, String defect, String diagnostic) throws Exception {
        Path source = fixture();
        Path apk = source.resolve(RELEASE + APK);
        Path checksum = source.resolve(RELEASE + APK + ".sha256");
        Path manifest = source.resolve(RELEASE + "release-validation.json");
        switch (defect) {
            case "missing-apk" -> Files.delete(apk);
            case "corrupt-apk" -> Files.writeString(apk, "altered");
            case "corrupt-checksum" -> Files.writeString(checksum, "a".repeat(64) + "  " + APK);
            case "wrong-sidecar-size" -> Files.writeString(checksum, Files.readString(checksum) + "\n");
            case "wrong-apk-name" -> Files.writeString(manifest, Files.readString(manifest).replace(APK, "Other.apk"));
            default -> throw new AssertionError(defect);
        }
        Path output = scratch.resolve("blocked");
        var run = launch(runner, source, output, "", null);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(run.log().contains("RELEASE-PREFLIGHT") && run.log().contains(diagnostic), run.log());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "Maven must never start for invalid release bytes");
    }

    @ParameterizedTest
    @ValueSource(strings = {GOVERNANCE, COMPONENTS})
    void aBuildEditAfterPlanningStopsBothLaunchers(String runner) throws Exception {
        Path source = fixture();
        Path pinned = scratch.resolve("plan-inputs.json");
        Path pom = source.resolve("pom.xml");
        // Capture the genuine launcher inventory so the only difference is one build input.
        var output = scratch.resolve("inventory");
        AuditedAssertions.assertEquals(0, launch(GOVERNANCE, source, output, "", null).exit());
        Files.copy(output.resolve("source-inputs.json"), pinned);
        Files.delete(scratch.resolve("maven.entered"));
        Files.writeString(pom, "<project><name>changed after pinning</name></project>");
        var run = launch(runner, source, scratch.resolve("changed"), "", pinned);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(run.log().contains("QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN"), run.log());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "A stale plan must stop before Maven");
    }

    @Test void governancePassesWithAnIsolatedRootAndThePinnedInputManifest() throws Exception {
        Path source = fixture(), output = scratch.resolve("healthy");
        Path sibling = Files.createDirectory(source.resolveSibling(source.getFileName() + "-tmp"));
        var run = launch(GOVERNANCE, source, output, "", null, sibling, sibling);
        AuditedAssertions.assertEquals(0, run.exit(), run.log());
        JsonNode result = JSON.readTree(output.resolve("governance-result.json").toFile());
        AuditedAssertions.assertEquals("-Dtest=AtfQualityGovernanceSuite",
                Files.readString(scratch.resolve("maven-test-selector")));
        Path stage = Path.of(result.required("stage").asText());
        AuditedAssertions.assertEquals(stage.toString(), Files.readString(scratch.resolve("maven.entered")));
        String expectedMavenTemp = stage.getParent().resolve("maven-temp").toString();
        AuditedAssertions.assertEquals(List.of(expectedMavenTemp, expectedMavenTemp),
                Files.readAllLines(scratch.resolve("maven-temp-roots")),
                "The isolated Maven/Surefire JVM must keep its temporary files beside the staged workspace.");
        AuditedAssertions.assertTrue(stage.startsWith(sibling.toRealPath()), "A sibling with the same prefix is external");
        AuditedAssertions.assertTrue(Files.isRegularFile(stage.resolve(RELEASE + APK)));
        AuditedAssertions.assertEquals(hash(output.resolve("release-inputs.json")), result.required("releaseFingerprint").asText());
        AuditedAssertions.assertEquals("PASS", JSON.readTree(output.resolve("release-preflight.log").toFile()).required("status").asText());
        AuditedAssertions.assertEquals(0, launch(GOVERNANCE, source, scratch.resolve("matching"), "",
                output.resolve("source-inputs.json")).exit());
    }

    @Test void aMissingConfiguredEvidenceRootIsReadyBeforeTheMavenChildStarts() throws Exception {
        Path source = fixture();
        Path evidenceRoot = scratch.resolve("missing external evidence").resolve("nested");
        Path output = scratch.resolve("evidence-root-launch");
        AuditedAssertions.assertFalse(Files.exists(evidenceRoot));

        var run = launch(GOVERNANCE, source, output, "", null, scratch, scratch, null, evidenceRoot);

        AuditedAssertions.assertEquals(0, run.exit(), run.log());
        AuditedAssertions.assertTrue(Files.isDirectory(evidenceRoot), "The configured root is prepared before Maven starts");
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("maven.entered")), "The mocked Maven child ran");
        AuditedAssertions.assertEquals("PASS", JSON.readTree(output.resolve("governance-result.json").toFile())
                .required("status").asText());
    }

    @Test void anEvidenceRootInsideTheCheckoutStopsBeforeMavenOrStaging() throws Exception {
        Path source = fixture();
        Path evidenceRoot = source.resolve("generated-evidence");
        var run = launch(GOVERNANCE, source, scratch.resolve("blocked-evidence-root"), "", null,
                scratch, scratch, null, evidenceRoot);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(run.log().contains("QUALIFICATION-EVIDENCE-IN-CHECKOUT"), run.log());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "Rejected roots stop before Maven");
        AuditedAssertions.assertFalse(Files.exists(evidenceRoot), "Rejected roots are not created");
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("blocked-evidence-root")), "No output run is staged");
    }

    @Test void governanceLauncherAllowsTheBoundedRagSuiteAndRejectsUnknownSelectorsBeforeStaging() throws Exception {
        Path source = fixture();
        var selected = launch(GOVERNANCE, source, scratch.resolve("rag-only"), "", null, scratch, scratch,
                "AtfRagGovernanceSuite");
        AuditedAssertions.assertEquals(0, selected.exit(), selected.log());
        AuditedAssertions.assertEquals("-Dtest=AtfRagGovernanceSuite",
                Files.readString(scratch.resolve("maven-test-selector")));
        Path reportManifest = scratch.resolve("rag-only/governance-report-manifest.json");
        JsonNode extent = JSON.readTree(reportManifest.toFile());
        AuditedAssertions.assertEquals("PASS", extent.required("status").asText());
        AuditedAssertions.assertEquals("ATF_EXTENT_COMPONENT_SUITE", extent.required("kind").asText());
        AuditedAssertions.assertEquals(hash(scratch.resolve("rag-only/component-extent.html")),
                extent.required("report").required("sha256").asText());
        AuditedAssertions.assertTrue(Files.readString(scratch.resolve("rag-only/component-extent.html"))
                .contains("data:image/png;base64,"));

        Files.delete(scratch.resolve("maven.entered"));
        Files.delete(scratch.resolve("maven-test-selector"));
        var missingExtent = launch(GOVERNANCE, source, scratch.resolve("rag-no-extent"), "no-extent", null,
                scratch, scratch, "AtfRagGovernanceSuite");
        AuditedAssertions.assertEquals(1, missingExtent.exit());
        JsonNode rejectedExtent = JSON.readTree(scratch.resolve("rag-no-extent/governance-report-manifest.json").toFile());
        AuditedAssertions.assertEquals("FAIL", rejectedExtent.required("status").asText());
        AuditedAssertions.assertTrue(rejectedExtent.required("diagnostics").toString().contains("EXPECTED_ONE_EXTENT_REPORT"));

        Files.delete(scratch.resolve("maven.entered"));
        Files.delete(scratch.resolve("maven-test-selector"));
        var rejected = launch(GOVERNANCE, source, scratch.resolve("invalid-suite"), "", null, scratch, scratch,
                "ArbitraryTestSelector");
        AuditedAssertions.assertEquals(1, rejected.exit());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")),
                "An unrecognized selector must fail before Maven or staging.");
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("invalid-suite")),
                "An invalid selector must not create an output run.");
    }

    @Test void checkoutTmpStopsBothLaunchersBeforeStagingOrMaven() throws Exception {
        Path source = fixture();
        Path nestedSource = Files.createDirectories(source.resolve("target/tmp"));
        Path parentCheckout = Files.createDirectory(scratch.resolve("parent checkout"));
        Files.createDirectory(parentCheckout.resolve(".git"));
        Path nestedCheckout = Files.createDirectories(parentCheckout.resolve("target/tmp"));
        expectTempBlock(source, nestedSource, scratch, nestedSource, "TMP");
        expectTempBlock(source, nestedCheckout, scratch, nestedCheckout, "TMP");
    }

    @Test void aJunctionInTempCannotHideAGitWorktree() throws Exception {
        Path source = fixture();
        Path worktree = Files.createDirectory(scratch.resolve("worktree"));
        Files.writeString(worktree.resolve(".git"), "gitdir: ../checkout/.git/worktrees/fixture\n");
        Path directory = Files.createDirectories(worktree.resolve("cache/tmp"));
        Path link = scratch.resolve("external alias");
        var junction = CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-Command",
                "$ErrorActionPreference='Stop'; New-Item -ItemType Junction -Path $env:ATF_FIXTURE_LINK "
                        + "-Target $env:ATF_FIXTURE_TARGET | Out-Null"),
                Map.of("ATF_FIXTURE_LINK", link.toString(), "ATF_FIXTURE_TARGET", directory.getParent().toString()),
                Duration.ofSeconds(30), false);
        AuditedAssertions.assertEquals(0, junction.exitCode(), junction.stderr());
        try {
            expectTempBlock(source, scratch, link.resolve("tmp"), directory, "TEMP");
        } finally {
            Files.delete(link);
        }
    }

    private void expectTempBlock(Path source, Path tmp, Path temp, Path physical, String variable) throws Exception {
        for (String runner : List.of(GOVERNANCE, COMPONENTS)) {
            Path output = scratch.resolve(runner + "-blocked-" + variable);
            var run = launch(runner, source, output, "", null, tmp, temp);
            AuditedAssertions.assertEquals(1, run.exit(), run.log());
            AuditedAssertions.assertTrue(run.log().contains("QUALIFICATION-TEMP-IN-CHECKOUT:")
                    && run.log().contains(variable + "="), run.log());
            AuditedAssertions.assertFalse(Files.exists(output), "Preflight must stop before creating the run directory");
            try (var entries = Files.list(physical)) {
                AuditedAssertions.assertTrue(entries.findAny().isEmpty(), "Preflight must stop before staging");
            }
            AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "Preflight must stop before Maven");
        }
    }

    @ParameterizedTest
    @CsvSource({"live-add, LIVE-RELEASE: release/added.json", "live-delete, LIVE-RELEASE: release/index.json",
            "stage-change, STAGED-RELEASE: release/index.json"})
    void driftInReleaseEvidenceInvalidatesAnOtherwisePassingRun(String mutation, String diagnostic) throws Exception {
        Path source = fixture(), output = scratch.resolve("drift");
        var run = launch(GOVERNANCE, source, output, mutation, null);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        JsonNode result = JSON.readTree(output.resolve("governance-result.json").toFile());
        AuditedAssertions.assertEquals(0, result.required("testExit").asInt());
        AuditedAssertions.assertEquals("FAIL", result.required("status").asText());
        AuditedAssertions.assertTrue(result.required("snapshotDifferences").toString().contains(diagnostic));
    }

    private Path fixture() throws Exception {
        Path source = Files.createDirectory(scratch.resolve("surse fixate"));
        write(source, "config/atf-governance-policy.json", "{\"documentationLanguage\":\"en\",\"mavenModules\":[],\"additionalSourceRoots\":[]}");
        write(source, "pom.xml", "<project/>");
        Path real = GovernanceProject.locateProjectRoot();
        Files.copy(real.resolve("config/release-platform-rag-policy.json"), source.resolve("config/release-platform-rag-policy.json"));
        Files.copy(real.resolve("config/gradle-app-collections.properties"), source.resolve("config/gradle-app-collections.properties"));
        Files.copy(real.resolve("config/atf-component-collections.json"), source.resolve("config/atf-component-collections.json"));
        Files.copy(real.resolve("config/atf-test-suites.json"), source.resolve("config/atf-test-suites.json"));
        if (realAdmission || realEditorial) {
            // Admission fixtures inspect a small external checkout; artifact qualification is a controlled neighbour.
            write(source, "atf-tests/tools/check-release-artifacts.mjs",
                    "console.log(JSON.stringify({status:'PASS',kind:'MOCK_ARTIFACT_NEIGHBOUR'}));\n");
        } else {
            // Artifact fixtures keep their real runtime closure; the tool host is independently complete.
            Path tools = real.resolve("atf-tests/tools");
            try (var entries = Files.list(tools)) {
                for (var file : entries.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".mjs")
                        && !path.toString().endsWith(".test.mjs")).toList()) {
                    Path target = source.resolve("atf-tests/tools").resolve(file.getFileName());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                }
            }
        }
        write(source, "release/index.json", "{\"schemaVersion\":2,\"currentVersion\":\"" + VERSION + "\",\"versions\":[\"" + VERSION + "\"]}");
        for (String guide : List.of("release/README.md", RELEASE + "README.md", RELEASE + "reports/README.md"))
            write(source, guide, "# Release guide\n\n## Overview\n\nControlled metadata fixture.\n");
        Path apk = write(source, RELEASE + APK, "apk fixture");
        Path checksum = write(source, RELEASE + APK + ".sha256", hash(apk) + "  " + APK + "\n");
        var manifest = JSON.createObjectNode();
        manifest.putObject("release").put("artifactKind", "apk").put("versionName", VERSION)
                .put("apkName", APK).put("apkRelativePath", APK).put("apkBytes", Files.size(apk)).put("apkSha256", hash(apk));
        var files = manifest.putObject("integrity").putArray("files");
        var qualification = manifest.putObject("qualification");
        qualification.putObject("e2e").put("status", "NOT_RUN").put("rationale", "Controlled fixture does not execute application flows.");
        qualification.putObject("e2eGroups");
        for (Path file : List.of(apk, checksum)) files.addObject()
                .put("path", file.getFileName().toString()).put("bytes", Files.size(file)).put("sha256", hash(file));
        JSON.writeValue(source.resolve(RELEASE + "release-validation.json").toFile(), manifest);
        if (realAdmission) {
            reviewBase = TestChangeAdmissionFixture.prepareReview(source);
            parserCache = TestChangeAdmissionFixture.parserCache(scratch.resolve("parser-cache"));
            launcherScripts = TestChangeAdmissionFixture.admissionLauncherHost(real, scratch.resolve("source-admission-launcher-host"));
        } else {
            reviewBase = "a".repeat(40);
            parserCache = realEditorial ? TestChangeAdmissionFixture.parserCache(scratch.resolve("parser-cache")) : scratch.resolve("unused-cache");
            launcherScripts = realEditorial
                    ? TestChangeAdmissionFixture.editorialLauncherHost(real, scratch.resolve("editorial-launcher-host"))
                    : TestChangeAdmissionFixture.mockedLauncherHost(real, scratch.resolve("controlled-launcher-host"));
        }
        return source;
    }

    private Run launch(String runner, Path source, Path output, String mutation, Path pinned) throws Exception {
        return launch(runner, source, output, mutation, pinned, scratch, scratch);
    }

    private Run launch(String runner, Path source, Path output, String mutation, Path pinned, Path tmp, Path temp) throws Exception {
        return launch(runner, source, output, mutation, pinned, tmp, temp, null);
    }

    private Run launch(String runner, Path source, Path output, String mutation, Path pinned, Path tmp, Path temp,
                       String governanceSuite) throws Exception {
        return launch(runner, source, output, mutation, pinned, tmp, temp, governanceSuite, null);
    }

    private Run launch(String runner, Path source, Path output, String mutation, Path pinned, Path tmp, Path temp,
                       String governanceSuite, Path evidenceRoot) throws Exception {
        Path tool = scratch.resolve("maven-probe.ps1");
        Files.writeString(tool, """
                $stage = (Get-Location).Path
                $admission = Join-Path $env:ATF_FIXTURE_RUN_ROOT 'test-change-admission-recheck.json'
                if (-not (Test-Path -LiteralPath $admission) -or
                    (Get-Content -LiteralPath $admission -Raw | ConvertFrom-Json).status -cne 'PASS') { exit 21 }
                if ($stage -eq $env:ATF_FIXTURE_SOURCE -or -not ($args -contains "-Datf.governance.root=$stage") -or
                    -not (Test-Path -LiteralPath (Join-Path $stage 'release/index.json'))) { exit 19 }
                if ($env:ATF_FIXTURE_EVIDENCE_ROOT -and -not (Test-Path -LiteralPath $env:ATF_FIXTURE_EVIDENCE_ROOT -PathType Container)) { exit 20 }
                [IO.File]::WriteAllText($env:ATF_FIXTURE_ENTERED, $stage)
                [IO.File]::WriteAllText($env:ATF_FIXTURE_TEMP_ROOTS, ($env:TEMP + [Environment]::NewLine + $env:TMP))
                if ($env:ATF_FIXTURE_MUTATION -ne 'no-extent') {
                    $reportDirectory = Join-Path $env:TEMP 'whist-component-reports'
                    New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
                    [IO.File]::WriteAllText((Join-Path $reportDirectory 'fixture.html'), '<!doctype html><html><body><img src="data:image/png;base64,aW1hZ2U="></body></html>')
                }
                $testSelector = @($args | Where-Object { $_ -like '-Dtest=*' } | Select-Object -First 1)
                [IO.File]::WriteAllText($env:ATF_FIXTURE_TEST_SELECTOR, [string]$testSelector)
                switch ($env:ATF_FIXTURE_MUTATION) {
                    'live-add' { [IO.File]::WriteAllText((Join-Path $env:ATF_FIXTURE_SOURCE 'release/added.json'), '{}') }
                    'live-delete' { Remove-Item -LiteralPath (Join-Path $env:ATF_FIXTURE_SOURCE 'release/index.json') }
                    'stage-change' { [IO.File]::AppendAllText((Join-Path $stage 'release/index.json'), ' ') }
                }
                exit 0
                """);
        var command = new ArrayList<>(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File",
                launcherScripts.resolve(runner).toString(),
                "-SourceRoot", source.toString(), "-RunRoot", output.toString(), "-Maven", tool.toString(),
                "-MavenCache", parserCache.toString(), "-TestChangeBaseRef", reviewBase));
        if (runner.equals(COMPONENTS) && mutation.equals("component-boundary")) command.addAll(List.of("-Groups", "fixture-invalid-group"));
        if (governanceSuite != null) command.addAll(List.of("-GovernanceSuite", governanceSuite));
        if (pinned != null) command.addAll(List.of("-PinnedSourceInputs", pinned.toString()));
        var builder = new ProcessBuilder(command);
        var environment = builder.environment();
        // The launcher under test must resolve the fixture's TEMP/TMP roots, not inherit
        // machine-wide output roots from the parent RAG process.
        environment.remove("WHIST_QUALIFICATION_TEMP_ROOT");
        environment.remove("WHIST_EVIDENCE_ROOT");
        environment.put("TEMP", temp.toString());
        environment.put("TMP", tmp.toString());
        environment.put("ATF_FIXTURE_SOURCE", source.toString());
        environment.put("ATF_FIXTURE_RUN_ROOT", output.toString());
        environment.remove("ATF_TEST_CHANGE_BASE_REF");
        environment.remove("ATF_TEST_CHANGE_PROOF_ROOT");
        if (proofRoot != null) environment.put("ATF_TEST_CHANGE_PROOF_ROOT", proofRoot.toString());
        environment.put("ATF_FIXTURE_ENTERED", scratch.resolve("maven.entered").toString());
        environment.put("ATF_FIXTURE_TEMP_ROOTS", scratch.resolve("maven-temp-roots").toString());
        environment.put("ATF_FIXTURE_TEST_SELECTOR", scratch.resolve("maven-test-selector").toString());
        environment.put("ATF_FIXTURE_MUTATION", mutation);
        environment.put("ATF_FIXTURE_VALIDATOR_CLASSPATH", System.getProperty("java.class.path"));
        environment.put("ATF_FIXTURE_VALIDATOR_CALLS", scratch.resolve("prepared-validator-calls.jsonl").toString());
        if (evidenceRoot != null) environment.put("WHIST_EVIDENCE_ROOT", evidenceRoot.toString());
        else environment.remove("WHIST_EVIDENCE_ROOT");
        environment.put("ATF_FIXTURE_EVIDENCE_ROOT", evidenceRoot == null ? "" : evidenceRoot.toString());
        Path log = Files.createTempFile(scratch, "launcher-", ".log");
        long started = System.nanoTime();
        var process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(60, TimeUnit.SECONDS),
                    () -> LauncherFailureLog.timeoutMessage(log, Duration.ofNanos(System.nanoTime() - started)));
            return new Run(process.exitValue(), new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private static Path write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text);
    }

    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private record Run(int exit, String log) { }
}
