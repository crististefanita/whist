package atf.tests.execution.proofs;

import atf.android.evidence.component.AndroidComponentReports;
import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.tests.execution.components.ComponentExecutionListener;
import atf.tests.execution.recovery.InvocationCheckpoint;
import atf.tests.execution.recovery.InvocationRecovery;
import atf.tests.execution.recovery.RecoveryPublication;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

/** One static proof control uses the same neutral capture and recovery ports as primary owners. */
public final class StaticProofRun {
    public static final String PROVENANCE = "proof-provenance.json";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> IDENTITY = List.of("testChangeReview", "testChangeChecks", "testChangeCompiler");

    public static Map<String, String> execute(String control, List<String> selectors, Path output,
                                               byte[] provenance, Path previous) throws Exception {
        String suite = "testChanges:" + control;
        var recovery = previous == null ? null : InvocationRecovery.admit(previous, suite, provenance,
                previous.resolve(PROVENANCE), IDENTITY);
        Files.write(output.resolve(PROVENANCE), provenance);
        System.setProperty(AndroidComponentReports.DIRECTORY, output.toString());
        System.setProperty(AndroidComponentReports.PROVENANCE, new String(provenance, java.nio.charset.StandardCharsets.UTF_8));
        System.setProperty(ComponentCaptureJournal.DIRECTORY, output.resolve("captures").toString());
        var checkpoint = new InvocationCheckpoint(output, suite, provenance);
        var listener = new ComponentExecutionListener(checkpoint::save);
        var builder = StaticProofSelectors.request(selectors);
        Map<String, String> ids;
        try (var session = LauncherFactory.openSession()) {
            var launcher = session.getLauncher();
            var discovered = launcher.discover(builder.build());
            ids = StaticProofSelectors.identities(discovered, selectors);
            if (recovery == null) launcher.execute(discovered, listener);
            else {
                var original = new java.util.HashSet<String>();
                recovery.original().forEach(row -> original.add(row.id()));
                if (!original.equals(Set.copyOf(ids.values())))
                    throw InvocationRecovery.invalid("PROOF_POPULATION", original.toString());
                if (!recovery.retry().isEmpty()) {
                    var retry = LauncherDiscoveryRequestBuilder.request();
                    recovery.retry().forEach(id -> retry.selectors(selectUniqueId(id)));
                    launcher.execute(StaticProofSelectors.configure(retry).build(), listener);
                }
            }
        }
        checkpoint.requireSaved();
        var rows = recovery == null ? listener.invocations() : recovery.reconcile(listener.invocations());
        var errors = new ArrayList<String>();
        Map<String, Object> lineage = null;
        if (recovery != null && listener.containerFailures().isEmpty()) {
            if (!listener.invocations().isEmpty()) canonicalize(output, listener.invocations());
            lineage = RecoveryPublication.publish(recovery, listener.invocations(), output, previous.resolve(PROVENANCE));
            checkpoint.save(rows, true, listener.containerFailures());
        } else canonicalize(output, rows);
        checkpoint.requireSaved();
        Path report = output.resolve("component-report.html");
        errors.addAll(ComponentReportCoverage.violations(rows, Files.readString(report)));
        var receipt = new LinkedHashMap<String, Object>();
        receipt.put("schemaVersion", 1);
        receipt.put("kind", "testChanges-proof-attempt");
        receipt.put("suite", suite);
        receipt.put("invocations", rows);
        receipt.put("containerFailures", listener.containerFailures());
        receipt.put("coverageErrors", errors);
        receipt.put("reportSha256", ComponentCaptureJournal.sha256(Files.readAllBytes(report)));
        receipt.put("invocationCheckpointSha256", ComponentCaptureJournal.sha256(Files.readAllBytes(output.resolve("invocation-checkpoint.json"))));
        if (lineage != null) receipt.put("recovery", lineage);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("execution.json").toFile(), receipt);
        var observed = new LinkedHashMap<String, String>();
        rows.forEach(row -> observed.put(row.id(), row.outcome()));
        if (!errors.isEmpty() || !listener.containerFailures().isEmpty()
                || !observed.keySet().equals(Set.copyOf(ids.values())) || observed.values().stream().anyMatch(value -> !value.equals("PASS")))
            throw new IllegalStateException("TEST-CHANGE-PROOF-INVOCATIONS: inspect immutable attempt at " + output);
        var result = new LinkedHashMap<String, String>();
        ids.forEach((selector, id) -> result.put(selector, observed.get(id)));
        return Map.copyOf(result);
    }

    private static void canonicalize(Path output, List<ComponentReportCoverage.Invocation> rows) throws Exception {
        List<Path> reports;
        try (var files = Files.list(output)) { reports = files.filter(file -> file.toString().endsWith(".html")).toList(); }
        if (reports.size() != 1) throw new IllegalStateException("TEST-CHANGE-PROOF-EXTENT: expected one report, got " + reports.size());
        Path canonical = output.resolve("component-report.html");
        if (!reports.getFirst().equals(canonical)) Files.move(reports.getFirst(), canonical);
        var errors = ComponentReportCoverage.violations(rows, Files.readString(canonical));
        if (!errors.isEmpty()) throw new IllegalStateException("TEST-CHANGE-PROOF-EXTENT: " + errors);
    }

    private StaticProofRun() { }
}
