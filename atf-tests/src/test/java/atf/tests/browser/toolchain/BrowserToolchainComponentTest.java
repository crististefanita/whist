package atf.tests.browser.toolchain;

import atf.android.evidence.component.ComponentInputs;
import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class BrowserToolchainComponentTest {
    @Test void javaCapabilitiesAndMavenPresencePrecedeBrowserPreparation(ComponentInputs inputs) throws Exception {
        var observed = fixture("ready", inputs);
        assertEquals(2, observed.path("rows").size());
        String source = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(GovernanceProject.locateProjectRoot().resolve("scripts/AtfBrowserToolchain.ps1"))));
        for(var row:observed.path("rows")) {
            assertEquals(source,row.path("sourceSha256").asText(),"The receipt identifies the actually consumed preflight implementation");
            assertEquals(4,row.path("capabilities").size());
            assertEquals(List.of("resolve:java","resolve:javac","resolve:jar","resolve:mvn","probe:java","probe:javac","probe:jar"),new ObjectMapper().convertValue(row.path("events"),List.class),"All tools are resolved before version probes; Maven presence is distinct from runtime qualification");
            assertEquals("APPLICATION_PRESENT",row.path("capabilities").get(3).path("kind").asText());
        }
        assertEquals(3,observed.path("bindings").size());
        for(var binding:observed.path("bindings"))assertTrue(binding.path("preflightBeforePreparation").asBoolean(),binding.path("name").asText());
    }
    @Test void missingOldAliasAndInterruptedToolsCannotPassOrMaskOriginalCauses(ComponentInputs inputs) throws Exception {
        var observed=fixture("failures",inputs);
        assertEquals(14,observed.path("rows").size(),"Four missing tools, four bad capabilities and six process failures");
        for(var row:observed.path("rows")) {
            assertTrue(row.path("refused").asBoolean(),row.path("name").asText());
            if(row.has("sourceBound"))assertTrue(row.path("sourceBound").asBoolean(),"Source-bound availability diagnostic");
            if(row.has("disposedOnce"))assertTrue(row.path("disposedOnce").asBoolean(),"One disposal on every process failure");
            assertFalse(row.path("cause").asText().isBlank(),"Original refusal and nested causes remain visible");
        }
    }
    private static JsonNode fixture(String selection,ComponentInputs inputs) throws Exception {
        var root=GovernanceProject.locateProjectRoot();
        inputs.add("Precondition","PowerShell fixture with controlled application resolution, version responses and process lifecycle; actual three launcher sources. Java 17/21, missing tools, old/alias responses, refusal, read failure, timeout, kill and dispose failure. No browser, build, device, sleeps or screenshots.");
        var result=CommandProcess.execute(List.of("pwsh","-NoProfile","-File",root.resolve("atf-tests/src/test/resources/toolchain/browser-toolchain-contract.ps1").toString(),"-Root",root.toString(),"-Case",selection),root,Map.of(),Duration.ofSeconds(45),true);
        String output=new String(result.stdout(),StandardCharsets.UTF_8);
        inputs.add("Original controlled process output",output+"\nSTDERR\n"+result.stderr());
        assertEquals(0,result.exitCode(),"PowerShell contract completed: "+output);
        var observed=new ObjectMapper().readTree(output);assertEquals("PASS",observed.path("status").asText());return observed;
    }
}
