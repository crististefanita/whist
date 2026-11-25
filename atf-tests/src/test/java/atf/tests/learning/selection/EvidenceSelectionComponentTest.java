package atf.tests.learning.selection;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.learning.*;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class EvidenceSelectionComponentTest {
    @TempDir Path root;

    @Test void boundedPopulationsReadEachCaptureOnceAndKeepAllMandatoryOutcomes(ComponentInputs inputs) throws Exception {
        var profile = SelectionFixture.JSON.readTree(GovernanceProject.locateProjectRoot().resolve("config/report-selection-profile.json").toFile());
        inputs.add("Declared selector resource profile", profile.toPrettyString());
        inputs.add("Preconditions and oracle", "Constructed immutable receipts; reversed IDs, three passing families, configured regression captures, quality gates, FAIL/ERROR/SKIP, complete Unicode context and repeated optional fingerprints. Independent literal specifications compute selected IDs and captured interval sums. No images, renderer, archive, APK or device.");
        assertEquals("WHIST_SELECTION_COST_QUALIFICATION", profile.path("kind").asText());
        assertEquals(128, profile.path("childHeapMiB").asInt(), "The selector cannot silently raise its admitted heap");
        for (var size : profile.path("populations")) for (boolean failed : new boolean[]{false, true}) {
            var log = root.resolve("population-" + size.asInt() + "-" + failed + ".log");
            String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
            var child = new ProcessBuilder(java, "-Xmx128m", "-XX:ActiveProcessorCount=2", "-cp", System.getProperty("java.class.path"),
                    SelectionCostProbe.class.getName(), size.asText(), Boolean.toString(failed)).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            boolean completed;
            try { completed = child.waitFor(profile.path("timeoutSeconds").asInt(), TimeUnit.SECONDS); }
            finally { if (child.isAlive()) { child.destroyForcibly(); child.waitFor(10, TimeUnit.SECONDS); } }
            String output = Files.readString(log);inputs.add("Original child " + size.asInt() + "/mostlyFailed=" + failed, output);
            assertTrue(completed, "Original child output survives timeout or refusal");
            assertEquals(0, child.exitValue(), "Independent selection oracle completed: " + output);
            var observed = SelectionFixture.JSON.readTree(output.trim());
            assertEquals("PASS", observed.path("status").asText());
            assertEquals(size.asInt(), observed.path("snapshots").asInt(), "Exactly one full receipt snapshot per input");
            assertEquals(observed.path("expectedRetained").asInt(), observed.path("retained").asInt());
            assertEquals(observed.path("expectedIdsSha256").asText(), observed.path("actualIdsSha256").asText(), "Original selected IDs and order match the independent oracle");
            assertTrue(observed.path("maximumHeapBytes").asLong() <= 128L * 1024 * 1024, "Child heap is actually bounded");
            assertTrue(observed.path("retained").asLong() >= observed.path("mandatory").asLong(), "Every failure, gate and required collection stays selected");
        }
    }

    @Test void completeCompositionKeepsOriginalValuesAndRejectsChangedCaptures(ComponentInputs inputs) throws Exception {
        var fixture = SelectionFixture.population(8, true);
        var manifest = SelectionFixture.JSON.createObjectNode().put("schemaVersion", 1).put("capturedInvocations", 8);
        var rows = manifest.putArray("receipts");
        for (var input : fixture.inputs()) {
            String file = input.source().getFileName().toString();
            byte[] bytes = SelectionFixture.JSON.writeValueAsBytes(input.receipt());Files.write(root.resolve(file), bytes);
            rows.addObject().put("file", file).put("invocationId", input.receipt().path("invocationId").asText()).put("sha256", LearningRunReport.hash(bytes));
        }
        Path manifestPath = root.resolve("manifest.json");Files.write(manifestPath, SelectionFixture.JSON.writeValueAsBytes(manifest));
        var loaded = LearningRunReport.load(root, manifestPath);
        var selection = EvidenceSelection.choose(loaded, SelectionFixture.policy(EvidencePolicy.DetailLevel.FULL));
        inputs.add("Preconditions", "Eight constructed receipt files, mostly failed; original integer comparisons and Unicode context. FULL keeps every invocation; tiny optional fingerprint budget never truncates required inputs. Generated HTML is a temporary inspection fixture, not extra test evidence.");
        inputs.add("Original selected IDs, verdicts and hashes", SelectionFixture.JSON.writeValueAsString(loaded.stream().map(i -> Map.of("id", i.receipt().path("invocationId").asText(), "status", i.receipt().path("status").asText(), "sha256", i.sha256())).toList()));
        Path report = root.resolve("constructed-report.html");
        LearningRunReport.publish(report, selection);
        assertEquals(8L, LearningReportHtmlAudit.verify(report, loaded), "Each original comparison, context, cause, status and timing survives real rendering");
        LearningReportHtmlAudit.verifyScope(report, selection);
        assertEquals(8, selection.inputs().size(), "Optional budgets never remove mandatory FULL invocations");
        var document = org.jsoup.Jsoup.parse(Files.readString(report));
        var context = document.select("pre").stream().filter(e -> e.wholeText().contains("Ω <literal>")).findFirst().orElseThrow();
        context.text(context.wholeText().replace("Ω <literal>", "CHANGED"));
        Path mutant = root.resolve("changed-report.html");Files.writeString(mutant, document.outerHtml());
        assertThrows(IllegalArgumentException.class, () -> LearningReportHtmlAudit.verify(mutant, loaded), "Changed mandatory context cannot pass the original-value oracle");
        Files.writeString(root.resolve(rows.get(0).path("file").asText()), "changed capture");
        assertThrows(IllegalArgumentException.class, () -> LearningRunReport.load(root, manifestPath), "Changed source bytes are refused before composition");
        var duplicate = new ArrayList<>(loaded);duplicate.add(loaded.get(0));
        assertThrows(IllegalArgumentException.class, () -> EvidenceSelection.choose(duplicate, selection.policy()), "Duplicate captures cannot inflate the denominator");
    }
}
