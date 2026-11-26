package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class ReportHeapComponentTest {
    private static final List<String> PUBLICATION_STAGES = List.of("RENDER_AND_FINALIZE", "VERIFY", "PROMOTE");

    @TempDir Path root;

    static atf.reporting.contract.AtfReportPorts.Session openFixtureRenderer(Path report,
            atf.reporting.configuration.ReportingSettings settings) {
        return new atf.reporting.extent.session.ExtentReportSession(report, settings);
    }

    @Test void boundedSmallMediumAndLargeReportsKeepEveryComparisonUnderTheDeclaredHeap(ComponentInputs inputs) throws Exception {
        var source = GovernanceProject.locateProjectRoot();
        var profile = new ObjectMapper().readTree(source.resolve("config/report-heap-profile.json").toFile());
        inputs.add("Declared resource profile", profile.toPrettyString());
        inputs.add("Independent expected population", "For each index i, the full original text is owner=i; value=x repeated valueCharacters times; final=i;. Only the final primary comparison fails deliberately. Two additional PNG-signature comparisons pass. Synthetic rows are not extra JUnit tests.");
        assertEquals("WHIST_REPORT_HEAP_QUALIFICATION", profile.path("kind").asText());
        assertEquals(3, profile.path("populations").size(), "Small, medium and large populations are all declared");
        assertTrue(profile.path("childHeapMiB").asInt() > 0 && profile.path("childHeapMiB").asInt() <= 768,
                "The profile cannot silently increase the suite heap");
        assertEquals(atf.android.evidence.component.publication.ComponentReportPublication.DEFAULT_MAX_BYTES,
                profile.path("maximumReportBytes").asLong(), "The fixture qualifies the actual default artifact guard");
        inputs.add("Qualification limits", "Real pinned Reporting JAR; isolated child per population; repeated synthetic PNG bytes, no product screenshots, emulator or APK. Rendering and navigation remain one combined library phase; pool peaks are cumulative upper bounds, not isolated phase measurements.");

        int previous = 0;
        for (JsonNode population : profile.path("populations")) {
            assertTrue(population.canConvertToInt() && population.asInt() > previous,
                    "Declared small, medium and large populations are positive and strictly increasing");
            previous = population.asInt();
            verifyPopulation(inputs, profile, population.asInt());
        }
    }

    private void verifyPopulation(ComponentInputs inputs, JsonNode profile, int population) throws Exception {
        Path directory = Files.createDirectory(root.resolve("population-" + population));
        Path log = directory.resolve("child.log");
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var process = new ProcessBuilder(java, "-Xmx" + profile.path("childHeapMiB").asInt() + "m", "-XX:ActiveProcessorCount=2",
                "-Djava.io.tmpdir=" + directory, "-cp", System.getProperty("java.class.path"), BoundedReportProbe.class.getName(),
                directory.toString(), Integer.toString(population), profile.path("valueCharacters").asText(),
                profile.path("maximumReportBytes").asText(), profile.path("bodyPartCharacters").asText())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished;
        try {
            finished = process.waitFor(profile.path("timeoutSeconds").asInt(), TimeUnit.SECONDS);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
        if (!finished) process.waitFor(10, TimeUnit.SECONDS);

        String original = Files.readString(log);
        inputs.add("Original isolated child output — " + population, original);
        assertTrue(finished, "The child remains within its declared time budget; original output is captured");
        assertEquals(0, process.exitValue(), "The declared heap completes publication and verification: " + original);

        String line = original.lines().filter(value -> value.startsWith("{\"status\":\"PASS\""))
                .reduce((left, right) -> right).orElseThrow();
        JsonNode observed = new ObjectMapper().readTree(line);
        assertEquals(population, observed.path("comparisons").asInt());
        assertEquals(1, observed.path("capturedFailures").asInt(), "The final deliberate mismatch remains a failed comparison");
        assertEquals(2, observed.path("mediaComparisons").asInt(), "Synthetic PNG validations have their own authored assertion owners");
        assertTrue(observed.path("profileParity").asBoolean(), "All authored comparison identities and body labels remain in the physical HTML");
        assertTrue(observed.path("mandatoryPngRetained").asBoolean(), "The original synthetic PNG remains inline after finalization");
        assertEquals(population, observed.path("completeValuesVerified").asInt(),
                "A streaming independent oracle verifies each full original value across body parts");

        JsonNode publication = observed.path("publication");
        assertEquals("PASS", publication.path("status").asText());
        assertTrue(publication.path("maximumHeapBytes").asLong() <= profile.path("childHeapMiB").asLong() * 1024 * 1024,
                "The child heap is actually bounded");
        assertPublicationStages(publication.path("stages"));
        assertTrue(publication.path("bytes").asLong() < profile.path("maximumReportBytes").asLong(),
                "The accepted artifact is within the declared byte limit");
    }

    private static void assertPublicationStages(JsonNode stages) {
        var names = new ArrayList<String>();
        for (JsonNode stage : stages) names.add(stage.path("name").asText());
        assertEquals(PUBLICATION_STAGES, names, "The receipt names rendering/finalization, verification and promotion separately");
        for (JsonNode stage : stages) {
            assertEquals("PASS", stage.path("status").asText(), "Every publication stage is individually accepted");
            assertTrue(stage.path("elapsedNanos").asLong() > 0, "Every stage has a measured duration");
            assertTrue(stage.path("cumulativePoolPeakUpperBound").asLong() > 0,
                    "The receipt retains its cumulative pool bound without presenting it as an isolated phase peak");
        }
    }
}
