package atf.tests.governance.support;

import atf.android.evidence.component.ComponentInputs;
import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
@ExtendWith(GameReportAudit.class)
class LauncherCapabilitiesComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void requiredToolsResolveBeforeVersionProbesAndBeforeLauncherPreparation(
            @TempDir Path work, ComponentInputs inputs) throws Exception {
        JsonNode ready = fixture("ready", work, inputs);
        assertEquals("PASS", ready.path("status").asText());
        for (JsonNode row : ready.path("rows")) {
            assertTrue(row.path("sourceBound").asBoolean(), row.path("name").asText());
            assertEquals(List.of("resolve:java", "resolve:javac", "resolve:jar", "resolve:mvn", "resolve:node",
                    "probe:java", "probe:mvn", "probe:node"), JSON.convertValue(row.path("events"), List.class));
            assertEquals("PINNED_WRAPPER_INPUTS", row.path("gradleKind").asText());
            assertEquals(3, row.path("jdkCapabilities").asInt(), "One runtime probe covers all binaries in one JDK");
            assertEquals("RUNTIME_VERSION", row.path("capabilities").get(3).path("kind").asText());
        }
        for (JsonNode binding : ready.path("bindings")) {
            assertTrue(binding.path("beforePreparation").asBoolean(), binding.path("file").asText());
        }
        inputs.add("Precondition and capability observations", ready.toPrettyString());
    }

    @Test void missingMalformedAndUnpinnedToolsAreRejectedBeforeExpensiveWork(@TempDir Path work,
                                                                               ComponentInputs inputs) throws Exception {
        JsonNode refused = fixture("failures", work, inputs);
        assertEquals("PASS", refused.path("status").asText());
        assertEquals(13, refused.path("rows").size());
        for (JsonNode row : refused.path("rows")) {
            assertTrue(row.path("refused").asBoolean(), row.path("name").asText());
            assertTrue(row.path("sourceBound").asBoolean(), row.path("cause").asText());
            if (row.path("name").asText().startsWith("missing-") || row.path("name").asText().equals("gradle-unpinned")) {
                assertFalse(row.path("probeStarted").asBoolean(), row.path("name").asText());
            }
        }
        inputs.add("Controlled missing tools, malformed versions and unpinned Gradle", refused.toPrettyString());
    }

    @Test void commandScriptLaunchUsesArgumentBoundariesAndRetainsExitCodes(@TempDir Path work,
                                                                             ComponentInputs inputs) throws Exception {
        Path folder = Files.createDirectories(work.resolve("Maven Tool Home"));
        Path success = Files.writeString(folder.resolve("mvn-ok.cmd"), "@echo off\r\necho Apache Maven 3.10.0\r\nexit /b 0\r\n");
        Path failure = Files.writeString(folder.resolve("mvn-fail.cmd"), "@echo off\r\necho controlled failure\r\nexit /b 7\r\n");
        Path probe = work.resolve("invoke-command.ps1");
        Files.writeString(probe, """
                param($Helper,$Success,$Failure)
                $ErrorActionPreference='Stop'
                . $Helper
                $ok=Invoke-AtfLauncherVersion $Success @('-version') 5 'mvn-fixture'
                $failed=Invoke-AtfLauncherVersion $Failure @('-version') 5 'mvn-fixture'
                @{ok=$ok;failed=$failed}|ConvertTo-Json -Depth 4 -Compress
                """);
        var process = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-File", probe.toString(),
                GovernanceProject.locateProjectRoot().resolve("scripts/AtfLauncherCapabilities.ps1").toString(),
                success.toString(), failure.toString()).redirectErrorStream(true).start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS),
                    "The bounded command-script adapter completed using paths with spaces");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            inputs.add("Controlled command-script executions", output);
            AuditedAssertions.assertEquals(0, process.exitValue(), output);
            JsonNode observed = JSON.readTree(output);
            AuditedAssertions.assertEquals(0, observed.path("ok").path("ExitCode").asInt());
            AuditedAssertions.assertTrue(observed.path("ok").path("Output").asText().contains("Apache Maven 3.10.0"));
            AuditedAssertions.assertEquals(7, observed.path("failed").path("ExitCode").asInt());
            AuditedAssertions.assertTrue(observed.path("failed").path("Output").asText().contains("controlled failure"));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private static JsonNode fixture(String scenario, Path work, ComponentInputs inputs) throws Exception {
        Path project = GovernanceProject.locateProjectRoot();
        Path script = project.resolve("atf-tests/src/test/resources/toolchain/launcher-capabilities-contract.ps1");
        inputs.add("Precondition", "Controlled command resolver and version responses with the production capability checker and actual launch-script ordering. No Gradle execution, Maven build, Android device, browser, sleep or screenshot.");
        var result = CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File",
                        script.toString(), "-Root", project.toString(), "-Work", work.toString(), "-Case", scenario),
                project, java.util.Map.of(), Duration.ofSeconds(30), true);
        String output = new String(result.stdout(), StandardCharsets.UTF_8);
        inputs.add("Controlled launcher output", output);
        assertEquals(0, result.exitCode(), output);
        return JSON.readTree(output);
    }
}
