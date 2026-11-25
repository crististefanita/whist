package atf.tests.learning;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Reads each immutable invocation once; private descriptors never expose the snapshot to callers. */
record EvidencePopulation(List<Entry> entries, long comparisons, long contextCharacters, long durationNanos) {
    record Entry(LearningRunReport.Input input, String id, String family, boolean mandatory,
                 long durationNanos, JsonNode fingerprints) { }

    static EvidencePopulation read(List<LearningRunReport.Input> population, EvidencePolicy policy) {
        var supplied = List.copyOf(population);
        if (supplied.isEmpty()) throw new IllegalArgumentException("Empty population");
        var entries = new ArrayList<Entry>();
        var ids = new HashSet<String>();
        long comparisons = 0, context = 0, duration = 0;
        for (var input : supplied) {
            var receipt = input.receipt();
            LearningRunReport.validate(receipt);
            String id = receipt.path("invocationId").asText();
            if (!ids.add(id)) throw new IllegalArgumentException("Repeated population invocation");
            for (var checkpoint : receipt.path("checkpoints")) {
                comparisons = Math.addExact(comparisons, checkpoint.path("comparisons").size());
                context = Math.addExact(context, checkpoint.path("context").toString().length());
            }
            long nanos = receipt.path("durationNanos").asLong();
            duration = Math.addExact(duration, nanos);
            var parameters = receipt.path("parameters");
            String family = parameters.has("role") ? parameters.path("role").asText() + " / " + parameters.path("opponent").asText()
                    : receipt.path("className").asText() + " / " + parameters.path("language").asText();
            boolean mandatory = policy.alwaysIncludeCollections().contains(parameters.path("collection").asText())
                    || !receipt.path("status").asText().equals("PASS")
                    || receipt.path("executionKind").asText().equals("NODE_COMPONENT_GATE");
            entries.add(new Entry(input, id, family, mandatory, nanos, receipt.path("sourceHashes")));
        }
        return new EvidencePopulation(List.copyOf(entries), comparisons, context, duration);
    }
}
