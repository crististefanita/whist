package atf.tests.reporting.autonomy;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class StandaloneReportContractComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @TempDir Path root;
    private static final String SOURCE = "<!doctype html><html><head><style>.value{color:#222}</style></head><body>"
            + "<details><summary>Original comparison</summary><pre>Expected: 41; actual: 42; FINAL_♠</pre></details></body></html>";

    @Test void detachedBytesRemainAuditableWithoutTheirFormerDirectory(ComponentInputs inputs) throws Exception {
        Path original = Files.writeString(root.resolve("source.html"), SOURCE);
        String sha = hash(original); Path detached = Files.copy(original, Files.createDirectory(root.resolve("alone")).resolve("report.html"));
        Files.delete(original);
        inputs.add("Preconditions", "One HTML copied alone; original removed; no network or auxiliary inputs in the handoff verifier.");
        var result = StandaloneReportContract.verify(detached, sha);
        assertEquals(sha, result.sha256(), "The accepted detached report retains its qualified source hash");
        assertTrue(Files.readString(detached).contains("actual: 42; FINAL_♠"), "The original last value is retained");
        assertEquals(0, result.inlineReferences(), "A text-only component has no invented screenshots");
    }

    @Test void alteredMissingAndUnsealedArtifactsCannotBeAdmitted(ComponentInputs inputs) throws Exception {
        Path file = Files.writeString(root.resolve("report.html"), SOURCE); String sha = hash(file);
        inputs.add("Negative controls", "Replace the last value, reassign its owner, then omit the file or the original qualification seal.");
        Files.writeString(file, SOURCE.replace("actual: 42", "actual: 41"));
        assertEquals("REPORT_BYTES_CHANGED", assertThrows(java.io.IOException.class, () -> StandaloneReportContract.verify(file, sha)).getMessage());
        Files.writeString(file, SOURCE.replace("Original comparison", "Another invocation's comparison"));
        assertEquals("REPORT_BYTES_CHANGED", assertThrows(java.io.IOException.class, () -> StandaloneReportContract.verify(file, sha)).getMessage(), "Reassigned content cannot retain the qualified original seal");
        assertThrows(java.io.IOException.class, () -> StandaloneReportContract.verify(file, null));
        Files.delete(file);
        assertEquals("REPORT_DETACHED_FILE_REQUIRED", assertThrows(java.io.IOException.class, () -> StandaloneReportContract.verify(file, sha)).getMessage());
    }

    @Test void aFreshHashCannotAuthorizeExternalOrBrokenEvidence(ComponentInputs inputs) throws Exception {
        inputs.add("Negative controls", "Each mutated file has its own current hash; structural autonomy still refuses XML links, external images/styles/scripts and unresolved captures.");
        for (String extra : new String[]{"<a href='details.xml'>Original input</a>", "<img src='https://example.invalid/frame.png'>",
                "<script src='script.js'></script>", "<link rel='stylesheet' href='remote.css'>",
                "<style>.value{background:url(https://example.invalid/image)}</style>",
                "<a data-atf-screenshot-ref='lost' href='#lost'>Original frame</a>", "<iframe srcdoc='remote content'></iframe>"}) {
            Path file = Files.writeString(root.resolve("mutated.html"), SOURCE.replace("</body>", extra+"</body>"));
            var failure = assertThrows(java.io.IOException.class, () -> StandaloneReportContract.verify(file, hash(file)));
            inputs.add("Observed refusal", failure.getMessage());
            assertTrue(failure.getMessage().startsWith("REPORT_"), "The handoff refusal identifies its autonomy boundary");
        }
    }
    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
