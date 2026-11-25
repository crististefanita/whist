package atf.tests.learning.selection;

import atf.testkit.AtfUnitTest;
import atf.tests.learning.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class EvidenceSelectionUnitTest {
    @Test void fullAndAdaptiveModesPreserveIndependentPopulationsInSuppliedOrder() throws Exception {
        for (int size : new int[]{1, 31, 32, 33, 128}) for (boolean failed : new boolean[]{false, true})
            for (var mode : EvidencePolicy.DetailLevel.values()) {
                var p = SelectionFixture.population(size, failed);
                var policy = SelectionFixture.policy(mode);
                var result = EvidenceSelection.choose(p.inputs(), policy);
                assertEquals(size, p.snapshots().get(), "One immutable snapshot per invocation, independently of sort comparisons");
                boolean all = mode == EvidencePolicy.DetailLevel.FULL || mode == EvidencePolicy.DetailLevel.AUTO && size <= 32 && (long) size * p.inputs().get(0).receipt().path("checkpoints").get(0).path("context").toString().length() <= 4096;
                assertEquals(SelectionFixture.expected(p, policy, all).stream().map(SelectionFixture.Spec::id).toList(), result.scope().get("includedIds"));
                assertEquals(size, (int)result.scope().get("retainedInvocations") + (int)result.scope().get("omittedInvocations"));
            }
    }
    @Test void exactOptionalRecordAndItemBudgetsKeepWholeFirstSeenDistinctFingerprints() throws Exception {
        var p = SelectionFixture.population(18, true);
        for (int budget : new int[]{0, 1, 2, 3}) {
            var policy = new EvidencePolicy(EvidencePolicy.DetailLevel.FULL, 99, 99, 99999, 1, Math.max(1, budget * 77), 100, 20, 2);
            var result = EvidenceSelection.choose(p.inputs(), policy);
            var unique = new LinkedHashSet<String>();p.specs().forEach(s -> unique.add(s.fingerprint()));
            var expected = new ArrayList<String>();long retained = 0;
            for (var row : unique) if (expected.size() < 2 && row.length() <= policy.optionalContextBudgetCharacters() - retained) {expected.add(row);retained += row.length();}
            assertEquals(expected, result.scope().get("optionalPolicyFingerprints"));
            assertEquals(unique.stream().mapToLong(String::length).sum(), result.scope().get("optionalDiagnosticsOriginalCharacters"));
            assertEquals(retained, result.scope().get("optionalDiagnosticsRetainedCharacters"));
            assertEquals(18, result.inputs().size());
        }
    }
    @Test void mandatoryFailuresAndConfiguredCollectionsNeverConsumePassingExampleSlots() throws Exception {
        var p = SelectionFixture.population(100, false);
        var result = EvidenceSelection.choose(p.inputs(), SelectionFixture.policy(EvidencePolicy.DetailLevel.SUMMARY));
        assertTrue(((List<?>)result.scope().get("includedIds")).containsAll(p.specs().stream().filter(SelectionFixture.Spec::mandatory).map(SelectionFixture.Spec::id).toList()));
        assertEquals(List.of(), result.scope().get("optionalPolicyFingerprints"));
    }
    @Test void duplicateAndContradictoryReceiptsCannotEnterSelection() throws Exception {
        var p = SelectionFixture.population(1, false);var policy = SelectionFixture.policy(EvidencePolicy.DetailLevel.FULL);
        assertThrows(IllegalArgumentException.class, () -> EvidenceSelection.choose(List.of(p.inputs().get(0), p.inputs().get(0)), policy));
        var receipt = (ObjectNode)p.inputs().get(0).receipt();
        ((ObjectNode)receipt.path("checkpoints").get(0).path("comparisons").get(0)).put("passed", false);
        var invalid = new LearningRunReport.Input(p.inputs().get(0).source(), receipt, p.inputs().get(0).sha256());
        assertThrows(IllegalArgumentException.class, () -> EvidenceSelection.choose(List.of(invalid), policy));
        assertThrows(IllegalArgumentException.class, () -> EvidenceSelection.choose(List.of(), policy));
    }
    @Test void returnedSelectionAndScopeCannotMutateOriginalCapturedReceipts() throws Exception {
        var p = SelectionFixture.population(3, false);var result = EvidenceSelection.choose(p.inputs(), SelectionFixture.policy(EvidencePolicy.DetailLevel.FULL));
        ((ObjectNode)result.inputs().get(0).receipt()).put("status", "FAIL");
        assertEquals("PASS", result.inputs().get(0).receipt().path("status").asText());
        assertThrows(UnsupportedOperationException.class, () -> result.inputs().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.scope().clear());
        assertThrows(UnsupportedOperationException.class, () -> ((List<?>)result.scope().get("includedIds")).clear());
    }
    @Test void capturedTimingOverflowIsRejectedWithoutWrappedScope() throws Exception {
        var p = SelectionFixture.population(2, false);var entries = new ArrayList<LearningRunReport.Input>();
        for(var input:p.inputs()){var r=(ObjectNode)input.receipt();r.put("durationNanos",Long.MAX_VALUE);entries.add(new LearningRunReport.Input(input.source(),r,input.sha256()));}
        assertThrows(ArithmeticException.class, () -> EvidenceSelection.choose(entries, SelectionFixture.policy(EvidencePolicy.DetailLevel.FULL)));
    }
}
