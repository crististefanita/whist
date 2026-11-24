package atf.tests.execution.recovery;

import atf.android.evidence.component.snapshot.CapturedInvocation;
import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.tests.reporting.aggregate.ComponentReportCoverage.Invocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Admission precedes selection; only complete, source-bound PASS observations may be carried forward. */
public final class InvocationRecovery {
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public record Plan(List<Invocation> original,List<CapturedInvocation> captured,List<CapturedInvocation> reused,
                       List<String> retry,String checkpointSha256,String originalReportSha256,Path previous) {
        public Plan { original=List.copyOf(original);captured=List.copyOf(captured);reused=List.copyOf(reused);retry=List.copyOf(retry); }
        public void validatePopulation(org.junit.platform.launcher.TestPlan current) throws IOException {
            var known=new LinkedHashMap<String,org.junit.platform.launcher.TestIdentifier>();
            for(var root:current.getRoots()) for(var node:current.getDescendants(root)) known.put(node.getUniqueId(),node);
            var expected=new HashSet<String>();original.forEach(row -> expected.add(row.id()));
            for(var node:known.values()) if(node.isTest() && !expected.contains(node.getUniqueId())) throw invalid("MISSING_IDENTITY",node.getUniqueId());
            for(var row:original) {
                var id=org.junit.platform.engine.UniqueId.parse(row.id());
                if(known.containsKey(id.toString())) continue;
                while(!id.getSegments().isEmpty() && !known.containsKey(id.toString())) {
                    if(id.getSegments().size()==1) break;
                    id=id.removeLastSegment();
                }
                var parent=known.get(id.toString());
                if(parent==null || parent.isTest() || !(parent.getSource().orElse(null) instanceof org.junit.platform.engine.support.descriptor.MethodSource))
                    throw invalid("FOREIGN_IDENTITY",row.id());
            }
        }
        public List<Invocation> reconcile(List<Invocation> executed) throws IOException {
            var fresh=new LinkedHashMap<String,String>();
            for(var row:executed) if(fresh.put(row.id(),row.outcome())!=null) throw invalid("DUPLICATE_RETRY",row.id());
            if(!fresh.keySet().equals(new LinkedHashSet<>(retry))) throw invalid("RETRY_POPULATION",fresh.keySet().toString());
            var pass=new HashSet<String>(); reused.forEach(row -> pass.add(row.id()));
            return original.stream().map(row -> new Invocation(row.id(),pass.contains(row.id()) ? "PASS" : fresh.get(row.id()))).toList();
        }
    }
    public static Plan admit(Path previous,String suite,byte[] currentProvenance) throws IOException {
        Path checkpoint=previous.resolve("invocation-checkpoint.json"); ComponentCaptureJournal.direct(checkpoint);
        byte[] checkpointBytes=Files.readAllBytes(checkpoint);
        JsonNode prior=JSON.readTree(checkpointBytes),current=JSON.readTree(currentProvenance);
        if(prior.path("schemaVersion").asInt()!=1 || !prior.path("kind").asText().equals("COMPONENT_INVOCATION_CHECKPOINT")) throw invalid("SCHEMA","");
        if(!prior.path("invocations").isArray() || !prior.path("containerFailures").isArray() || !prior.path("populationComplete").isBoolean()) throw invalid("SCHEMA","");
        if(!prior.path("suite").asText().equals(suite)) throw invalid("SUITE",suite);
        JsonNode source=prior.required("sourceContext");
        Path originalProvenance=previous.getParent().resolve("source-provenance.json");ComponentCaptureJournal.direct(originalProvenance);
        byte[] oldProvenance=Files.readAllBytes(originalProvenance);
        if(!prior.required("provenanceSha256").asText().equals(ComponentCaptureJournal.sha256(oldProvenance))
                || !source.equals(JSON.readTree(oldProvenance))) throw invalid("PROVENANCE","");
        for(String field:List.of("sourceFingerprint","releaseFingerprint","qualificationContext","inputPartCharacters"))
            if(!source.hasNonNull(field) || !source.get(field).equals(current.get(field))) throw invalid("SOURCE",field);
        if(!prior.path("populationComplete").asBoolean() || !prior.required("containerFailures").isEmpty()) throw invalid("INCOMPLETE_POPULATION","");
        var original=new ArrayList<Invocation>();var captured=new ArrayList<CapturedInvocation>();var reused=new ArrayList<CapturedInvocation>();var retry=new ArrayList<String>();
        var ids=new LinkedHashSet<String>();
        for(var row:prior.required("invocations")) {
            String id=row.required("id").asText(),outcome=row.required("outcome").asText();
            if(!row.required("id").isTextual() || !row.required("outcome").isTextual() || id.isBlank() || !ids.add(id) || !Set.of("PASS","FAIL","ABORTED","NOT_EXECUTED").contains(outcome)) throw invalid("IDENTITY",id);
            try { org.junit.platform.engine.UniqueId.parse(id); } catch(RuntimeException failure) {throw invalid("IDENTITY",id);}
            original.add(new Invocation(id,outcome));
            CapturedInvocation value=null;
            if(row.hasNonNull("captureSha256")) {
                value=ComponentCaptureJournal.read(previous.resolve("captures"),id,row.get("captureSha256").asText());
                if(!value.outcome().name().equals(outcome)) throw invalid("OUTCOME",id);
                captured.add(value);
            }
            if(outcome.equals("PASS")) {
                if(value==null || value.phases().stream().noneMatch(p -> p.phase()==atf.testkit.assertions.ComponentAssertionAudit.Phase.TEST_BODY && !p.checks().isEmpty())
                        || value.phases().stream().anyMatch(p -> p.outcome()!=atf.testkit.assertions.ComponentAssertionAudit.Outcome.PASS || p.checks().stream().anyMatch(c -> !c.passed())))
                    throw invalid("PASS_WITHOUT_COMPARISONS",id);
                reused.add(value);
            } else retry.add(id);
        }
        if(ids.isEmpty()) throw invalid("EMPTY_POPULATION","");
        ComponentCaptureJournal.direct(previous.resolve("captures"));
        if(Files.exists(previous.resolve("captures"))) try(var files=Files.list(previous.resolve("captures"))) {
            Set<String> expected=new HashSet<>(); captured.forEach(row -> expected.add(ComponentCaptureJournal.name(row.id())));
            Set<String> actual=new HashSet<>();files.forEach(path -> actual.add(path.getFileName().toString()));
            if(!expected.equals(actual)) throw invalid("FOREIGN_CAPTURE",actual.toString());
        }
        String reportHash="";
        Path report=previous.resolve("component-report.html");
        if(Files.exists(report)) {
            ComponentCaptureJournal.direct(report); byte[] reportBytes=Files.readAllBytes(report);reportHash=ComponentCaptureJournal.sha256(reportBytes);
            var errors=atf.tests.reporting.aggregate.ComponentReportCoverage.violations(captured.stream().map(row -> new Invocation(row.id(),row.outcome().name())).toList(),new String(reportBytes,java.nio.charset.StandardCharsets.UTF_8));
            if(!errors.isEmpty()) throw invalid("ORIGINAL_REPORT",errors.toString());
            Path execution=previous.resolve("execution.json"); ComponentCaptureJournal.direct(execution);
            var receipt=JSON.readTree(Files.readAllBytes(execution));
            if(!receipt.required("reportSha256").asText().equals(reportHash)
                    || !receipt.required("invocationCheckpointSha256").asText().equals(ComponentCaptureJournal.sha256(checkpointBytes))) throw invalid("ORIGINAL_SEAL","");
        }
        return new Plan(original,captured,reused,retry,ComponentCaptureJournal.sha256(checkpointBytes),reportHash,previous);
    }
    public static IOException invalid(String code,String detail) {return new IOException("COMPONENT_INVOCATION_RECOVERY_"+code+": "+detail);}
    private InvocationRecovery() { }
}
