package atf.tests.execution.recovery;

import atf.android.evidence.component.ComponentInputs;
import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class InvocationRecoveryComponentTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @TempDir Path temporary;
    @Test void retryRunsOnlyFailedAndAbortedIdsAndRepublishesOriginalComparisons(ComponentInputs inputs) throws Exception {
        var fixture=prepare(RecoveryFixture.ControlledSuite.class);
        var first=execute(fixture,"initial",null);assertNotEquals(0,first.exitCode(),"The original controlled suite must actually fail");
        Path initial=temporary.resolve("initial/group");byte[] original=Files.readAllBytes(initial.resolve("component-report.html"));
        Files.writeString(temporary.resolve("allow-pass"),"enabled");
        var next=execute(fixture,"retry",initial);inputs.add("Original and retry child outcomes",first.exitCode()+" -> "+next.exitCode());
        inputs.add("Original child output",new String(first.stdout(),StandardCharsets.UTF_8)+first.stderr());
        inputs.add("Retry child output",new String(next.stdout(),StandardCharsets.UTF_8)+next.stderr());
        assertEquals(0,next.exitCode(),"Only failed/aborted invocations rerun and final coverage reconciles");
        var result=JSON.readTree(Files.readAllBytes(temporary.resolve("retry/group/execution.json")));
        assertEquals(6,result.path("tests").asInt(),"The population is the original six identities, not the retry count");
        assertEquals(3,result.at("/recovery/reusedInvocationIds").size(),"Three successful bodies are inherited");
        assertEquals(3,result.at("/recovery/executedInvocationIds").size(),"Two failed bodies and one abort rerun");
        for(String id:List.of("pass","nested","parameter-0")) assertEquals(1,Files.readAllLines(temporary.resolve(id+".count")).size(),"Already passing body never runs twice: "+id);
        for(String id:List.of("failure","abort","parameter-1")) assertEquals(2,Files.readAllLines(temporary.resolve(id+".count")).size(),"Only unresolved body runs again: "+id);
        assertEquals(new String(original,StandardCharsets.UTF_8),Files.readString(initial.resolve("component-report.html")),"The rejected original report is immutable");
        String html=Files.readString(temporary.resolve("retry/group/component-report.html"));
        var rows=JSON.convertValue(result.path("invocations"),new com.fasterxml.jackson.core.type.TypeReference<List<ComponentReportCoverage.Invocation>>(){});
        assertEquals(List.of(),ComponentReportCoverage.violations(rows,html),"All original identities have one final receipt and original comparisons");
        assertTrue(html.contains("retained value") && html.contains("REUSED_ORIGINAL_COMPARISONS"),"Inherited values and their historical origin remain visible");
        assertTrue(html.contains("Original attempt (immutable)") && html.contains("Retry execution (immutable)"),"Canonical Extent retains both immutable attempt identities without truncation");
        String index=Files.readString(temporary.resolve("retry/group/README.md"));
        assertTrue(index.contains("](attempts/original-report.html)") && index.contains("](attempts/retry-report.html)"),"The accompanying index links both reports with relative paths");
        inputs.add("Reconciled invocation outcomes and lineage",JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
    @Test void interruptedStaticPopulationResumesTheMissingBodiesFromDurableCaptures(ComponentInputs inputs) throws Exception {
        var source=prepare(RecoveryFixture.InterruptedSuite.class);
        var first=execute(source,"initial",null);assertEquals(23,first.exitCode(),"The first child really halts before publication");
        Path initial=temporary.resolve("initial/group");
        assertFalse(Files.exists(initial.resolve("component-report.html")),"Interrupted execution cannot invent a completed original report");
        Files.writeString(temporary.resolve("allow-pass"),"enabled");
        var next=execute(source,"retry",initial);inputs.add("Interrupted child output",new String(next.stdout(),StandardCharsets.UTF_8)+next.stderr());
        assertEquals(0,next.exitCode(),"A durable completed capture and known static population permit bounded resumption");
        assertEquals(1,Files.readAllLines(temporary.resolve("completed.count")).size(),"Completed body never reruns");
        assertEquals(2,Files.readAllLines(temporary.resolve("interrupted.count")).size(),"Interrupted body retries");
        assertEquals(1,Files.readAllLines(temporary.resolve("pending.count")).size(),"Unexecuted body runs once");
        String report=Files.readString(temporary.resolve("retry/group/component-report.html"));
        assertTrue(report.contains("Original captured prefix (republication)"),"Rebuilt prefix is labeled as republication rather than original completed HTML");
        inputs.add("Original interruption checkpoint",Files.readString(initial.resolve("invocation-checkpoint.json")));
    }
    @Test void aFailedRetryStaysFailedUntilOnlyItsRemainingIdentitiesPass(ComponentInputs inputs) throws Exception {
        var source=prepare(RecoveryFixture.ControlledSuite.class);
        execute(source,"initial",null);
        Path initial=temporary.resolve("initial/group");
        var failed=execute(source,"retry-fail",initial);assertNotEquals(0,failed.exitCode(),"Inherited PASS bodies never hide another failed attempt");
        Files.writeString(temporary.resolve("allow-pass"),"enabled");
        var passed=execute(source,"retry-pass",temporary.resolve("retry-fail/group"));
        inputs.add("Final attempt output",new String(passed.stdout(),StandardCharsets.UTF_8)+passed.stderr());
        assertEquals(0,passed.exitCode(),"A later attempt reconciles its own checkpoint without rerunning successful siblings");
        assertEquals(1,Files.readAllLines(temporary.resolve("pass.count")).size(),"An original successful body stays single across three attempts");
        assertEquals(3,Files.readAllLines(temporary.resolve("failure.count")).size(),"Only the unresolved body executes in each attempt");
    }
    @Test void changedSourcesUnknownPopulationsAndTamperedEvidenceAreRefused(ComponentInputs inputs) throws Exception {
        var source=prepare(RecoveryFixture.ControlledSuite.class);execute(source,"initial",null);
        Path initial=temporary.resolve("initial/group"),checkpoint=initial.resolve("invocation-checkpoint.json");
        byte[] original=Files.readAllBytes(checkpoint),provenance=Files.readAllBytes(initial.getParent().resolve("source-provenance.json"));
        var observations=new ArrayList<String>();
        for(String change:List.of("schema","suite","provenance","source","release","context","budget","unfinished","owner-failure","duplicate","unknown-outcome","missing-capture")) {
            var edited=(com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(original);
            switch(change) {
                case "schema" -> edited.put("schemaVersion",99);
                case "suite" -> edited.put("suite","foreign.Suite");
                case "provenance" -> edited.put("provenanceSha256","0".repeat(64));
                case "source" -> ((com.fasterxml.jackson.databind.node.ObjectNode)edited.get("sourceContext")).put("sourceFingerprint","c".repeat(64));
                case "release" -> ((com.fasterxml.jackson.databind.node.ObjectNode)edited.get("sourceContext")).put("releaseFingerprint","c".repeat(64));
                case "context" -> ((com.fasterxml.jackson.databind.node.ObjectNode)edited.get("sourceContext")).putObject("qualificationContext").put("foreign",true);
                case "budget" -> ((com.fasterxml.jackson.databind.node.ObjectNode)edited.get("sourceContext")).put("inputPartCharacters",2);
                case "unfinished" -> edited.put("populationComplete",false);
                case "owner-failure" -> edited.putArray("containerFailures").add("Original publication failure");
                case "duplicate" -> ((com.fasterxml.jackson.databind.node.ArrayNode)edited.get("invocations")).add(edited.get("invocations").get(0).deepCopy());
                case "unknown-outcome" -> ((com.fasterxml.jackson.databind.node.ObjectNode)edited.get("invocations").get(0)).put("outcome","UNKNOWN");
                case "missing-capture" -> {for(var row:edited.get("invocations")) if(row.path("outcome").asText().equals("PASS")) ((com.fasterxml.jackson.databind.node.ObjectNode)row).remove("captureSha256");}
                default -> throw new AssertionError(change);
            }
            JSON.writeValue(checkpoint.toFile(),edited);
            assertThrows(java.io.IOException.class,() -> InvocationRecovery.admit(initial,RecoveryFixture.ControlledSuite.class.getName(),provenance));
            observations.add(change+": REFUSED");Files.write(checkpoint,original);
        }
        var plan=InvocationRecovery.admit(initial,RecoveryFixture.ControlledSuite.class.getName(),provenance);
        assertThrows(java.io.IOException.class,() -> plan.reconcile(List.of()));
        assertThrows(java.io.IOException.class,() -> plan.reconcile(List.of(new ComponentReportCoverage.Invocation("foreign","PASS"))));
        assertThrows(java.io.IOException.class,() -> plan.reconcile(List.of(new ComponentReportCoverage.Invocation(plan.retry().getFirst(),"PASS"),new ComponentReportCoverage.Invocation(plan.retry().getFirst(),"PASS"))));
        Path capture=initial.resolve("captures").resolve(atf.android.evidence.component.snapshot.ComponentCaptureJournal.name(plan.reused().getFirst().id()));
        byte[] saved=Files.readAllBytes(capture);Files.writeString(capture,"{}");
        assertThrows(java.io.IOException.class,() -> InvocationRecovery.admit(initial,RecoveryFixture.ControlledSuite.class.getName(),provenance));Files.write(capture,saved);
        Path report=initial.resolve("component-report.html");byte[] html=Files.readAllBytes(report);Files.writeString(report,"<html>changed</html>");
        assertThrows(java.io.IOException.class,() -> InvocationRecovery.admit(initial,RecoveryFixture.ControlledSuite.class.getName(),provenance));Files.write(report,html);
        inputs.add("Independent rejection observations",String.join("\n",observations)+"\nmissing/foreign/duplicate retry, altered capture, altered HTML: REFUSED");
    }
    private Class<?> selected;
    @Test void successfulOwnerReusePreservesItsOriginalSealedAttemptArtifacts(ComponentInputs inputs) throws Exception {
        Path root=atf.tests.governance.support.GovernanceProject.locateProjectRoot();
        String result=atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runNode(List.of("node","--test","--test-name-pattern","recovery preserves",
                root.resolve("atf-tests/tools/prepare-component-reuse.test.mjs").toString()),root,Duration.ofSeconds(30),value -> inputs.add("Original reuse and rejection observations",value));
        assertTrue(result.contains("Recovery artifact observations:"),"The real reuse copier verifies every retained attempt artifact");
    }
    @Test void currentSuiteDiscoveryRefusesMissingAndForeignOriginalIdentities(ComponentInputs inputs) throws Exception {
        var source=prepare(RecoveryFixture.ControlledSuite.class);execute(source,"initial",null);
        Path previous=temporary.resolve("initial/group");
        var plan=InvocationRecovery.admit(previous,selected.getName(),Files.readAllBytes(previous.getParent().resolve("source-provenance.json")));
        try(var session=org.junit.platform.launcher.core.LauncherFactory.openSession()) {
            var population=session.getLauncher().discover(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                    .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(selected))
                    .filters(org.junit.platform.launcher.EngineFilter.includeEngines("junit-platform-suite")).build());
            plan.validatePopulation(population);
            var missing=plan.original().stream().filter(row -> !row.id().contains("alwaysPass()")).toList();
            assertThrows(java.io.IOException.class,()->new InvocationRecovery.Plan(missing,plan.captured(),plan.reused(),plan.retry(),
                    plan.checkpointSha256(),plan.originalReportSha256(),previous).validatePopulation(population));
            var foreign=new ArrayList<>(plan.original());
            foreign.add(new ComponentReportCoverage.Invocation("[engine:junit-platform-suite]/[suite:"+selected.getName()+"]/"
                    +"[engine:junit-jupiter]/[class:foreign.Case]/[method:unknown()]","NOT_EXECUTED"));
            assertThrows(java.io.IOException.class,()->new InvocationRecovery.Plan(foreign,plan.captured(),plan.reused(),plan.retry(),
                    plan.checkpointSha256(),plan.originalReportSha256(),previous).validatePopulation(population));
        }
        inputs.add("Discovery admission","The actual current suite admits the six original unique IDs; dropping a static case or adding an unrelated class is refused before dispatch.");
    }
    private Path prepare(Class<?> suite) throws Exception {
        selected=suite;
        Path source=temporary.resolve("source");Files.createDirectories(source.resolve("config"));
        JSON.writeValue(source.resolve("config/atf-test-suites.json").toFile(),Map.of("suites",List.of(Map.of("suite",suite.getName(),"role","primary","layer","component"))));
        return source;
    }
    private CommandProcess.Output execute(Path source,String attempt,Path previous) throws Exception {
        Path run=temporary.resolve(attempt);Files.createDirectory(run);
        var provenance=Map.of("kind","live-source-components","apkQualification",false,"sourceFingerprint","a".repeat(64),"releaseFingerprint","b".repeat(64),
                "qualificationContext",Map.of("sourceSha256","a".repeat(64),"releaseSha256","b".repeat(64)),"inputPartCharacters",16384,"observedUtc",attempt);
        Path file=run.resolve("source-provenance.json");JSON.writeValue(file.toFile(),provenance);
        var command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin/java.exe").toString(),"-Xmx384m","-XX:ActiveProcessorCount=2",
                "-Datf.recovery.fixture="+temporary,"-Djava.io.tmpdir="+temporary,"-cp",System.getProperty("java.class.path"),
                "atf.tests.execution.components.ComponentSuiteExecution",selected.getName(),run.resolve("group").toString(),file.toString(),source.toString()));
        if(previous!=null) command.add(previous.toString());
        return CommandProcess.execute(command,temporary,Map.of(),Duration.ofSeconds(90),true);
    }
}
