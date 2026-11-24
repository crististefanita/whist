package atf.android.e2e.support;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.android.evidence.timing.AndroidPhaseClock;
import atf.android.evidence.timing.AndroidPhaseTiming;
import atf.android.e2e.support.journal.AndroidUiFlowJournal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persists ordered, renderer-neutral Android UI checkpoints for the final Whist Extent report. */
public final class AndroidUiFlowTrace implements atf.android.e2e.support.flow.AndroidUiFlowEvidence {
    private final Path outputDirectory;
    private final Path manifest;
    private final String id;
    private final String name;
    private final String testClass;
    private final String testPackage;
    private final String appPackage;
    private final List<String> tags;
    private final AndroidPhaseClock clock;
    private final Map<String, List<Checkpoint>> phases = new LinkedHashMap<>();
    private final List<AtfUiActionRecord> pendingActions = new ArrayList<>();
    private final List<AtfUiActionRecord> failedActions = new ArrayList<>();
    private String activePhase = "";
    private String failurePhase = "";
    private int actionSequence;
    private AtfReportStatus status = AtfReportStatus.WARNING;
    private String failure = "";

    public AndroidUiFlowTrace(Path outputDirectory, String id, String name,
            String testClass, String testPackage, String appPackage, List<String> tags) throws IOException {
        this(outputDirectory, id, name, testClass, testPackage, appPackage, tags, System::nanoTime);
    }

    public AndroidUiFlowTrace(Path outputDirectory, String id, String name,
            String testClass, String testPackage, String appPackage, List<String> tags,
            java.util.function.LongSupplier ticks) throws IOException {
        this.outputDirectory = Objects.requireNonNull(outputDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.outputDirectory);
        this.id = required(id, "id");
        this.clock = new AndroidPhaseClock(this.id, ticks);
        this.name = required(name, "name");
        this.testClass = required(testClass, "testClass");
        this.testPackage = required(testPackage, "testPackage");
        this.appPackage = required(appPackage, "appPackage");
        this.tags = List.copyOf(tags);
        this.manifest = this.outputDirectory.resolve("ui-flow-" + id + ".properties");
        flush();
    }

    /** Must precede the first action of a phase; checkpoints never reconstruct its start. */
    public synchronized void beginPhase(String name) throws IOException {
        String phase = required(name, "phase");
        phases.computeIfAbsent(phase, ignored -> new ArrayList<>());
        clock.enter(phase, new ArrayList<>(phases.keySet()).indexOf(phase));
        activePhase = phase;
        flush();
    }

    public synchronized AndroidPhaseClock.Scope capture() { return clock.capture(); }
    public synchronized boolean hasOpenPhase() { return phases.keySet().stream().anyMatch(clock::running); }

    public synchronized int nextActionSequence() {
        return ++actionSequence;
    }

    /** Retains one measured interaction until the next ordered UI checkpoint. */
    public synchronized void recordAction(AtfUiActionRecord action) {
        Objects.requireNonNull(action, "action");
        pendingActions.add(new AtfUiActionRecord(++actionSequence, action.operation(), action.target(),
                action.expected(), action.observed(), action.durationMillis(), action.status(), action.source()));
    }

    public synchronized void checkpoint(String phase, String keyword, String title, String target,
            Path screenshot, List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus checkpointStatus, List<Path> supportingArtifacts) throws IOException {
        String phaseName = required(phase, "phase");
        if (phases.keySet().stream().anyMatch(clock::running) && !clock.running(phaseName)) {
            throw new IllegalStateException("Checkpoint does not belong to the active Android phase: " + phaseName);
        }
        String screenshotName = artifactName(screenshot);
        List<String> artifactNames = supportingArtifacts == null ? List.of()
                : supportingArtifacts.stream().map(this::artifactName).filter(value -> !value.isBlank()).toList();
        List<AtfUiActionRecord> checkpointActions = new ArrayList<>(pendingActions);
        if (actions != null) checkpointActions.addAll(actions);
        var checkpoint = new Checkpoint(required(keyword, "keyword"), required(title, "title"),
                required(target, "target"), screenshotName, List.copyOf(checkpointActions),
                List.copyOf(assertions), Objects.requireNonNull(checkpointStatus), artifactNames);
        appendCheckpoint(phaseName, checkpoint);
    }

    private void appendCheckpoint(String phase, Checkpoint checkpoint) throws IOException {
        boolean newPhase = !phases.containsKey(phase);
        var steps = phases.computeIfAbsent(phase, ignored -> new ArrayList<>());
        var pending = List.copyOf(pendingActions);
        steps.add(checkpoint);
        pendingActions.clear();
        boolean[] committed = {false};
        try {
            clock.journal(() -> { writeManifest(); committed[0] = true; });
        } catch (IOException | RuntimeException failure) {
            // A timing failure after replacement must not erase already committed evidence.
            if (!committed[0]) {
                steps.removeLast();
                if (newPhase) phases.remove(phase);
                pendingActions.addAll(pending);
            }
            throw failure;
        }
    }

    public synchronized void finish(AtfReportStatus finalStatus) throws IOException {
        Objects.requireNonNull(finalStatus);
        clock.stop(false);
        boolean recordedFailure = status == AtfReportStatus.FAIL || !failure.isBlank()
                || pendingActions.stream().anyMatch(action -> action.status() == AtfReportStatus.FAIL)
                || phases.values().stream().flatMap(List::stream).anyMatch(checkpoint ->
                        checkpoint.status() == AtfReportStatus.FAIL
                                || checkpoint.actions().stream().anyMatch(action -> action.status() == AtfReportStatus.FAIL)
                                || checkpoint.assertions().stream().anyMatch(assertion -> assertion.resultStatus() == AtfReportStatus.FAIL));
        status = recordedFailure ? AtfReportStatus.FAIL : finalStatus;
        flush();
    }

    public synchronized void fail(Throwable cause) throws IOException {
        clock.stop(true);
        if (failurePhase.isBlank()) failurePhase = activePhase;
        failedActions.addAll(pendingActions);
        pendingActions.clear();
        status = AtfReportStatus.FAIL;
        String detail = cause == null ? "Unknown test failure" : cause.getClass().getSimpleName() + ": "
                + String.valueOf(cause.getMessage());
        failure = failure.isBlank() ? detail : failure + " | additional failure: " + detail;
        flush();
    }

    public Path manifest() { return manifest; }

    public String id() { return id; }
    public String name() { return name; }
    public synchronized String activePhaseName() { return required(activePhase,"active phase"); }
    public String testClass() { return testClass; }
    public String testPackage() { return testPackage; }
    public String appPackage() { return appPackage; }
    public List<String> tags() { return tags; }
    public synchronized AtfReportStatus status() { return status; }
    public synchronized String failure() { return failure; }
    public synchronized String failurePhase() { return failurePhase; }
    public synchronized List<AtfUiActionRecord> pendingActions() {
        var retained = new ArrayList<>(failedActions);
        retained.addAll(pendingActions);
        return List.copyOf(retained);
    }

    /** Immutable in-memory projection used to publish each checkpoint through the public UI report API. */
    public synchronized List<PhaseSnapshot> phaseSnapshots() {
        return phases.entrySet().stream()
                .map(entry -> new PhaseSnapshot(entry.getKey(), entry.getValue().stream()
                        .map(checkpoint -> new CheckpointSnapshot(checkpoint.keyword(), checkpoint.title(),
                                checkpoint.target(), resolveArtifact(checkpoint.screenshot()),
                                checkpoint.actions(), checkpoint.assertions(), checkpoint.status(),
                                checkpoint.artifacts().stream().map(this::resolveArtifact).toList()))
                        .toList(), clock.snapshot(entry.getKey())))
                .toList();
    }

    /** Returns the recorded comparisons in execution order with their checkpoint context. */
    public synchronized List<AtfAssertion> assertions() {
        return phases.entrySet().stream()
                .flatMap(phase -> phase.getValue().stream()
                        .flatMap(checkpoint -> checkpoint.assertions().stream()
                                .map(assertion -> new AtfAssertion(
                                        phase.getKey() + " · " + checkpoint.keyword() + " · "
                                                + checkpoint.title() + " · " + assertion.name(),
                                        assertion.expected(), assertion.actual(), assertion.passed(),
                                        assertion.resultStatus()))))
                .toList();
    }

    private String artifactName(Path file) {
        if (file == null) return "";
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(outputDirectory)) {
            throw new IllegalArgumentException("Android UI flow evidence must stay under " + outputDirectory + ": " + file);
        }
        return outputDirectory.relativize(normalized).toString().replace('\\', '/');
    }

    private Path resolveArtifact(String relative) {
        return relative == null || relative.isBlank() ? null
                : outputDirectory.resolve(relative).normalize();
    }

    private void flush() throws IOException {
        clock.journal(this::writeManifest);
    }

    private void writeManifest() throws IOException {
        var running = phases.keySet().stream().filter(clock::running).collect(java.util.stream.Collectors.toSet());
        AndroidUiFlowJournal.write(manifest, AndroidUiFlowJournal.encode(this, failurePhase, running));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private record Checkpoint(String keyword, String title, String target, String screenshot,
            List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus status, List<String> artifacts) { }

    public record PhaseSnapshot(String name, List<CheckpointSnapshot> checkpoints, java.util.Optional<AndroidPhaseTiming> timing) {
        public PhaseSnapshot(String name, List<CheckpointSnapshot> checkpoints) { this(name, checkpoints, java.util.Optional.empty()); }
        public PhaseSnapshot { checkpoints = List.copyOf(checkpoints); }
    }

    public record CheckpointSnapshot(String keyword, String title, String target, Path screenshot,
            List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus status, List<Path> supportingArtifacts) {
        public CheckpointSnapshot {
            actions = List.copyOf(actions);
            assertions = List.copyOf(assertions);
            supportingArtifacts = List.copyOf(supportingArtifacts);
        }
    }
}
