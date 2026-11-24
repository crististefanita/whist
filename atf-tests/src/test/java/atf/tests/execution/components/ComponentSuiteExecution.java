package atf.tests.execution.components;

import atf.android.evidence.component.AndroidComponentReports;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Executes one declared primary component gate and rejects an incomplete HTML instead of converting old XML. */
public final class ComponentSuiteExecution {
    public static void main(String[] args) throws Exception {
        if (args.length != 4 && args.length != 5) throw new IllegalArgumentException("suite, new external output, provenance JSON, source root and optional previous selection required");
        Path root = Path.of(args[3]).toRealPath(), output = Path.of(args[1]).toAbsolutePath().normalize();
        if (output.startsWith(root)) throw new IllegalArgumentException("Execution output must be external");
        if (Files.exists(output)) throw new IllegalArgumentException("Execution output must be new");
        var mapper = new ObjectMapper();
        var catalog = mapper.readTree(Files.readString(root.resolve("config/atf-test-suites.json")));
        boolean allowed = false;
        for (var entry : catalog.required("suites")) if (entry.path("suite").asText().equals(args[0])
                && entry.path("role").asText().equals("primary") && entry.path("layer").asText().equals("component")) allowed = true;
        if (!allowed) throw new IllegalArgumentException("Only a declared primary component gate can run here: " + args[0]);
        Files.createDirectories(output);
        byte[] provenanceBytes = Files.readAllBytes(Path.of(args[2]));
        var provenance = mapper.readTree(provenanceBytes);
        if (!provenance.path("kind").asText().equals("live-source-components") || provenance.path("apkQualification").asBoolean(true))
            throw new IllegalArgumentException("Explicit live-source provenance without APK qualification required");
        var recovery=args.length==5 ? atf.tests.execution.recovery.InvocationRecovery.admit(Path.of(args[4]),args[0],provenanceBytes) : null;
        System.setProperty("atf.governance.root", root.toString());
        System.setProperty(AndroidComponentReports.DIRECTORY, output.toString());
        System.setProperty(AndroidComponentReports.PROVENANCE, provenance.toString());
        System.setProperty(atf.android.evidence.component.snapshot.ComponentCaptureJournal.DIRECTORY,output.resolve("captures").toString());
        System.setProperty(AndroidComponentReports.INPUT_PART_CHARACTERS,
                System.getProperty(AndroidComponentReports.INPUT_PART_CHARACTERS, "16384"));
        var checkpoint=new atf.tests.execution.recovery.InvocationCheckpoint(output,args[0],provenanceBytes);
        var listener = new ComponentExecutionListener(checkpoint::save);
        var builder = LauncherDiscoveryRequestBuilder.request();
        if(recovery==null) builder.selectors(selectClass(args[0]));
        else for(var id:recovery.retry()) builder.selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId(id));
        var request = builder
                .filters(EngineFilter.includeEngines("junit-platform-suite"))
                .configurationParameter("atf.component.aggregate", "true")
                .configurationParameter("atf.test.run.scope", "suite")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
        try (var session = LauncherFactory.openSession()) {
            var launcher=session.getLauncher();
            if(recovery!=null) recovery.validatePopulation(launcher.discover(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(args[0]))
                    .filters(EngineFilter.includeEngines("junit-platform-suite")).build()));
            if(recovery==null || !recovery.retry().isEmpty()) launcher.execute(request, listener);
        }
        checkpoint.requireSaved();
        var invocations=recovery==null ? listener.invocations() : recovery.reconcile(listener.invocations());
        Map<String,Object> reconciliation=null;
        if(recovery!=null) {
            if(!listener.containerFailures().isEmpty()) throw new IllegalStateException("Retry owner failed: "+listener.containerFailures());
            if(!listener.invocations().isEmpty()) {
                List<Path> fresh;
                try(var files=Files.list(output)){fresh=files.filter(p -> p.toString().endsWith(".html")).toList();}
                if(fresh.size()!=1) throw new IllegalStateException("Retry requires exactly one original report");
                Path retryReport=output.resolve("component-report.html");Files.move(fresh.getFirst(),retryReport);
                var defects=ComponentReportCoverage.violations(listener.invocations(),Files.readString(retryReport));
                if(!defects.isEmpty()) throw new IllegalStateException("Retry HTML differs from executed observations: "+defects);
            }
            reconciliation=atf.tests.execution.recovery.RecoveryPublication.publish(recovery,listener.invocations(),output);
            checkpoint.save(invocations,true,listener.containerFailures());
            checkpoint.requireSaved();
        }
        List<Path> reports;
        try (var entries = Files.list(output)) { reports = entries.filter(p -> p.toString().endsWith(".html")).toList(); }
        var errors = new ArrayList<String>();
        if (reports.size() != 1) errors.add("Expected exactly one report, found " + reports.size());
        Path canonical = output.resolve("component-report.html");
        if (reports.size() == 1) {
            if(!reports.getFirst().equals(canonical)) Files.move(reports.getFirst(), canonical);
            errors.addAll(ComponentReportCoverage.violations(invocations, Files.readString(canonical)));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", 1);result.put("kind", "live-source-components");result.put("apkQualification", false);
        result.put("suite", args[0]);result.put("tests", invocations.size());
        var outcomes=new TreeMap<String,Long>();invocations.forEach(row -> outcomes.merge(row.outcome(),1L,Long::sum));
        result.put("outcomes", outcomes);result.put("coverageErrors", errors);
        result.put("containerFailures", listener.containerFailures());
        result.put("junitIdentitySha256", digest(mapper.writeValueAsBytes(invocations)));
        result.put("invocations", invocations);
        result.put("invocationCheckpointSha256",digest(Files.readAllBytes(output.resolve("invocation-checkpoint.json"))));
        if(reconciliation!=null) result.put("recovery",reconciliation);
        result.put("provenanceSha256", provenanceFingerprint(provenanceBytes));
        if (Files.exists(canonical)) result.put("reportSha256", digest(Files.readAllBytes(canonical)));
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution.json").toFile(), result);
        System.out.println(mapper.writeValueAsString(result));
        if (!errors.isEmpty() || invocations.isEmpty() || !listener.containerFailures().isEmpty() || invocations.stream().anyMatch(row -> !row.outcome().equals("PASS"))) throw new IllegalStateException("Component execution or exact HTML coverage failed; inspect " + output);
    }
    private static String digest(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String provenanceFingerprint(byte[] provenanceBytes) throws Exception { return digest(provenanceBytes); }
    private ComponentSuiteExecution() { }
}
