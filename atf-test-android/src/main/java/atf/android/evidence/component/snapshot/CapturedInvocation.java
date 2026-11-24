package atf.android.evidence.component.snapshot;

import atf.android.evidence.component.ComponentInputs;
import atf.reporting.audit.AssertionAuditReport;
import atf.testkit.assertions.ComponentAssertionAudit;
import atf.testkit.assertions.RecordedAssertions;
import atf.testkit.timing.InvocationTimer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Neutral, versioned data already observed by the consumer; never an executable Java object. */
public record CapturedInvocation(AssertionAuditReport.Identity identity, String scenarioName,
        List<ComponentInputs.Entry> inputs, List<Phase> phases, ComponentAssertionAudit.Outcome outcome) {
    public CapturedInvocation {
        java.util.Objects.requireNonNull(identity); java.util.Objects.requireNonNull(outcome);
        if (scenarioName == null || scenarioName.isBlank()) throw new IllegalArgumentException("Missing captured scenario");
        inputs=List.copyOf(inputs); phases=List.copyOf(phases);
        if (phases.isEmpty()) throw new IllegalArgumentException("Missing captured phases");
        if (inputs.stream().filter(i -> i.name().equals("JUnit invocation identity")).count()!=1)
            throw new IllegalArgumentException("One original JUnit identity is required");
    }
    public String id() { return inputs.stream().filter(i -> i.name().equals("JUnit invocation identity")).findFirst().orElseThrow().value(); }
    public ComponentAssertionAudit.Result result() {
        return new ComponentAssertionAudit.Result(phases.stream().map(Phase::restore).toList(),outcome,null,false);
    }
    public static CapturedInvocation capture(AssertionAuditReport.Identity identity, String scenario,
            List<ComponentInputs.Entry> inputs, ComponentAssertionAudit.Result result,
            atf.reporting.security.SensitiveDataRedactor redactor) {
        return new CapturedInvocation(identity,scenario,inputs.stream().map(i -> new ComponentInputs.Entry(i.name(),
                redactor.redactField(i.name(),i.value()))).toList(),result.phases().stream().map(p -> new Phase(p.phase(),
                p.checks(),p.outcome(),redactor.redactText(p.diagnostic()),p.timing().map(t -> t.elapsed().toNanos()).orElse(null),
                p.timing().map(InvocationTimer.Snapshot::intervals).orElse(0))).toList(),result.outcome());
    }
    public record Phase(ComponentAssertionAudit.Phase phase, List<RecordedAssertions.Check> checks,
            ComponentAssertionAudit.Outcome outcome, String diagnostic, Long elapsedNanos, int intervals) {
        public Phase {
            java.util.Objects.requireNonNull(phase); java.util.Objects.requireNonNull(outcome);
            java.util.Objects.requireNonNull(diagnostic); checks=List.copyOf(checks);
            if (elapsedNanos == null ? intervals!=0 : elapsedNanos<0 || intervals<=0)
                throw new IllegalArgumentException("Invalid original invocation timing");
        }
        ComponentAssertionAudit.PhaseResult restore() {
            return new ComponentAssertionAudit.PhaseResult(phase,checks,outcome,diagnostic,
                    elapsedNanos==null ? Optional.empty() : Optional.of(new InvocationTimer.Snapshot(Duration.ofNanos(elapsedNanos),intervals)));
        }
    }
}
