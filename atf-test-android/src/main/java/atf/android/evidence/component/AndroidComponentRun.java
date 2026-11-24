package atf.android.evidence.component;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.ComponentAssertionAudit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Captures invocations once, then publishes on the closing owner; execution timing remains original. */
public final class AndroidComponentRun implements AutoCloseable {
    private record Invocation(AssertionAuditReport.Identity identity, ComponentAssertionAudit.Result result,
                              List<ComponentInputs.Entry> inputs, String scenarioName) { }
    private final Path file;
    private final boolean fallback;
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    private final List<Invocation> invocations = new ArrayList<>();
    private final String provenance = System.getProperty(AndroidComponentReports.PROVENANCE, "");
    private boolean provenancePublished;
    private boolean closed;
    private final atf.android.evidence.component.snapshot.ComponentCaptureJournal journal;
    private List<ReportObservation.Artifact> lineage=List.of();

    public AndroidComponentRun(Path file, boolean fallback, ReportingSettings settings) {
        this.file = java.util.Objects.requireNonNull(file);
        this.fallback = fallback;
        this.settings = java.util.Objects.requireNonNull(settings);
        try {
            String directory=System.getProperty(atf.android.evidence.component.snapshot.ComponentCaptureJournal.DIRECTORY,"");
            journal=directory.isBlank() || !Path.of(directory).toAbsolutePath().getParent().equals(file.toAbsolutePath().getParent())
                    ? null : new atf.android.evidence.component.snapshot.ComponentCaptureJournal(Path.of(directory));
        } catch(java.io.IOException failure) {throw new java.io.UncheckedIOException(failure);}
    }
    /** Scoped republication of admitted original observations; this method never executes or evaluates a test. */
    public synchronized void includeCaptured(List<atf.android.evidence.component.snapshot.CapturedInvocation> saved,
            List<ReportObservation.Artifact> lineage) {
        if(closed || !invocations.isEmpty()) throw new IllegalStateException("Captured republication requires a new report owner");
        var ids=new java.util.HashSet<String>();
        for(var row:saved) {
            if(!ids.add(row.id())) throw new IllegalArgumentException("Duplicate captured invocation: "+row.id());
            invocations.add(new Invocation(row.identity(),row.result(),row.inputs(),row.scenarioName()));
        }
        this.lineage=List.copyOf(lineage);
    }
    public AssertionValueDisplay display() {
        return new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField);
    }
    public AssertionValueDisplay display(ComponentInputs inputs) {
        return new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField, projection -> {
            if (projection.truncated()) inputs.add("Full comparison value — " + projection.role() + " — "
                    + redactor.redactText(String.valueOf(projection.name())), projection.fullText());
        });
    }
    synchronized void capture(AssertionAuditReport.Identity identity, ComponentAssertionAudit.Result result, ComponentInputs inputs) {
        if (closed) throw new IllegalStateException("Component report owner is closed");
        long ordinal = invocations.stream().filter(invocation -> invocation.identity().testClass().equals(identity.testClass())
                && invocation.identity().testPackage().equals(identity.testPackage())
                && invocation.identity().method().equals(identity.method())).count() + 1;
        var captured = new ArrayList<>(inputs.snapshot());
        captured.add(new ComponentInputs.Entry("JUnit display name", identity.scenario()));
        String scenarioName=identity.method() + " [" + ordinal + "]";
        if(journal!=null) try {
            journal.append(atf.android.evidence.component.snapshot.CapturedInvocation.capture(identity,scenarioName,captured,result,redactor));
        } catch(java.io.IOException failure) {throw new java.io.UncheckedIOException(failure);}
        invocations.add(new Invocation(identity, result, List.copyOf(captured), scenarioName));
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        try (var timing = new atf.android.evidence.timing.ExtentPublicationTiming(file)) {
            publish();
            timing.passed();
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private void publish() {
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "JVM — no Android device")
                .put(ReportEnvironmentField.SUITE, "Whist JVM component and unit acceptance")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Captured inputs and comparisons; publication after execution").build();
        var scenarios = new ArrayList<atf.reporting.artifact.ReportArtifactProfile.Scenario>();
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings)
                : new ExtentReportSession(file, settings, redactor, AtfConsoleSink.noOp(), environment)) {
            for (var invocation : invocations) scenarios.add(publish(session, invocation));
        } finally { invocations.clear(); }
        if (!scenarios.isEmpty()) {
            var expectedProfile = new atf.reporting.artifact.ReportArtifactProfile(fallback
                    ? atf.reporting.artifact.ReportArtifactProfile.Renderer.FALLBACK
                    : atf.reporting.artifact.ReportArtifactProfile.Renderer.EXTENT, scenarios);
            var inspection = atf.reporting.artifact.ReportArtifactInspector.inspect(file, expectedProfile);
            if (!inspection.violations().isEmpty()) throw new IllegalStateException("Published component report differs from captured intent: " + inspection.violations());
        }
    }
    private atf.reporting.artifact.ReportArtifactProfile.Scenario publish(AtfReportPorts.Session session, Invocation invocation) {
        var id = invocation.identity();
        var phases = new ArrayList<atf.reporting.artifact.ReportArtifactProfile.Phase>();
        try (var scenario = session.startScenario(id.testClass(), id.testPackage(), invocation.scenarioName(), id.tags())) {
            for (var observed : invocation.result().phases()) {
                try (var phase = scenario.startPhase(observed.phase().name().replace('_', ' '))) {
                    var evidence = new ArrayList<atf.reporting.artifact.ReportArtifactProfile.Evidence>();
                    java.util.function.Consumer<ReportObservation> observation = value -> {
                        evidence.add(new atf.reporting.artifact.ReportArtifactProfile.Observation(visibleLabel(value.title()),
                                value.fields().stream().map(field -> visibleLabel(field.name())).toList(),
                                value.bodies().stream().map(body -> visibleLabel(body.label())).toList()));
                        phase.observation(value);
                    };
                    phase.detail("Test method", id.method());
                    if (observed == invocation.result().phases().getFirst()) {
                        var receipt = new java.util.LinkedHashMap<String, Object>();
                        receipt.put("schemaVersion", 1);
                        receipt.put("invocationId", invocation.inputs().stream().filter(i -> i.name().equals("JUnit invocation identity"))
                                .map(ComponentInputs.Entry::value).findFirst().orElse(""));
                        receipt.put("outcome", invocation.result().outcome().name());
                        receipt.put("comparisons", invocation.result().phases().stream().mapToInt(p -> p.checks().size()).sum());
                        observation.accept(new ReportObservation("Component invocation receipt", List.of(),
                                inputBodies(new ComponentInputs.Entry("Component invocation receipt",
                                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(receipt).toString())), List.of()));
                        if (!provenancePublished && !provenance.isBlank()) {
                            observation.accept(new ReportObservation("Live source execution provenance", List.of(),
                                    inputBodies(new ComponentInputs.Entry("Execution provenance", provenance)), List.of()));
                        }
                        if(!provenancePublished && !lineage.isEmpty()) observation.accept(new ReportObservation(
                                "Recovery report lineage",List.of(new ReportField("Meaning","Original attempt and retry; no original verdict is edited")),
                                inputBodies(new ComponentInputs.Entry("Attempt identities and immutable report hashes",new com.fasterxml.jackson.databind.ObjectMapper()
                                        .valueToTree(lineage.stream().map(a -> java.util.Map.of("label",a.label(),"path",
                                                file.toAbsolutePath().getParent().relativize(Path.of(a.location())).toString().replace('\\','/'),"sha256",a.sha256())).toList()).toPrettyString())),List.of()));
                        provenancePublished = true;
                    }
                    observed.timing().ifPresent(time -> phase.executionTiming(new ReportPhaseTiming(time.elapsed(), time.intervals())));
                    if (observed.phase() == ComponentAssertionAudit.Phase.TEST_BODY) {
                        int ordinal = 0;
                        for (var input : invocation.inputs()) observation.accept(new ReportObservation("Component input: " + input.name(),
                                List.of(new ReportField("Input ordinal", Integer.toString(++ordinal)),
                                        new ReportField("Role", "Declared component input or comparison context, not a native capture")),
                                inputBodies(input), List.of()));
                    }
                    if (!observed.checks().isEmpty()) {
                        evidence.add(new atf.reporting.artifact.ReportArtifactProfile.Assertions("Component assertions", observed.checks().stream()
                                .map(row -> new atf.reporting.artifact.ReportArtifactProfile.Comparison(visibleLabel(row.name()), row.passed())).toList()));
                        AssertionAuditReport.publishStep(phase, "Component assertions", observed.checks().stream()
                                .map(row -> new AtfAssertion(row.name(), row.expected(), row.actual(), row.passed())).toList(), AtfReportStatus.INFO);
                    }
                    if (!observed.diagnostic().isBlank()) {
                        String diagnostic = redactor.redactText(observed.diagnostic());
                        phase.diagnostic(observed.outcome() == ComponentAssertionAudit.Outcome.ABORTED
                                ? ReportPhaseDiagnostic.Kind.EXECUTION_ABORTED : ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, diagnostic);
                        observation.accept(new ReportObservation("Full original execution diagnostic", List.of(),
                                    inputBodies(new ComponentInputs.Entry("Execution diagnostic", diagnostic)), List.of()));
                    }
                    phase.complete(status(observed.outcome()));
                    phases.add(new atf.reporting.artifact.ReportArtifactProfile.Phase(observed.phase().name().replace('_', ' '), status(observed.outcome()), evidence));
                }
            }
            scenario.complete(status(invocation.result().outcome()));
        }
        return new atf.reporting.artifact.ReportArtifactProfile.Scenario(id.testClass(), visibleLabel(invocation.scenarioName()), status(invocation.result().outcome()), phases);
    }
    /** Profile labels describe visible HTML text; publication retains the original labels and comparison bodies. */
    private String visibleLabel(String value) {
        return redactor.redactText(value).replaceAll("[\\t\\n\\r\\f \\u00a0]+", " ").trim();
    }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome outcome) {
        return switch (outcome) { case PASS -> AtfReportStatus.PASS; case FAIL -> AtfReportStatus.FAIL; case ABORTED -> AtfReportStatus.SKIP; };
    }
    private List<ReportObservation.Body> inputBodies(ComponentInputs.Entry input) {
        var parts = atf.android.evidence.text.EvidenceTextParts.split(
                redactor.redactField(input.name(), input.value()), settings.bodyMaxDisplayCharacters());
        var bodies = new ArrayList<ReportObservation.Body>();
        for (int index = 0; index < parts.size(); index++) bodies.add(new ReportObservation.Body(
                "Full captured input — part " + (index + 1) + "/" + parts.size(), parts.get(index), ReportObservation.Format.TEXT));
        return List.copyOf(bodies);
    }
}
