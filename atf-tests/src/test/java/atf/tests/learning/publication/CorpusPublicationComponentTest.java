package atf.tests.learning.publication;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode;

/** Real filesystem publication; only write/promotion faults and concurrent scheduling are controlled. */
@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class CorpusPublicationComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();

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
}
