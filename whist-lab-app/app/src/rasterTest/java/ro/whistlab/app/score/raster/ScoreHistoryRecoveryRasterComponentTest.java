package ro.whistlab.app.score.raster;

import atf.gradle.audit.input.CapturedValue;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.app.score.series.ScoredHandFixture;
import ro.whistlab.core.rules.GameMode;

public final class ScoreHistoryRecoveryRasterComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void savedBiddingPlayingAndSettlementKeepHistoricalRectanglesAndPixelsInBothLanguages() throws Exception {
        String lastRasterContext = "Raster fixture not evaluated";
        try {
            for (int players = 3; players <= 6; players++) {
                var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.PENALTY_WITH_POSITIVE_TOTAL);
                var expected = HistoryRecoveryProof.Expected.current(game);

                lastRasterContext = CapturedValue.text(new Object[]{"Fixture before first assertion", "expected", expected, "game", game, "players", players});
                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "expected", expected, "players", players, "fixture contract", "Controlled JVM projection; full immutable context retained on first failure");
                assertEquals("expected.series().get(0).intValue() must equal -10", -10, expected.series().get(0).intValue());
                assertTrue("expected.totals().get(0) > 0 must hold", expected.totals().get(0) > 0);
                String id = "resume-" + players + "p";
                game = HistoryRecoveryProof.verify(id + "-award", game, expected, temporary.getRoot().toPath());
                game.startNextHand();
                ScoredHandFixture.finish(game, 0, (stage, source) ->
                        HistoryRecoveryProof.verify(id + "-" + stage, source, expected, temporary.getRoot().toPath()));
            }
        } catch (Exception | AssertionError failure) {
            atf.gradle.audit.GradleInputs.note("Full first raster failure context", lastRasterContext);
            throw failure;
        }
    }

    @Test public void finalHandsAndTheLastEligiblePaymentsRemainVisibleAfterFileRecoveryAtThreeToSixSeats() throws Exception {
        String lastRasterContext = "Raster fixture not evaluated";
        try {
            for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) for (int players = 3; players <= 6; players++) {
                var fixture = AwardRasterFixture.completed(players, mode);
                var game = fixture.game();

                lastRasterContext = CapturedValue.text(new Object[]{"Fixture before first assertion", "fixture", fixture, "game", game, "mode", mode, "players", players});
                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "fixture", fixture, "mode", mode, "players", players, "fixture contract", "Controlled JVM projection; full immutable context retained on first failure");
                assertTrue("game.snapshot().finalHand() must hold", game.snapshot().finalHand());
                var finalExpected = HistoryRecoveryProof.Expected.current(game);
                if (mode == GameMode.ROMANIAN_WHIST) {
                    assertEquals("game.snapshot().handSize() must equal 1", 1, game.snapshot().handSize());
                    assertTrue("finalExpected.series().stream().allMatch((value)->value == 0) must hold", finalExpected.series().stream().allMatch(value -> value == 0));
                } else {
                    assertEquals("finalExpected.series().get(0).intValue() must equal -10", -10, finalExpected.series().get(0).intValue());
                    assertEquals("finalExpected.series().get(1).intValue() must equal 10", 10, finalExpected.series().get(1).intValue());
                }
                String id = "final-" + mode.name().toLowerCase(java.util.Locale.ROOT) + "-" + players + "p";
                HistoryRecoveryProof.verify(id, game, finalExpected, temporary.getRoot().toPath());
                assertEquals("fixture.eligibleSeries().get(0).intValue() must equal -10", -10, fixture.eligibleSeries().get(0).intValue());
                assertEquals("fixture.eligibleSeries().get(1).intValue() must equal 10", 10, fixture.eligibleSeries().get(1).intValue());
                if (mode == GameMode.ROMANIAN_WHIST)
                    HistoryRecoveryProof.verify(id + "-last-eligible", game,
                            new HistoryRecoveryProof.Expected(fixture.lastEligibleIndex(), fixture.eligibleTotals(), fixture.eligibleSeries()),
                            temporary.getRoot().toPath());
            }
        } catch (Exception | AssertionError failure) {
            atf.gradle.audit.GradleInputs.note("Full first raster failure context", lastRasterContext);
            throw failure;
        }
    }
}
