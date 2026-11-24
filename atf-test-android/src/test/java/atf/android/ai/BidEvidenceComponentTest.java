package atf.android.ai;

import atf.android.component.AndroidComponentAudit;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.inference.BidSampleEvidence;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.rules.GameMode;
import static atf.testkit.assertions.AuditedAssertions.*;

/** The public facade, neutral accumulator and search lifecycle collaborate without a device. */
@AtfComponentTest @ExtendWith(AndroidComponentAudit.class)
class BidEvidenceComponentTest {
    @Test void weightedEstimatesRetainIndependentArithmeticAndFacadeCompatibility(ComponentInputs inputs) {
        var evidence = new BidSampleEvidence(List.of(0, 1), 2, false);
        var facade = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
        for (boolean first : List.of(true, false)) {
            double[] points = first ? new double[]{10, 0} : new double[]{0, 10};
            double[] exact = first ? new double[]{.9, .1} : new double[]{.1, .9};
            evidence.add(points, exact, first ? 1 : 3); facade.add(points, exact, first ? 1 : 3);
        }
        inputs.add("Evaluated worlds and independent oracle", "Weights 1:3; score vectors [10,0] and [0,10]; exact vectors [.9,.1] and [.1,.9]. Means: [2.5,7.5], [.3,.7]; ESS=(1+3)^2/(1+9)=1.6.");
        inputs.add("Original evaluated estimates", evidence.options().toString());
        assertEquals(2.5, evidence.options().getFirst().expectedPoints(), 1e-12, "Weighted score for bid zero");
        assertEquals(.7, evidence.options().getLast().exactContractProbability(), 1e-12, "Weighted exact support for bid one");
        assertEquals(1.6, evidence.effectiveSamples(), 1e-12, "Concentration reduces effective sample size");
        assertEquals(evidence.best(), facade.best(), "The compatibility facade preserves selected bid");
        assertEquals(evidence.samples(), facade.samples(), "The facade preserves raw population");
        assertEquals(evidence.enough(), facade.enough(), "The facade preserves stopping decisions");
        assertEquals(evidence.effectiveSamples(), facade.effectiveSamples(), "The facade preserves confidence arithmetic exactly");
        assertThrows(UnsupportedOperationException.class, () -> evidence.options().clear(), "Published options remain immutable");
        var safe = new MonteCarloAi.BidSamples(List.of(0, 1), 2, true);
        safe.add(new double[]{10, 0}, new double[]{.1, .9});
        assertEquals(1, safe.best(), "Safety mode keeps exact-contract support ahead of the larger score");
    }

    @Test void stoppingKeepsTheMinimumEffectivePopulationAndPairedVariance(ComponentInputs inputs) {
        var separated = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
        for (int index = 0; index < 31; index++) separated.add(new double[]{10, 0}, new double[]{1, 0});
        assertFalse(separated.enough(), "Thirty-one effective worlds cannot stop sampling");
        separated.add(new double[]{10, 0}, new double[]{1, 0});
        assertTrue(separated.enough(), "A separated thirty-two-world margin can stop");
        var uncertain = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
        for (int index = 0; index < 32; index++) uncertain.add(index % 2 == 0 ? new double[]{1, 0} : new double[]{0, 1}, new double[]{.5, .5});
        assertFalse(uncertain.enough(), "A paired unresolved margin retains sampling");
        for (int index = 32; index < 128; index++) uncertain.add(index % 2 == 0 ? new double[]{1, 0} : new double[]{0, 1}, new double[]{.5, .5});
        inputs.add("Stopping boundaries", "Independent fixtures: separated 31/32 effective worlds and tied 32/128 effective worlds. No elapsed-time threshold.");
        assertTrue(uncertain.enough(), "The declared 128-effective-world ceiling terminates the unresolved estimate");
    }

    @Test void invalidWeightsAndUtilitiesCannotClaimCompletedEvidence(ComponentInputs inputs) {
        inputs.add("Rejected inputs", "Weights: 0, -1, NaN, positive infinity. First evaluated utility: NaN, positive infinity. Every fixture has a new accumulator.");
        for (double weight : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var sample = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
            assertThrows(IllegalArgumentException.class, () -> sample.add(new double[]{1, 0}, new double[]{1, 0}, weight), "Non-positive or non-finite weight is refused: " + weight);
            assertEquals(0, sample.samples(), "Rejected weight is not a completed world");
        }
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            var sample = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
            assertThrows(IllegalArgumentException.class, () -> sample.add(new double[]{value, 0}, new double[]{1, 0}), "Non-finite utility is refused");
            assertEquals(0, sample.samples(), "Rejected utility is not a completed world");
        }
    }

    @Test void frozenBidOutputsSeedsAndWorkOrderRemainByteIdentical(ComponentInputs inputs) throws Exception {
        int compared = 0;
        try (var resource = getClass().getResourceAsStream("/ai/bid-evidence-baseline.tsv")) {
            assertNotNull(resource, "The source-bound baseline fixture is required");
            for (String row : new String(resource.readAllBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.startsWith("#") && !line.isBlank()).toList()) {
                String[] fields = row.split("\t");
                var view = AiBidFixtures.view(GameMode.valueOf(fields[0]), Integer.parseInt(fields[1]), 4, true);
                inputs.add("Frozen public observation " + fields[0] + "/" + fields[1], view.toString());
                try (var work = SearchWork.measure(Long.MAX_VALUE)) {
                    var actual = MonteCarloAi.chooseBid(view, ro.whistlab.core.ai.profile.HardStrategyProfile.CURRENT);
                    assertEquals(fields[2], Long.toString(MonteCarloAi.seed(view, 0)), "Original first-world seed");
                    assertEquals(fields[3], Long.toString(MonteCarloAi.seed(view, 31)), "Original later-world seed");
                    assertEquals(fields[4], actual.toString(), "Every original option, estimate, sample count and selected bid is retained");
                    assertEquals(fields[5], Long.toString(work.snapshot().workUnits()), "Search checkpoint consumption order is unchanged");
                    for (var event : SearchWork.Event.values()) assertEquals(fields[6 + event.ordinal()], Long.toString(work.snapshot().events().get(event)), "Original work event population: " + event);
                }
                compared++;
            }
        }
        assertEquals(6, compared, "Every frozen baseline observation is compared");
    }

    @Test void independentDecisionsKeepCancellationAndWorkBudgetOwnership(ComponentInputs inputs) {
        var view = AiBidFixtures.view(GameMode.ROMANIAN_WHIST, 2, 4, true);
        inputs.add("Public decision input", view.toString());
        var first = MonteCarloAi.chooseBid(view, ro.whistlab.core.ai.profile.HardStrategyProfile.CURRENT);
        assertEquals(first, MonteCarloAi.chooseBid(view, ro.whistlab.core.ai.profile.HardStrategyProfile.CURRENT), "A second decision starts with fresh evidence and reproduces the original result");
        try (var work = SearchWork.measure(0)) {
            assertThrows(SearchWork.LimitExceeded.class, () -> MonteCarloAi.chooseBid(view), "A zero work budget cannot publish a completed decision");
            assertEquals("WORK_LIMIT", work.snapshot().stopReason(), "The original stopping cause is retained");
        }
        try (var work = SearchWork.measure(100)) {
            Thread.currentThread().interrupt();
            assertThrows(CancellationException.class, () -> MonteCarloAi.chooseBid(view), "Cancellation precedes evidence allocation");
            assertTrue(Thread.currentThread().isInterrupted(), "The caller retains its interrupt flag");
            assertEquals(0L, work.snapshot().workUnits(), "An interrupted search consumes no decision checkpoints");
        } finally { Thread.interrupted(); }
    }
}
