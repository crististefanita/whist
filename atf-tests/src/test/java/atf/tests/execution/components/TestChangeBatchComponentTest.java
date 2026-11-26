package atf.tests.execution.components;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class TestChangeBatchComponentTest {
    private static final String FIRST = "FIRST-CONTROL";
    private static final String SECOND = "SECOND-CONTROL";
    private static final String THIRD = "THIRD-CONTROL";
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path scratch;

    @Test void publishesBothControlsAfterCheckingTheSnapshot() throws Exception {
        var result = execute("success");
        AuditedAssertions.assertEquals(0, result.exitCode(), diagnostic(result));
        AuditedAssertions.assertEquals(List.of(FIRST, SECOND, "snapshot"), calls());
        for (String id : List.of(FIRST, SECOND)) {
            Path folder = scratch.resolve("proofs").resolve(id);
            AuditedAssertions.assertEquals(id, Files.readString(folder.resolve("proof.json")));
            AuditedAssertions.assertEquals(id, Files.readString(folder.resolve("component-report.html")));
        }
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("proofs.pending")));
    }

    @Test void failedControlDoesNotStopTheRemainingControlsOrPublishTheirProofs() throws Exception {
        assertUnpublished(execute("control"), "TEST-CHANGE-BATCH-CONTROL", List.of(FIRST, SECOND, "THIRD-CONTROL"));
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("proofs.pending/THIRD-CONTROL/proof.pending.json")));
    }

    @Test void changedSourcePreventsPublicationOfPassingControls() throws Exception {
        assertUnpublished(execute("snapshot"), "QUALIFICATION-SNAPSHOT-CHANGED", List.of(FIRST, SECOND, "snapshot"));
    }

    @Test void changedReviewPreventsPublicationOfPassingControls() throws Exception {
        assertUnpublished(execute("review"), "TEST-CHANGE-BATCH-PLAN", List.of(FIRST, SECOND, "snapshot"));
    }

    @Test void isolatedDriverCorrectionRerunsTheBundleAndSharedInputDriftRejectsPromotion() throws Exception {
        Path sharedSource = scratch.resolve("shared-input.txt");
        Files.writeString(sharedSource, "pinned-shared-input", StandardCharsets.UTF_8);
        String pinnedHash = sha256(Files.readAllBytes(sharedSource));
        Path failedAttempt = Files.createDirectories(scratch.resolve("driver-failure"));
        var failed = execute(failedAttempt, sharedSource, "driver-failure", "attempt-one", pinnedHash);
        AuditedAssertions.assertEquals(1, failed.exitCode(), diagnostic(failed));
        AuditedAssertions.assertTrue(diagnostic(failed).contains("TEST-CHANGE-BATCH-CONTROL"), diagnostic(failed));
        AuditedAssertions.assertEquals(List.of(FIRST, SECOND, THIRD), calls(failedAttempt));
        AuditedAssertions.assertTrue(Files.isRegularFile(failedAttempt.resolve("proofs.pending/" + FIRST + "/proof.pending.json")));
        AuditedAssertions.assertFalse(Files.exists(failedAttempt.resolve("proofs")));
        var originalIdentities = identities(failedAttempt.resolve("proofs.pending"), List.of(FIRST, THIRD), true);

        Path correctedAttempt = Files.createDirectories(scratch.resolve("driver-corrected"));
        var corrected = execute(correctedAttempt, sharedSource, "success", "attempt-two", pinnedHash);
        AuditedAssertions.assertEquals(0, corrected.exitCode(), diagnostic(corrected));
        AuditedAssertions.assertEquals(List.of(FIRST, SECOND, THIRD, "snapshot"), calls(correctedAttempt));
        var correctedIdentities = identities(correctedAttempt.resolve("proofs"), List.of(FIRST, SECOND, THIRD), false);
        for (String id : List.of(FIRST, SECOND, THIRD)) {
            verifyPopulation(correctedAttempt.resolve("proofs").resolve(id), id, "attempt-two", pinnedHash);
        }
        AuditedAssertions.assertEquals(originalIdentities, identities(failedAttempt.resolve("proofs.pending"), List.of(FIRST, THIRD), true),
                "The failed attempt's original passing JUnit and report bytes remain immutable after a corrected attempt");
        AuditedAssertions.assertTrue(!originalIdentities.get(FIRST + "/component-report.html")
                .equals(correctedIdentities.get(FIRST + "/component-report.html")),
                "The corrected attempt owns a distinct Extent report identity");

        Path sharedChangeAttempt = Files.createDirectories(scratch.resolve("shared-input-change"));
        var changed = execute(sharedChangeAttempt, sharedSource, "shared-change", "attempt-three", pinnedHash);
        AuditedAssertions.assertEquals(1, changed.exitCode(), diagnostic(changed));
        AuditedAssertions.assertTrue(diagnostic(changed).contains("QUALIFICATION-SNAPSHOT-CHANGED"), diagnostic(changed));
        AuditedAssertions.assertEquals(List.of(FIRST, SECOND, THIRD, "snapshot"), calls(sharedChangeAttempt));
        AuditedAssertions.assertFalse(Files.exists(sharedChangeAttempt.resolve("proofs")));
        AuditedAssertions.assertTrue(Files.isRegularFile(sharedChangeAttempt.resolve("proofs.pending/" + FIRST + "/proof.pending.json")));
        AuditedAssertions.assertEquals(originalIdentities, identities(failedAttempt.resolve("proofs.pending"), List.of(FIRST, THIRD), true));
        AuditedAssertions.assertEquals(correctedIdentities, identities(correctedAttempt.resolve("proofs"), List.of(FIRST, SECOND, THIRD), false));
        AuditedAssertions.assertTrue(!originalIdentities.isEmpty(), "The first attempt keeps its original JUnit and report digests");
        AuditedAssertions.assertTrue(!correctedIdentities.isEmpty(), "The corrected bundle has its own JUnit and report digests");
        AuditedAssertions.assertTrue(diagnostic(changed).contains("QUALIFICATION-SNAPSHOT-CHANGED"),
                "A changed shared source cannot be promoted from a prior attempt");
    }

    private void assertUnpublished(CommandProcess.Output result, String code, List<String> expected) throws Exception {
        AuditedAssertions.assertEquals(1, result.exitCode(), diagnostic(result));
        AuditedAssertions.assertTrue(diagnostic(result).contains(code), diagnostic(result));
        AuditedAssertions.assertEquals(expected, calls());
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("proofs.pending/" + FIRST + "/proof.pending.json")));
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("proofs")));
    }

    private List<String> calls() throws Exception {
        return calls(scratch);
    }

    private static List<String> calls(Path root) throws Exception { return Files.readAllLines(root.resolve("calls.txt")); }

    private CommandProcess.Output execute(String mode) throws Exception {
        Path launcher = scratch.resolve("batch.ps1");
        Files.writeString(launcher, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_TEST_BATCH_HELPER
                $root = $env:ATF_TEST_BATCH_ROOT
                $mode = $env:ATF_TEST_BATCH_MODE
                $calls = Join-Path $root 'calls.txt'
                $source = Join-Path $root 'input.txt'
                [IO.File]::WriteAllText($source, 'pinned')
                Invoke-AtfTestChangeBatch -RunRoot $root -ReadPlan {
                    'FIRST-CONTROL'
                    if ($mode -ne 'review' -or !(Test-Path -LiteralPath $calls)) { 'SECOND-CONTROL' }
                    if ($mode -eq 'control') { 'THIRD-CONTROL' }
                } -RunControl {
                    param($id,$output)
                    Add-Content -LiteralPath $calls -Value $id
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'control') { return 7 }
                    [IO.Directory]::CreateDirectory($output) | Out-Null
                    [IO.File]::WriteAllText((Join-Path $output 'proof.pending.json'), $id)
                    [IO.File]::WriteAllText((Join-Path $output 'component-report.html'), $id)
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'snapshot') { [IO.File]::WriteAllText($source, 'changed') }
                    0
                } -CheckSnapshot {
                    Add-Content -LiteralPath $calls -Value 'snapshot'
                    if ([IO.File]::ReadAllText($source) -cne 'pinned') { throw 'QUALIFICATION-SNAPSHOT-CHANGED: input.txt' }
                }
                """);
        var helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfTestChangeBatch.ps1");
        return CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", launcher.toString()),
                Map.of("ATF_TEST_BATCH_HELPER", helper.toString(), "ATF_TEST_BATCH_ROOT", scratch.toString(),
                        "ATF_TEST_BATCH_MODE", mode), Duration.ofSeconds(45), true);
    }

    private CommandProcess.Output execute(Path runRoot, Path sharedSource, String mode, String runId, String expectedSourceHash) throws Exception {
        Path launcher = runRoot.resolve("batch.ps1");
        Files.writeString(launcher, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_TEST_BATCH_HELPER
                $root = $env:ATF_TEST_BATCH_ROOT
                $source = $env:ATF_TEST_BATCH_SHARED_SOURCE
                $mode = $env:ATF_TEST_BATCH_MODE
                $attempt = $env:ATF_TEST_BATCH_RUN_ID
                $expectedHash = $env:ATF_TEST_BATCH_EXPECTED_SOURCE_HASH
                $calls = Join-Path $root 'calls.txt'
                Invoke-AtfTestChangeBatch -RunRoot $root -ReadPlan {
                    'FIRST-CONTROL'
                    'SECOND-CONTROL'
                    'THIRD-CONTROL'
                } -RunControl {
                    param($id,$output)
                    Add-Content -LiteralPath $calls -Value $id
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'driver-failure') { return 7 }
                    [IO.Directory]::CreateDirectory($output) | Out-Null
                    $sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
                    $junit = '<testsuite name="' + $id + '" tests="2" failures="0"><testcase name="case-a"/><testcase name="case-b"/></testsuite>'
                    $junitPath = Join-Path $output 'junit.xml'
                    $extentPath = Join-Path $output 'component-report.html'
                    [IO.File]::WriteAllText($junitPath, $junit)
                    $junitHash = (Get-FileHash -LiteralPath $junitPath -Algorithm SHA256).Hash.ToLowerInvariant()
                    [IO.File]::WriteAllText($extentPath, '<html data-attempt="' + $attempt + '" data-control="' + $id + '"><body>Extent population=2 junit=' + $junitHash + '</body></html>')
                    $extentHash = (Get-FileHash -LiteralPath $extentPath -Algorithm SHA256).Hash.ToLowerInvariant()
                    $proof = [ordered]@{control=$id; attempt=$attempt; sourceHash=$sourceHash; selector=$id; junitPopulation=@('case-a','case-b'); junitSha256=$junitHash; extentSha256=$extentHash} | ConvertTo-Json -Compress
                    [IO.File]::WriteAllText((Join-Path $output 'proof.pending.json'), $proof)
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'shared-change') { [IO.File]::WriteAllText($source, 'changed-shared-input') }
                    0
                } -CheckSnapshot {
                    Add-Content -LiteralPath $calls -Value 'snapshot'
                    $actualHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
                    if ($actualHash -cne $expectedHash) { throw 'QUALIFICATION-SNAPSHOT-CHANGED: shared-input.txt' }
                }
                """);
        var helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfTestChangeBatch.ps1");
        return CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", launcher.toString()),
                Map.of("ATF_TEST_BATCH_HELPER", helper.toString(), "ATF_TEST_BATCH_ROOT", runRoot.toString(),
                        "ATF_TEST_BATCH_SHARED_SOURCE", sharedSource.toString(), "ATF_TEST_BATCH_MODE", mode,
                        "ATF_TEST_BATCH_RUN_ID", runId, "ATF_TEST_BATCH_EXPECTED_SOURCE_HASH", expectedSourceHash),
                Duration.ofSeconds(45), true);
    }

    private static Map<String, String> identities(Path root, List<String> controls, boolean pending) throws Exception {
        var result = new java.util.LinkedHashMap<String, String>();
        for (String id : controls) {
            Path folder = root.resolve(id);
            String proofName = pending ? "proof.pending.json" : "proof.json";
            for (String file : List.of(proofName, "junit.xml", "component-report.html")) {
                Path path = folder.resolve(file);
                result.put(id + "/" + file, sha256(Files.readAllBytes(path)));
            }
        }
        return result;
    }

    private static void verifyPopulation(Path folder, String id, String attempt, String sourceHash) throws Exception {
        JsonNode proof = JSON.readTree(Files.readString(folder.resolve("proof.json")));
        AuditedAssertions.assertEquals(id, proof.path("control").asText());
        AuditedAssertions.assertEquals(attempt, proof.path("attempt").asText());
        AuditedAssertions.assertEquals(sourceHash, proof.path("sourceHash").asText());
        AuditedAssertions.assertEquals(id, proof.path("selector").asText());
        AuditedAssertions.assertEquals(List.of("case-a", "case-b"), JSON.convertValue(proof.path("junitPopulation"), List.class));
        AuditedAssertions.assertEquals(2, Files.readString(folder.resolve("junit.xml")).split("<testcase ", -1).length - 1);
        AuditedAssertions.assertTrue(Files.readString(folder.resolve("junit.xml")).contains("tests=\"2\" failures=\"0\""));
        AuditedAssertions.assertEquals(proof.path("junitSha256").asText(), sha256(Files.readAllBytes(folder.resolve("junit.xml"))));
        AuditedAssertions.assertEquals(proof.path("extentSha256").asText(), sha256(Files.readAllBytes(folder.resolve("component-report.html"))));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String diagnostic(CommandProcess.Output result) {
        return new String(result.stdout(), StandardCharsets.UTF_8);
    }
}
