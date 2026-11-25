package atf.tests.learning;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Optional fingerprints are whole records; mandatory inputs and comparisons are never budgeted here. */
final class EvidenceDiagnostics {
    private EvidenceDiagnostics() { }

    static void describe(Map<String, Object> scope, List<EvidencePopulation.Entry> included, EvidencePolicy policy) {
        var unique = new LinkedHashSet<String>();
        for (var entry : included) if (!entry.fingerprints().isMissingNode()) unique.add(entry.fingerprints().toString());
        long original = 0, retained = 0;
        var records = new ArrayList<String>();
        for (var diagnostic : unique) {
            original = Math.addExact(original, diagnostic.length());
            if (policy.detailLevel() != EvidencePolicy.DetailLevel.SUMMARY && records.size() < policy.optionalMaxItems()
                    && diagnostic.length() <= policy.optionalContextBudgetCharacters() - retained) {
                records.add(diagnostic);
                retained = Math.addExact(retained, diagnostic.length());
            }
        }
        scope.put("optionalDiagnosticsOriginalRecords", unique.size());
        scope.put("optionalDiagnosticsRetainedRecords", records.size());
        scope.put("optionalDiagnosticsOmittedRecords", unique.size() - records.size());
        scope.put("optionalDiagnosticsOriginalCharacters", original);
        scope.put("optionalDiagnosticsRetainedCharacters", retained);
        scope.put("optionalDiagnosticsOmittedCharacters", original - retained);
        scope.put("optionalDiagnosticsReason", "SUMMARY omits optional policy hashes; otherwise repeated hashes are deduplicated and whole records use the shared cumulative budget. No assertion inputs or verdicts are optional.");
        scope.put("optionalPolicyFingerprints", List.copyOf(records));
    }
}
