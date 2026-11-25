package atf.tests.learning.selection;

import atf.tests.learning.EvidencePolicy;
import atf.tests.learning.LearningRunReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Constructed receipts and an independent semantic selection oracle; no archived results. */
final class SelectionFixture {
    static final ObjectMapper JSON = new ObjectMapper();
    record Spec(String id, String status, String family, boolean mandatory, long nanos, String fingerprint) { }
    record Population(List<LearningRunReport.Input> inputs, List<Spec> specs, AtomicInteger snapshots) { }

    static EvidencePolicy policy(EvidencePolicy.DetailLevel level) {
        return new EvidencePolicy(level, 32, 32, 4096, 2, 180, 100, 20, 2);
    }

    static Population population(int size, boolean mostlyFailed) throws Exception {
        var inputs = new ArrayList<LearningRunReport.Input>();
        var specs = new ArrayList<Spec>();
        var snapshots = new AtomicInteger();
        for (int i = 0; i < size; i++) {
            String id = String.format(Locale.ROOT, "capture-%05d", size - i);
            String status = mostlyFailed ? List.of("PASS", "FAIL", "ERROR", "SKIP").get(i % 4)
                    : i % 8 < 4 ? "PASS" : List.of("FAIL", "ERROR", "SKIP", "PASS").get(i % 4);
            String family = "role-" + i % 3;
            boolean configured = i % 11 == 0, gate = i % 13 == 0;
            var r = new CountingReceipt(snapshots);
            r.put("schemaVersion", 1).put("runId", "constructed-selection").put("invocationId", id)
                    .put("className", "constructed.Selection").put("methodName", "independentLiteral")
                    .put("startedAt", "2026-01-01T00:00:00Z").put("endedAt", "2026-01-01T00:00:00.001Z")
                    .put("durationNanos", i + 1L).put("status", status);
            var parameters = r.putObject("parameters");
            parameters.put("role", family).put("opponent", "frozen").put("collection", configured ? "regression" : "learning");
            if (gate) r.put("executionKind", "NODE_COMPONENT_GATE");
            if (!status.equals("PASS")) r.put("failure", "Constructed " + status + " cause " + id);
            var hashes = r.putObject("sourceHashes");
            hashes.put("policy", String.format(Locale.ROOT, "%064x", i % 17));
            var checkpoint = r.putArray("checkpoints").addObject();
            checkpoint.put("owner", id + "/row").put("title", "Complete original literal");
            checkpoint.putObject("context").put("literal", "Ω <literal> " + "x".repeat(256) + " end=" + i);
            checkpoint.putArray("comparisons").addObject().put("name", "Independent integer")
                    .put("expected", i).put("actual", status.equals("FAIL") ? i + 1 : i).put("passed", !status.equals("FAIL"));
            var bytes = JSON.writeValueAsBytes(r);
            inputs.add(new LearningRunReport.Input(Path.of(id + ".json"), r, LearningRunReport.hash(bytes)));
            specs.add(new Spec(id, status, family + " / frozen", configured || gate || !status.equals("PASS"), i + 1L, hashes.toString()));
        }
        snapshots.set(0);
        return new Population(List.copyOf(inputs), List.copyOf(specs), snapshots);
    }

    static List<Spec> expected(Population population, EvidencePolicy policy, boolean all) {
        var chosen = new HashSet<String>();
        var byFamily = new TreeMap<String, List<String>>();
        for (var spec : population.specs()) if (!spec.mandatory())
            byFamily.computeIfAbsent(spec.family(), ignored -> new ArrayList<>()).add(spec.id());
        for (var family : byFamily.values()) {
            Collections.sort(family);
            chosen.addAll(family.subList(0, Math.min(family.size(), policy.passingExamplesPerFamily())));
        }
        return population.specs().stream().filter(s -> all || s.mandatory() || chosen.contains(s.id())).toList();
    }

    private static final class CountingReceipt extends ObjectNode {
        private final AtomicInteger snapshots;
        CountingReceipt(AtomicInteger snapshots) { super(JSON.getNodeFactory()); this.snapshots = snapshots; }
        @Override public ObjectNode deepCopy() {
            snapshots.incrementAndGet();
            var copy = new CountingReceipt(snapshots);
            fields().forEachRemaining(entry -> copy.set(entry.getKey(), entry.getValue().deepCopy()));
            return copy;
        }
    }
}
