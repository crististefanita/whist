package atf.tests.reporting.acceptance;

import atf.android.evidence.component.ComponentInputs;
import atf.android.evidence.component.publication.ComponentReportPublication;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class ComponentPublicationComponentTest {
    @TempDir Path root;
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test void verificationMustPassBeforeCanonicalReplacement(ComponentInputs inputs) throws Exception {
        Path report=Files.writeString(root.resolve("report.html"),"previous qualified report");
        inputs.add("Preconditions","Existing canonical report; real private files; verifier receives the staged path, not the canonical path.");
        ComponentReportPublication.publish(report,1024, pending -> Files.writeString(pending,"new complete report"),
                pending -> { if(pending.equals(report)||!Files.readString(pending).equals("new complete report")) throw new IOException("Wrong staged bytes"); });
        assertEquals("new complete report",Files.readString(report));
        var receipt=receipt(report); inputs.add("Original publication stages",receipt.toPrettyString());
        assertEquals("PASS",receipt.path("status").asText());
        assertEquals(List.of("RENDER_AND_FINALIZE","VERIFY","PROMOTE"),
                JSON.convertValue(receipt.path("stages").findValues("name"),new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}));
        assertTrue(receipt.path("sha256").asText().matches("[a-f0-9]{64}"),"Streaming seal identifies the accepted bytes");
    }
    @Test void failedVerificationLimitsAndOriginalErrorsPreserveThePreviousReportAndJUnit(ComponentInputs inputs) throws Exception {
        inputs.add("Negative preconditions","Existing qualified HTML and unchanged JUnit XML; controlled render, verification, byte-limit, mutation and heap failures.");
        Path xml=Files.writeString(root.resolve("TEST-original.xml"),"<testsuite tests='1' failures='1'/>");
        for(String mode:List.of("render","verify","limit","mutate","heap")){
            Path report=Files.writeString(root.resolve(mode+".html"),"qualified");
            var cause=new IOException("Original "+mode);var heap=new OutOfMemoryError("Controlled renderer failure, not real heap exhaustion");
            var failure=assertThrows(Throwable.class,()->ComponentReportPublication.publish(report,mode.equals("limit")?1:1024,
                    staged->{Files.writeString(staged,"candidate");if(mode.equals("render"))throw cause;if(mode.equals("heap"))throw heap;},
                    staged->{if(mode.equals("verify"))throw cause;if(mode.equals("mutate"))Files.writeString(staged,"changed");}));
            if(mode.equals("render")||mode.equals("verify"))assertSame(cause,failure);
            if(mode.equals("heap"))assertSame(heap,failure);
            if(mode.equals("limit")||mode.equals("mutate")){
                assertEquals(IOException.class,failure.getClass(),"The intended guard, not an unrelated error, refuses the candidate");
                assertTrue(failure.getMessage().startsWith(mode.equals("limit")?"COMPONENT_REPORT_BYTE_LIMIT":"COMPONENT_REPORT_CHANGED_DURING_VERIFICATION"),
                        "The refusal identifies its exact boundary");
            }
            assertEquals("qualified",Files.readString(report),"A failed publication cannot replace prior accepted bytes");
            assertEquals("<testsuite tests='1' failures='1'/>",Files.readString(xml),"Publication never rewrites original JUnit outcomes");
            var receipt=receipt(report);inputs.add("Original refusal — "+mode,receipt.toPrettyString());
            assertEquals("FAIL",receipt.path("status").asText());
            assertEquals(mode.equals("render")||mode.equals("heap")?"RENDER_AND_FINALIZE":"VERIFY",receipt.path("failedStage").asText());
            assertTrue(Files.isRegularFile(Path.of(receipt.path("staged").asText())),"Available candidate evidence remains inspectable after failure");
        }
    }
    private static com.fasterxml.jackson.databind.JsonNode receipt(Path file) throws IOException {
        return JSON.readTree(file.resolveSibling(file.getFileName()+".publication.json").toFile());
    }
}
