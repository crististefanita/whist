package atf.tests.learning.selection;

import atf.tests.learning.EvidencePolicy;
import atf.tests.learning.EvidenceSelection;
import java.util.*;

/** Isolated selector-only cost probe; no renderer, browser, images or archived games. */
public final class SelectionCostProbe {
    public static void main(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        boolean mostlyFailed = Boolean.parseBoolean(args[1]);
        var population = SelectionFixture.population(size, mostlyFailed);
        var policy = SelectionFixture.policy(EvidencePolicy.DetailLevel.SUMMARY);
        long began = System.nanoTime();
        List<?> retained;
        Map<?, ?> scope;
        if (args.length == 3) {
            var type = Class.forName("atf.tests.learning.ReferenceEvidenceSelection");
            var result = type.getMethod("choose", List.class, EvidencePolicy.class).invoke(null, population.inputs(), policy);
            retained = (List<?>) type.getMethod("inputs").invoke(result);
            scope = (Map<?, ?>) type.getMethod("scope").invoke(result);
        } else {
            var result = EvidenceSelection.choose(population.inputs(), policy);
            retained = result.inputs(); scope = result.scope();
        }
        long elapsed = System.nanoTime() - began;
        int snapshots = population.snapshots().get();
        var expected = SelectionFixture.expected(population, policy, false);
        var expectedIds = expected.stream().map(SelectionFixture.Spec::id).toList();
        if (!expectedIds.equals(scope.get("includedIds")) || retained.size() != expected.size()) throw new AssertionError("Independent selection oracle");
        var selectedIds = new HashSet<>(expectedIds);
        var expectedOmitted = population.specs().stream().map(SelectionFixture.Spec::id).filter(id -> !selectedIds.contains(id)).toList();
        if (!expectedOmitted.equals(scope.get("omittedIds"))) throw new AssertionError("Complete omission inventory");
        var timing = (Map<?, ?>) scope.get("executionTiming");
        if (!Long.valueOf(population.specs().stream().mapToLong(SelectionFixture.Spec::nanos).sum()).equals(timing.get("suppliedCapturedIntervalsSumNanos"))) throw new AssertionError("Original timing inventory");
        var result = new LinkedHashMap<String, Object>();
        result.put("status", "PASS"); result.put("population", size); result.put("mostlyFailed", mostlyFailed);
        result.put("snapshots", snapshots); result.put("retained", retained.size()); result.put("expectedRetained", expected.size());
        result.put("mandatory", population.specs().stream().filter(SelectionFixture.Spec::mandatory).count());
        result.put("expectedIdsSha256", atf.tests.learning.LearningRunReport.hash(SelectionFixture.JSON.writeValueAsBytes(expectedIds)));
        result.put("actualIdsSha256", atf.tests.learning.LearningRunReport.hash(SelectionFixture.JSON.writeValueAsBytes(scope.get("includedIds"))));
        result.put("scopeSha256", atf.tests.learning.LearningRunReport.hash(SelectionFixture.JSON.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsBytes(scope)));
        result.put("elapsedNanos", elapsed); result.put("maximumHeapBytes", Runtime.getRuntime().maxMemory());
        System.out.println(SelectionFixture.JSON.writeValueAsString(result));
    }
    private SelectionCostProbe() { }
}
