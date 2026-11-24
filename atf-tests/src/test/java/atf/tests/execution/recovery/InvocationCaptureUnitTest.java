package atf.tests.execution.recovery;

import atf.android.evidence.component.ComponentInputs;
import atf.android.evidence.component.snapshot.CapturedInvocation;
import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.reporting.audit.AssertionAuditReport;
import atf.testkit.AtfUnitTest;
import atf.testkit.assertions.ComponentAssertionAudit;
import atf.testkit.assertions.RecordedAssertions;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class InvocationCaptureUnitTest {
    @TempDir Path directory;
    @Test void neutralBytesPreserveUnicodeComparisonsAndOriginalTimingWithoutExecutableObjects() throws Exception {
        String id="[engine:junit-jupiter]/[class:Controlled]/[method:preserved()]",value="♠𐐀\n"+"complete-tail".repeat(60);
        var record=new CapturedInvocation(new AssertionAuditReport.Identity("Controlled","test","display","preserved",List.of("component")),"preserved [1]",
                List.of(new ComponentInputs.Entry("JUnit invocation identity",id),new ComponentInputs.Entry("Full original value",value)),
                List.of(new CapturedInvocation.Phase(ComponentAssertionAudit.Phase.TEST_BODY,List.of(new RecordedAssertions.Check("Exact Unicode",value,value,true)),
                        ComponentAssertionAudit.Outcome.PASS,"",13L,2)),ComponentAssertionAudit.Outcome.PASS);
        new ComponentCaptureJournal(directory).append(record);
        Path file=directory.resolve(ComponentCaptureJournal.name(id));String hash=ComponentCaptureJournal.sha256(Files.readAllBytes(file));
        var restored=ComponentCaptureJournal.read(directory,id,hash);
        assertEquals(record,restored);assertEquals(value,restored.result().phases().getFirst().checks().getFirst().actual());
        assertEquals(13L,restored.result().phases().getFirst().timing().orElseThrow().elapsed().toNanos());
        assertEquals(2,restored.result().phases().getFirst().timing().orElseThrow().intervals());
        assertNull(restored.result().failure());assertFalse(restored.result().propagate());
        assertThrows(java.io.IOException.class,()->new ComponentCaptureJournal(directory).append(record));
        assertEquals(hash,ComponentCaptureJournal.sha256(Files.readAllBytes(file)));
    }
    @Test void checkpointWriteFailuresRemainFatalEvenIfJUnitWouldOnlyLogListenerFailures() throws Exception {
        Path blocked=directory.resolve("not-a-directory");Files.writeString(blocked,"owned fixture");
        var checkpoint=new InvocationCheckpoint(blocked,"controlled", "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var error=assertThrows(java.io.UncheckedIOException.class,()->checkpoint.save(new atf.tests.execution.components.ComponentExecutionListener()));
        var retained=assertThrows(java.io.IOException.class,checkpoint::requireSaved);
        assertSame(error.getCause(),retained.getCause());assertTrue(retained.getMessage().contains("CHECKPOINT_WRITE"));
    }
    @Test void interruptionBeforeAnyCaptureKeepsEveryKnownIdentityPending() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        byte[] provenance=json.writeValueAsBytes(java.util.Map.of("kind","live-source-components","apkQualification",false,
                "sourceFingerprint","a".repeat(64),"releaseFingerprint","b".repeat(64),"qualificationContext",java.util.Map.of("source","fixed"),"inputPartCharacters",16384));
        Files.write(directory.resolve("source-provenance.json"),provenance);Path previous=Files.createDirectory(directory.resolve("group"));
        String id="[engine:junit-jupiter]/[class:Controlled]/[method:pending()]";
        var checkpoint=java.util.Map.of("schemaVersion",1,"kind","COMPONENT_INVOCATION_CHECKPOINT","suite","controlled.Suite",
                "provenanceSha256",ComponentCaptureJournal.sha256(provenance),"sourceContext",json.readTree(provenance),
                "populationComplete",true,"containerFailures",List.of(),"invocations",List.of(java.util.Map.of("id",id,"outcome","NOT_EXECUTED")));
        json.writeValue(previous.resolve("invocation-checkpoint.json").toFile(),checkpoint);
        var plan=InvocationRecovery.admit(previous,"controlled.Suite",provenance);
        assertEquals(List.of(id),plan.retry());assertTrue(plan.reused().isEmpty());assertTrue(plan.captured().isEmpty());
        assertFalse(Files.exists(previous.resolve("captures")));
    }
}
