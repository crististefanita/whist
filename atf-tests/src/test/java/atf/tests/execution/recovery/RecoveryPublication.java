package atf.tests.execution.recovery;

import atf.android.evidence.component.AndroidComponentRun;
import atf.android.evidence.component.ComponentInputs;
import atf.android.evidence.component.snapshot.CapturedInvocation;
import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.model.ReportObservation;
import atf.tests.reporting.aggregate.ComponentReportCoverage.Invocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Republishes neutral original comparisons through Reporting; original and retry HTML remain immutable. */
public final class RecoveryPublication {
    public static Map<String,Object> publish(InvocationRecovery.Plan plan,List<Invocation> executed,Path output) throws IOException {
        var directory=output.resolve("attempts");Files.createDirectory(directory);
        var artifacts=new ArrayList<ReportObservation.Artifact>();
        Path original=directory.resolve("original-report.html");
        if(!plan.originalReportSha256().isBlank()) Files.copy(plan.previous().resolve("component-report.html"),original);
        else if(!plan.captured().isEmpty()) {
            String current=System.getProperty(atf.android.evidence.component.AndroidComponentReports.PROVENANCE);
            try {
                System.setProperty(atf.android.evidence.component.AndroidComponentReports.PROVENANCE,Files.readString(plan.previous().getParent().resolve("source-provenance.json")));
                render(original,plan.captured(),List.of());
            } finally {
                if(current==null) System.clearProperty(atf.android.evidence.component.AndroidComponentReports.PROVENANCE);
                else System.setProperty(atf.android.evidence.component.AndroidComponentReports.PROVENANCE,current);
            }
        }
        if(Files.exists(original)) artifacts.add(reference(plan.originalReportSha256().isBlank() ? "Original captured prefix (republication)" : "Original attempt (immutable)",original));
        Path retry=output.resolve("component-report.html");
        if(Files.exists(retry)) {
            Path target=directory.resolve("retry-report.html");Files.move(retry,target);artifacts.add(reference("Retry execution (immutable)",target));
        }
        Files.copy(plan.previous().resolve("invocation-checkpoint.json"),directory.resolve("original-checkpoint.json"));
        var all=new LinkedHashMap<String,CapturedInvocation>();
        var journal=new ComponentCaptureJournal(output.resolve("captures"));
        for(var row:plan.reused()) {
            var inputs=new ArrayList<>(row.inputs());
            inputs.add(new ComponentInputs.Entry("Original invocation recovery",new ObjectMapper().writeValueAsString(Map.of(
                    "originalCheckpointSha256",plan.checkpointSha256(),"originalReportSha256",plan.originalReportSha256(),
                    "mode","REUSED_ORIGINAL_COMPARISONS","originalInvocationId",row.id()))));
            var inherited=new CapturedInvocation(row.identity(),row.scenarioName(),inputs,row.phases(),row.outcome());
            journal.append(inherited);all.put(row.id(),inherited);
        }
        for(var row:executed) {
            Path file=output.resolve("captures").resolve(ComponentCaptureJournal.name(row.id()));
            var capture=ComponentCaptureJournal.read(file.getParent(),row.id(),ComponentCaptureJournal.sha256(Files.readAllBytes(file)));
            if(!capture.outcome().name().equals(row.outcome())) throw InvocationRecovery.invalid("RETRY_CAPTURE",row.id());
            if(all.put(row.id(),capture)!=null) throw InvocationRecovery.invalid("DUPLICATE_RETRY",row.id());
        }
        var ordinals=new HashMap<String,Integer>();var ordered=new ArrayList<CapturedInvocation>();
        for(var row:plan.original()) {
            var value=Objects.requireNonNull(all.get(row.id()));
            String key=value.identity().testPackage()+"/"+value.identity().testClass()+"/"+value.identity().method();
            int ordinal=ordinals.merge(key,1,Integer::sum);
            ordered.add(new CapturedInvocation(value.identity(),value.identity().method()+" ["+ordinal+"]",value.inputs(),value.phases(),value.outcome()));
        }
        render(retry,ordered,artifacts);
        var evidence=new ArrayList<Map<String,String>>();
        for(var artifact:artifacts) evidence.add(Map.of("path",output.relativize(Path.of(artifact.location())).toString().replace('\\','/'),"sha256",artifact.sha256()));
        evidence.add(Map.of("path","attempts/original-checkpoint.json","sha256",plan.checkpointSha256()));
        String originalLink=Files.exists(original) ? "[Original observations](attempts/original-report.html) · " : "";
        String retryLink=Files.exists(directory.resolve("retry-report.html")) ? "[Retry execution](attempts/retry-report.html) · " : "";
        Files.writeString(output.resolve("README.md"),"# Component invocation recovery\n\n## Overview\n\n"
                +"The [canonical Extent](component-report.html) contains every admitted original comparison and fresh retry outcome. "
                +"It is autonomous; immutable attempt identities and SHA-256 values are captured inside it.\n\n"
                +originalLink+retryLink
                +"[Original checkpoint](attempts/original-checkpoint.json) · [Final execution](execution.json)\n\n"
                +"A captured prefix is explicitly a republication when interruption prevented original HTML publication. "
                +"The original failure is never edited or presented as an original PASS.\n");
        evidence.add(Map.of("path","README.md","sha256",ComponentCaptureJournal.sha256(Files.readAllBytes(output.resolve("README.md")))));
        return Map.of("schemaVersion",1,"originalCheckpointSha256",plan.checkpointSha256(),
                "reusedInvocationIds",plan.reused().stream().map(CapturedInvocation::id).toList(),
                "executedInvocationIds",executed.stream().map(Invocation::id).toList(),"artifacts",evidence);
    }
    private static ReportObservation.Artifact reference(String label,Path file) throws IOException {
        return new ReportObservation.Artifact(label,file.toUri(),ComponentCaptureJournal.sha256(Files.readAllBytes(file)));
    }
    private static void render(Path file,List<CapturedInvocation> values,List<ReportObservation.Artifact> artifacts) {
        try(var run=new AndroidComponentRun(file,false,AtfReportConfigurationLoader.load().reportingSettings())) {
            run.includeCaptured(values,artifacts);
        }
    }
    private RecoveryPublication() { }
}
