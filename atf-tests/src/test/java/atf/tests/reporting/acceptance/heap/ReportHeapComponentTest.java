package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class ReportHeapComponentTest {
    @TempDir Path root;
    @Test void boundedSmallMediumAndLargeReportsKeepEveryComparisonUnderTheDeclaredHeap(ComponentInputs inputs) throws Exception {
        var json=new ObjectMapper();var source=GovernanceProject.locateProjectRoot();
        var profile=json.readTree(source.resolve("config/report-heap-profile.json").toFile());
        inputs.add("Declared resource profile",profile.toPrettyString());
        assertEquals("WHIST_REPORT_HEAP_QUALIFICATION",profile.path("kind").asText());
        assertEquals(3,profile.path("populations").size(),"Small, medium and large populations are all declared");
        assertTrue(profile.path("childHeapMiB").asInt()>0&&profile.path("childHeapMiB").asInt()<=768,"The profile cannot silently increase the suite heap");
        assertEquals(atf.android.evidence.component.publication.ComponentReportPublication.DEFAULT_MAX_BYTES,
                profile.path("maximumReportBytes").asLong(),"The fixture qualifies the actual default artifact guard");
        inputs.add("Qualification limits","Real pinned Reporting JAR; isolated child per population; repeated synthetic PNG bytes, no product screenshots, emulator or APK. Rendering and navigation are one opaque J24 phase. Pool peaks are cumulative upper bounds.");
        int previous=0;
        for(var population:profile.path("populations")){
            assertTrue(population.canConvertToInt()&&population.asInt()>previous,"Declared small, medium and large populations are positive and strictly increasing");
            previous=population.asInt();
            Path directory=Files.createDirectory(root.resolve("population-"+population.asInt()));
            Path log=directory.resolve("child.log");String java=Path.of(System.getProperty("java.home"),"bin",
                    System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
            var process=new ProcessBuilder(java,"-Xmx"+profile.path("childHeapMiB").asInt()+"m","-XX:ActiveProcessorCount=2",
                    "-Djava.io.tmpdir="+directory,"-cp",System.getProperty("java.class.path"),BoundedReportProbe.class.getName(),
                    directory.toString(),population.asText(),profile.path("valueCharacters").asText(),profile.path("maximumReportBytes").asText())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            boolean finished;
            try { finished=process.waitFor(profile.path("timeoutSeconds").asInt(),TimeUnit.SECONDS); }
            finally { if(process.isAlive())process.destroyForcibly(); }
            if(!finished)process.waitFor(10,TimeUnit.SECONDS);
            String original=Files.readString(log);inputs.add("Original isolated child output — "+population.asInt(),original);
            assertTrue(finished,"The child remains within the declared time budget; original output is captured");
            assertEquals(0,process.exitValue(),"The declared heap completes publication and verification: "+original);
            String line=original.lines().filter(value->value.startsWith("{\"status\":\"PASS\"")).reduce((left,right)->right).orElseThrow();
            var observed=json.readTree(line);assertEquals(population.asInt(),observed.path("comparisons").asInt());
            assertEquals(1,observed.path("capturedFailures").asInt(),"The final deliberate mismatch remains a failed comparison");
            assertTrue(observed.path("profileParity").asBoolean(),"All authored comparison identities and body labels remain in the physical HTML");
            assertTrue(observed.path("mandatoryPngRetained").asBoolean(),"The original synthetic PNG remains inline after finalization");
            assertEquals(population.asInt(),observed.path("completeValuesVerified").asInt(),"A streaming independent oracle verifies each full original value across body parts");
            var publication=observed.path("publication");assertEquals("PASS",publication.path("status").asText());
            assertTrue(publication.path("maximumHeapBytes").asLong()<=profile.path("childHeapMiB").asLong()*1024*1024,"The child heap is actually bounded");
            assertEquals(3,publication.path("stages").size(),"Rendering/finalization, verification and promotion have separate receipts");
            assertTrue(publication.path("bytes").asLong()<profile.path("maximumReportBytes").asLong(),"The accepted artifact is within the declared byte limit");
        }
    }
}
