package atf.tests.learning.publication;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

/** Real filesystem publication; only write/promotion faults and concurrent scheduling are controlled. */
@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class CorpusPublicationComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void independentPlayerPreservesEquivalentScoreContextsThroughItsGradleComponent(
            @TempDir Path scratch, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var reportRoot = Files.createDirectories(scratch.resolve("independent-score-context-report"));
        var script = scratch.resolve("run-independent-score-context.ps1");
        Files.writeString(script, """
                param($Root,$ReportRoot)
                $ErrorActionPreference='Stop'
                $project=Join-Path $Root 'whist-lab-app'
                $gradle=Join-Path $project 'gradlew.bat'
                $build=Join-Path $project 'algorithm-build'
                & $gradle -p $build :ai-lab:test --tests 'ro.whistlab.coach.IndependentPlayerContractTest.equivalentRelativeScoreContextsKeepIndependentSamplesAndDecisions' --offline --no-daemon "-Pwhist.auditOutput=$ReportRoot" '-Pwhist.auditSource=ATF-J4 independent score context component'
                exit $LASTEXITCODE
                """);
        inputs.add("Precondition", "The AI component runs on the test-audit Gradle path from the isolated project snapshot. It compares full bid/card choices under a common score translation and a relative-score negative control; no archived games, APK, emulator, screenshot or full Gradle collection is used.");
        var process = atf.android.process.CommandProcess.execute(
                List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString(), root.toString(), reportRoot.toString()),
                root, Map.of(), Duration.ofMinutes(3), true);
        String output = new String(process.stdout(), java.nio.charset.StandardCharsets.UTF_8);
        inputs.add("Original Gradle component process", output);
        AuditedAssertions.assertEquals(0, process.exitCode(), output);
        try (var reports = Files.list(reportRoot)) {
            var html = reports.filter(file -> file.getFileName().toString().endsWith(".html")).toList();
            AuditedAssertions.assertEquals(1, html.size(), "The Gradle AI component owns one canonical HTML report");
            AuditedAssertions.assertTrue(Files.size(html.getFirst()) > 0, "The original Gradle report remains available beside its digest");
            inputs.add("Gradle component report identity", html.getFirst().getFileName() + " SHA-256=" + sha256(html.getFirst()));
        }
        AuditedAssertions.assertTrue(output.contains("BUILD SUCCESSFUL"), "The child build reports its original successful completion");
    }

    @Test void completeCorpusAndPairedBatchesKeepIndependentOriginalByteHashes(ComponentInputs inputs) throws Exception {
        String output = exercise("retains", inputs);
        var corpus = observation(output, "Corpus publication observations:");
        AuditedAssertions.assertEquals(4, corpus.path("sourceGames").asInt(), "Constructed legal 3–6-seat source games");
        comparisons(corpus);
        var pairs = observation(output, "Pair publication observations:");
        AuditedAssertions.assertEquals(4, pairs.path("pairs").asInt(), "Every protocol seat appears once");
        AuditedAssertions.assertEquals(8, pairs.path("gameAudits").asInt(), "Both games of every pair independently replay");
        AuditedAssertions.assertEquals("batch-integrity-development-composed", pairs.path("gate").asText(), "Final identity excludes private staging names");
        comparisons(pairs);
    }

    @Test void interruptedConcurrentAndFailedInputsCannotPublishOrEraseOriginals(ComponentInputs inputs) throws Exception {
        String output = exercise("refuses", inputs);
        var corpus = observation(output, "Corpus publication rejection observations:");
        AuditedAssertions.assertEquals(1, corpus.path("concurrentWinners").asInt());
        comparisons(corpus);
        var pairs = observation(output, "Pair publication rejection observations:");
        AuditedAssertions.assertEquals(1, pairs.path("concurrentWinners").asInt());
        AuditedAssertions.assertTrue(pairs.path("duplicateRefused").asBoolean(), "Duplicate completed pairs cannot be counted twice");
        AuditedAssertions.assertTrue(pairs.path("failedRefused").asBoolean(), "A recorded FAIL cannot be converted to PASS");
        comparisons(pairs);
    }

    @Test void curatorSelectionScalesAcrossDeclaredVolumesWithinTheBoundedNodeHeap(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Synthetic selector entries only; no archived or heldout games, filesystem publication, emulator, APK or images. After a 128-entry warm-up, declared populations are 100, 1,000 and 5,000 unique traces, with eight lesson tokens each, a 100-game cap and a 128 MiB Node heap. Retained heap after explicit GC is recorded; it is not presented as peak memory.");
        String output = runNode(List.of("node", "--expose-gc", "--max-old-space-size=128", "--test",
                        "--test-name-pattern", "declared curator volume profile",
                        root.resolve("whist-lab-app/ai-lab/tools/corpus/whist-active-corpus.test.mjs").toString()),
                root, Duration.ofSeconds(60), text -> inputs.add("Original Node scaling observations", text));
        String marker = "Curator volume observations:";
        var line = output.lines().filter(value -> value.contains(marker)).findFirst().orElseThrow();
        var profile = JSON.readTree(line.substring(line.indexOf(marker) + marker.length()).trim());
        AuditedAssertions.assertEquals("synthetic-selector-v1", profile.path("profile").asText());
        AuditedAssertions.assertEquals(100, profile.path("maxGames").asInt());
        AuditedAssertions.assertEquals(8, profile.path("lessonsPerEntry").asInt());
        AuditedAssertions.assertEquals(128, profile.path("warmupGames").asInt());
        AuditedAssertions.assertEquals(60, profile.path("timeoutSeconds").asInt());
        AuditedAssertions.assertEquals(3, profile.path("rows").size());
        int[] expectedVolumes = {100, 1000, 5000};
        for (int index = 0; index < expectedVolumes.length; index++) {
            var row = profile.path("rows").get(index);
            AuditedAssertions.assertEquals(expectedVolumes[index], row.path("inputGames").asInt());
            AuditedAssertions.assertEquals(expectedVolumes[index], row.path("uniqueTraces").asInt());
            AuditedAssertions.assertEquals(64, row.path("coveredLessons").asInt());
            AuditedAssertions.assertTrue(row.path("selectedGames").asInt() <= 100,
                    "The active cap holds at input volume " + expectedVolumes[index]);
            AuditedAssertions.assertEquals(row.path("selectionHash").asText(), row.path("reverseHash").asText());
            AuditedAssertions.assertTrue(row.path("inputBytes").asLong() > 0);
            AuditedAssertions.assertTrue(row.path("heapAfterBytes").asLong() <= row.path("heapBudgetBytes").asLong());
            AuditedAssertions.assertTrue(row.path("elapsedMs").asDouble() >= 0);
            AuditedAssertions.assertTrue(row.path("reverseElapsedMs").asDouble() >= 0);
            inputs.add("Volume row " + expectedVolumes[index], row.toPrettyString());
        }
        AuditedAssertions.assertEquals(128L * 1024 * 1024, profile.path("heapBudgetBytes").asLong());
    }

    private static String exercise(String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Locally generated legal Whist traces, 3–6 seats; four declared paired seats. Actual byte hashes, fresh directories and real cooperative writers. Injected write/promotion faults; no archive, device or screenshots. Node heap is bounded at 128 MiB.");
        return runNode(List.of("node", "--max-old-space-size=128", "--test", "--test-name-pattern", selection,
                root.resolve("whist-lab-app/ai-lab/tools/testing/corpus-publication.test.mjs").toString()), root,
                Duration.ofSeconds(60), text -> inputs.add("Original Node observations and comparisons", text));
    }

    private static void comparisons(JsonNode result) {
        AuditedAssertions.assertTrue(!result.path("rows").isEmpty(), "Independent expected/actual rows are mandatory");
        for (var row : result.path("rows")) {
            AuditedAssertions.assertEquals(row.required("expected"), row.required("actual"), row.path("name").asText());
        }
    }

    private static JsonNode observation(String output, String marker) throws Exception {
        String line = output.lines().filter(value -> value.contains(marker)).findFirst().orElseThrow();
        return JSON.readTree(line.substring(line.indexOf(marker) + marker.length()).trim());
    }

    private static String sha256(Path file) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file)));
    }
}
