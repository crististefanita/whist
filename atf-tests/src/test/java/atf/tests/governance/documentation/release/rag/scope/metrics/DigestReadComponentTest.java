package atf.tests.governance.documentation.release.rag.scope.metrics;

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

/** Actual filesystem and digest implementations; source identity and reader admission are controlled. */
@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class DigestReadComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void preparedReaderKeepsIndependentFullByteDigestsAndColdFallback(ComponentInputs inputs) throws Exception {
        String text = exercise("source-bound reader preserves", inputs);
        JsonNode result = observation(text, "Seal reader byte observations:");
        AuditedAssertions.assertEquals(28, result.path("rows").size(), "Fourteen independent byte fixtures through both real readers");
        for (var row : result.path("rows")) {
            AuditedAssertions.assertEquals(row.required("expected").asText(), row.required("actual").asText(),
                    "Complete SHA-256 · " + row.path("file").asText() + " · " + row.path("reader").asText());
        }
        AuditedAssertions.assertEquals("native-source-bound", result.path("reader").asText());
        AuditedAssertions.assertEquals(15, result.path("hashes").asInt(), "All class/resource bytes and only cache JAR/POM files enter the seal");
        AuditedAssertions.assertTrue(result.path("coldImportDidNotCompile").asBoolean(), "Hash-only callers retain the original path without compiler startup");
    }

    @Test void foreignReadersAndFreshCorruptionCannotBorrowAValidSeal(ComponentInputs inputs) throws Exception {
        String text = exercise("unadmitted readers|a foreign preloaded", inputs);
        JsonNode result = observation(text, "Seal reader rejection observations:");
        AuditedAssertions.assertEquals(6, result.path("fallbacks").size(), "Missing, malformed, stale, foreign-method and multicast capabilities are refused");
        for (var row : result.path("fallbacks")) {
            AuditedAssertions.assertEquals("powershell-full-stream", row.path("reader").asText());
            AuditedAssertions.assertEquals(row.required("expected").asText(), row.required("actual").asText(), "Fallback retains full byte identity");
        }
        for (var row : result.path("changes")) {
            AuditedAssertions.assertEquals(row.required("expected").asText(), row.required("actual").asText(), "Independent corruption oracle at byte " + row.path("offset").asLong());
            AuditedAssertions.assertNotEquals(result.path("baselineFingerprint").asText(), row.path("fingerprint").asText(), "Same-length/time alteration cannot borrow the original seal");
        }
        AuditedAssertions.assertEquals("IOException", result.path("readFailure").asText());
        AuditedAssertions.assertEquals("FileNotFoundException", result.path("missing").asText());
        AuditedAssertions.assertTrue(result.path("handlesReleased").asBoolean());
        JsonNode foreign = observation(text, "Foreign reader observations:");
        AuditedAssertions.assertTrue(foreign.path("foreignTypeRefused").asBoolean());
        AuditedAssertions.assertEquals("powershell-full-stream", foreign.path("reader").asText());
    }

    private static String exercise(String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        inputs.add("Precondition", "Actual old PowerShell/native SHA-256 implementations and full independent Node oracles. Empty, Unicode, block-boundary and multi-megabyte fixtures; controlled reader descriptors and real sharing refusal. No timing threshold or screenshot.");
        return runNode(List.of("node", "--test", "--test-name-pattern", selection,
                root.resolve("atf-tests/tools/seal-reader.test.mjs").toString()), root, Duration.ofSeconds(130),
                value -> inputs.add("Original process output and byte comparisons", value));
    }

    private static JsonNode observation(String output, String marker) throws Exception {
        String line = output.lines().filter(value -> value.contains(marker)).findFirst().orElseThrow();
        return JSON.readTree(line.substring(line.indexOf(marker) + marker.length()).trim());
    }
}
