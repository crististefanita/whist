package atf.tests.learning;

import java.util.*;

/** Selects whole measured invocations, retaining all non-PASS and every quality gate. */
public record EvidenceSelection(List<LearningRunReport.Input> inputs, Map<String, Object> scope, EvidencePolicy policy) {
    public EvidenceSelection {
        inputs = List.copyOf(inputs);
        scope = Collections.unmodifiableMap(new LinkedHashMap<>(scope));
        Objects.requireNonNull(policy);
    }

    public static EvidenceSelection choose(List<LearningRunReport.Input> population, EvidencePolicy policy) {
        Objects.requireNonNull(policy);
        var observed = EvidencePopulation.read(population, policy);
        boolean small = observed.entries().size() <= policy.maxDetailedInvocations()
                && observed.comparisons() <= policy.maxDetailedComparisons()
                && observed.contextCharacters() <= policy.maxDetailedContextCharacters();
        boolean all = policy.detailLevel() == EvidencePolicy.DetailLevel.FULL
                || policy.detailLevel() == EvidencePolicy.DetailLevel.AUTO && small;
        var passIds = new HashSet<String>();
        if (!all) {
            var families = new HashMap<String, Integer>();
            for (var entry : observed.entries().stream().sorted(Comparator.comparing(EvidencePopulation.Entry::id)).toList()) {
                if (!entry.mandatory() && families.getOrDefault(entry.family(), 0) < policy.passingExamplesPerFamily()) {
                    passIds.add(entry.id());
                    families.merge(entry.family(), 1, Integer::sum);
                }
            }
        }
        var included = new ArrayList<EvidencePopulation.Entry>();
        var omitted = new ArrayList<String>();
        long retainedNanos = 0;
        for (var entry : observed.entries()) {
            if (all || entry.mandatory() || passIds.contains(entry.id())) {
                included.add(entry);
                retainedNanos = Math.addExact(retainedNanos, entry.durationNanos());
            } else omitted.add(entry.id());
        }
        var scope = new LinkedHashMap<String, Object>();
        scope.put("requestedDetail", policy.detailLevel().name());
        scope.put("effectiveScope", omitted.isEmpty() ? "All measured invocations in the supplied collection"
                : "Selected whole examples plus ALL non-PASS invocations, configured complete collections and the full-population quality gate; omitted games are NOT individually audited by this HTML");
        scope.put("originalInvocations", observed.entries().size());
        scope.put("retainedInvocations", included.size());
        scope.put("omittedInvocations", omitted.size());
        scope.put("originalComparisons", observed.comparisons());
        scope.put("originalContextCharacters", observed.contextCharacters());
        scope.put("selectionReason", all ? "FULL requested or AUTO within all configured inclusive thresholds"
                : "Representative selection at invocation boundaries; deterministic first IDs per family, not a best-result filter");
        scope.put("includedIds", included.stream().map(EvidencePopulation.Entry::id).toList());
        scope.put("omittedIds", List.copyOf(omitted));
        scope.put("policy", Map.of("detailLevel", policy.detailLevel().name(), "maxDetailedInvocations", policy.maxDetailedInvocations(),
                "maxDetailedComparisons", policy.maxDetailedComparisons(), "maxDetailedContextCharacters", policy.maxDetailedContextCharacters(),
                "passingExamplesPerFamily", policy.passingExamplesPerFamily(), "optionalContextBudgetCharacters", policy.optionalContextBudgetCharacters(),
                "rowsPerGroup", policy.rowsPerGroup(), "fieldsPerGroup", policy.fieldsPerGroup(), "optionalMaxItems", policy.optionalMaxItems(),
                "alwaysIncludeCollections", policy.alwaysIncludeCollections()));
        EvidenceDiagnostics.describe(scope, included, policy);
        scope.put("executionTiming", Map.of("suppliedCapturedIntervalsSumNanos", observed.durationNanos(),
                "retainedIntervalsSumNanos", retainedNanos,
                "definition", "Sum of captured invocation intervals; concurrent intervals may overlap. Not full-run wall time; publication and other native test suites are excluded."));
        scope.put("presentationOrder", "Supplied manifest order, preserving checkpoints within each invocation. Actual timestamps retained; concurrent workers are not one sequential UI journey.");
        return new EvidenceSelection(included.stream().map(EvidencePopulation.Entry::input).toList(), scope, policy);
    }
}
