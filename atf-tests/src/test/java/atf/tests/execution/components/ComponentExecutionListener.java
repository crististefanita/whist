package atf.tests.execution.components;

import atf.tests.reporting.aggregate.ComponentReportCoverage.Invocation;
import java.util.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;

/** Independent JUnit receipt; dynamic invocations retain their native unique IDs. */
public final class ComponentExecutionListener implements TestExecutionListener {
    private final Map<String, String> rows = new LinkedHashMap<>();
    private final List<String> failures = new ArrayList<>();
    private final Set<String> unresolved = new LinkedHashSet<>();
    private final java.util.function.Consumer<ComponentExecutionListener> checkpoint;
    private boolean complete;
    public ComponentExecutionListener() { this(ignored -> { }); }
    public ComponentExecutionListener(java.util.function.Consumer<ComponentExecutionListener> checkpoint) { this.checkpoint=checkpoint; }
    @Override public synchronized void testPlanExecutionStarted(TestPlan plan) {
        for (var root : plan.getRoots()) for (var id : plan.getDescendants(root)) {
            if (id.isTest()) rows.put(id.getUniqueId(), "NOT_EXECUTED");
            else if (id.getSource().orElse(null) instanceof org.junit.platform.engine.support.descriptor.MethodSource) unresolved.add(id.getUniqueId());
        }
        checkpoint.accept(this);
    }
    @Override public synchronized void dynamicTestRegistered(TestIdentifier id) {
        if (id.isTest()) { rows.put(id.getUniqueId(), "NOT_EXECUTED"); checkpoint.accept(this); }
    }
    @Override public synchronized void testPlanExecutionFinished(TestPlan plan) { complete=true; checkpoint.accept(this); }
    @Override public synchronized void executionSkipped(TestIdentifier id, String reason) {
        if (id.isTest()) rows.put(id.getUniqueId(), "ABORTED"); else failures.add("Skipped container: " + id.getUniqueId() + " / " + reason);
        checkpoint.accept(this);
    }
    @Override public synchronized void executionFinished(TestIdentifier id, TestExecutionResult result) {
        String outcome = switch (result.getStatus()) {case SUCCESSFUL -> "PASS";case FAILED -> "FAIL";case ABORTED -> "ABORTED";};
        if (id.isTest()) rows.put(id.getUniqueId(), outcome);
        else if (result.getStatus() == TestExecutionResult.Status.FAILED) failures.add(id.getUniqueId() + " / " + result.getThrowable().map(failure -> {
            var diagnostic = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(diagnostic));
            return diagnostic.toString();
        }).orElse("No original throwable supplied"));
        if (!id.isTest() && result.getStatus()==TestExecutionResult.Status.SUCCESSFUL) unresolved.remove(id.getUniqueId());
        checkpoint.accept(this);
    }
    public synchronized List<Invocation> invocations() { return rows.entrySet().stream().map(e -> new Invocation(e.getKey(),e.getValue())).toList(); }
    public synchronized List<String> containerFailures() { return List.copyOf(failures); }
    public synchronized boolean populationComplete() { return complete || unresolved.isEmpty(); }
    synchronized Map<String, Long> counts() {
        var counts = new TreeMap<String, Long>();rows.values().forEach(v -> counts.merge(v,1L,Long::sum));return counts;
    }
    synchronized boolean successful() { return !rows.isEmpty() && failures.isEmpty() && rows.values().stream().allMatch("PASS"::equals); }
}
