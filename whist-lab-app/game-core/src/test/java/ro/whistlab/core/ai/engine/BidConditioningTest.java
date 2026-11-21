package ro.whistlab.core.ai.engine;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.ai.policy.HandTrickPotential;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.ai.profile.HardStrategyProfile;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;
import org.junit.Test;

/** Small public-information regressions for honor cover and bid-conditioned deal weights. */
public final class BidConditioningTest extends atf.gradle.audit.GradleAuditedTest {
    private static final List<PlayedCard> FIRST_TRICK = List.of(
            new PlayedCard(3, c(Suit.CLUBS, 14)),
            new PlayedCard(0, c(Suit.CLUBS, 10)),
            new PlayedCard(1, c(Suit.CLUBS, 7)),
            new PlayedCard(2, c(Suit.CLUBS, 8)));

    @Test public void aCoveredKingCarriesNearlyAceLevelPotentialWithoutBecomingACertainty() {
        List<Card> coveredKing = List.of(c(Suit.SPADES, 13), c(Suit.SPADES, 7));
        List<Card> bareKing = List.of(c(Suit.SPADES, 13));
        List<Card> ace = List.of(c(Suit.SPADES, 14), c(Suit.DIAMONDS, 7));
        double covered = HandTrickPotential.score(coveredKing, Trump.NO_TRUMP);
        double uncovered = HandTrickPotential.score(bareKing, Trump.NO_TRUMP);
        double aceStrength = HandTrickPotential.score(ace, Trump.NO_TRUMP);

        atf.gradle.audit.GradleInputs.fixture("Covered honor is a potential, not a forced win",
                "covered king", coveredKing, "bare king", bareKing, "ace comparison", ace,
                "covered score", covered, "ace score", aceStrength);
        assertTrue("The cover should materially raise the king's hand value", covered >= uncovered + .23);
        assertTrue("A king with one same-suit cover stays close to an ace", aceStrength - covered <= .19);
        assertEquals("Both holdings remain an estimate of one trick, not a guarantee", 1,
                HandTrickPotential.estimate(coveredKing, Trump.NO_TRUMP));
    }

    @Test public void shortSideSuitsGainOnlyModestRuffFlexibilityWhenTrumpIsHeld() {
        List<Card> flexible = List.of(c(Suit.SPADES, 8), c(Suit.HEARTS, 9),
                c(Suit.HEARTS, 10), c(Suit.DIAMONDS, 14));
        List<Card> distributed = List.of(c(Suit.SPADES, 8), c(Suit.HEARTS, 9),
                c(Suit.DIAMONDS, 10), c(Suit.CLUBS, 14));
        List<Card> noTrump = List.of(c(Suit.CLUBS, 8), c(Suit.HEARTS, 9),
                c(Suit.HEARTS, 10), c(Suit.DIAMONDS, 14));

        double flexibleValue = HandTrickPotential.score(flexible, Trump.SPADES);
        double distributedValue = HandTrickPotential.score(distributed, Trump.SPADES);
        double noTrumpValue = HandTrickPotential.score(noTrump, Trump.SPADES);

        atf.gradle.audit.GradleInputs.fixture("A void and singleton are conditional ruff options, not certain tricks",
                "one trump with void/singleton", flexible, "same ranks spread across suits", distributed,
                "same shape without a trump", noTrump, "flexibility score", flexibleValue,
                "distributed score", distributedValue, "no-trump score", noTrumpValue);
        assertTrue("A trump-backed void modestly improves the bid-time hand estimate",
                flexibleValue > distributedValue + .07);
        assertEquals("Short suits add no ruff value without holding a trump",
                distributedValue - .145, noTrumpValue, 1e-12);
    }

    @Test public void publicBidsPreferCompatibleDealsButRetainSupportForUnusualHands() {
        MonteCarloAi.View view = beforeFirstTrick();
        List<List<Card>> strongBidDeal = deal(true, false);
        List<List<Card>> weakBidDeal = deal(false, false);
        double strongWeight = BidConditioning.likelihood(view, strongBidDeal);
        double weakWeight = BidConditioning.likelihood(view, weakBidDeal);

        atf.gradle.audit.GradleInputs.fixture("Auction clues weight only hypothetical original hands",
                "view", view, "strong-bid hand", strongBidDeal.get(0),
                "weak-bid hand", weakBidDeal.get(0), "strong weight", strongWeight,
                "weak weight", weakWeight, "public clues", BidConditioning.evidenceCount(view));
        assertEquals("The three earlier non-forced bids are observable clues", 3,
                BidConditioning.evidenceCount(view));
        assertTrue("A deal that better explains a high bid should receive more posterior weight",
                strongWeight > weakWeight);
        assertTrue("Soft evidence must not eliminate a deal solely because of a surprising bid",
                weakWeight > 0);
        assertTrue("The bounded mixture prevents a single bid from becoming a card reveal",
                strongWeight / weakWeight < 8);
    }

    @Test public void twoCardBidEvidenceIsMoreConservativeThanTheLongerHandModel() {
        MonteCarloAi.View twoCards = twoCardView();
        List<List<Card>> strongTwo = twoCardDeal(true);
        List<List<Card>> weakTwo = twoCardDeal(false);
        double twoCardRatio = BidConditioning.likelihood(twoCards, strongTwo)
                / BidConditioning.likelihood(twoCards, weakTwo);
        MonteCarloAi.View fourCards = beforeFirstTrick();
        double fourCardRatio = BidConditioning.likelihood(fourCards, deal(true, false))
                / BidConditioning.likelihood(fourCards, deal(false, false));

        atf.gradle.audit.GradleInputs.fixture("Small-hand auction clues remain bounded when one trick dominates",
                "two-card view", twoCards, "strong/weak two-card deals", strongTwo + " / " + weakTwo,
                "two-card likelihood ratio", twoCardRatio, "four-card likelihood ratio", fourCardRatio);
        assertTrue("The known bid is still evidence at two cards", twoCardRatio > 1.0);
        assertTrue("The two-card likelihood is less aggressive than the longer-hand model",
                twoCardRatio < fourCardRatio);
        assertTrue("The conservative mixture still keeps a surprising hand possible", weakTwo != null
                && BidConditioning.likelihood(twoCards, weakTwo) > 0);
    }

    @Test public void completedPublicCardsReconstructTheBidTimeHandWithoutDoubleCounting() {
        MonteCarloAi.View before = beforeFirstTrick();
        MonteCarloAi.View after = afterFirstTrick();
        List<List<Card>> strongBefore = deal(true, false);
        List<List<Card>> weakBefore = deal(false, false);
        List<List<Card>> strongAfter = deal(true, true);
        List<List<Card>> weakAfter = deal(false, true);

        double strongInitialWeight = BidConditioning.likelihood(before, strongBefore);
        double strongUpdatedWeight = BidConditioning.likelihood(after, strongAfter);
        double weakInitialWeight = BidConditioning.likelihood(before, weakBefore);
        double weakUpdatedWeight = BidConditioning.likelihood(after, weakAfter);

        atf.gradle.audit.GradleInputs.fixture("Played cards remain public evidence about the original auction hand",
                "before", before, "after", after, "strong initial", strongBefore,
                "strong remaining", strongAfter, "weak initial", weakBefore,
                "weak remaining", weakAfter);
        assertEquals("The full trick is restored once when rebuilding seat 0's bid-time hand",
                strongInitialWeight, strongUpdatedWeight, 1e-12);
        assertEquals("Distinct public plays reconstruct each bid-time hand once",
                weakInitialWeight, weakUpdatedWeight, 1e-12);
        assertTrue("The same public bid still distinguishes the two compatible original holdings",
                strongUpdatedWeight > weakUpdatedWeight);
        assertEquals("After the trick, all four seats have three cards", 3,
                after.seats().stream().mapToInt(MonteCarloAi.Seat::remaining).min().orElseThrow());
    }

    @Test public void aCardInTheUnfinishedTrickIsAuctionEvidenceEvenIfHistoryOmitsItsSuffix() {
        Card lead = FIRST_TRICK.get(0).card();
        List<MonteCarloAi.Seat> seats = List.of(new MonteCarloAi.Seat(4, 0, 0),
                new MonteCarloAi.Seat(4, 1, 0), new MonteCarloAi.Seat(4, 0, 0),
                new MonteCarloAi.Seat(3, 0, 0));
        List<Card> ownRemaining = new ArrayList<>(deal(true, false).get(3));
        ownRemaining.remove(lead);
        MonteCarloAi.View trickOnly = new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,
                Trump.SPADES, 3, 2, 4, ownRemaining, seats, List.of(FIRST_TRICK.get(0)),
                List.of(), c(Suit.SPADES, 7));
        MonteCarloAi.View historyAndTrick = new MonteCarloAi.View(GameMode.ROMANIAN_WHIST,
                Trump.SPADES, 3, 2, 4, ownRemaining, seats, List.of(FIRST_TRICK.get(0)),
                List.of(FIRST_TRICK.get(0)), c(Suit.SPADES, 7));
        List<List<Card>> possibleHands = deal(true, false);
        List<Card> actorHand = new ArrayList<>(possibleHands.get(3));
        actorHand.remove(lead);
        possibleHands = new ArrayList<>(possibleHands);
        possibleHands.set(3, List.copyOf(actorHand));

        double trickEvidence = BidConditioning.likelihood(trickOnly, possibleHands);
        double deduplicatedEvidence = BidConditioning.likelihood(historyAndTrick, possibleHands);

        atf.gradle.audit.GradleInputs.fixture("The active trick restores the bid-time hand exactly once",
                "lead", lead, "trick without history suffix", trickOnly,
                "history already containing the lead", historyAndTrick,
                "possible remaining hands", possibleHands);
        assertEquals("History and current trick must not duplicate the same public card",
                trickEvidence, deduplicatedEvidence, 1e-12);
        assertEquals("The three earlier opponents still contribute public bid clues", 3,
                BidConditioning.evidenceCount(trickOnly));
    }

    @Test public void missingBidPrefixIsNotInventedAsEvidence() {
        MonteCarloAi.View complete = beforeFirstTrick();
        List<MonteCarloAi.Seat> seats = new ArrayList<>(complete.seats());
        seats.set(2, new MonteCarloAi.Seat(4, null, 0));
        MonteCarloAi.View incomplete = new MonteCarloAi.View(complete.mode(), complete.trump(),
                complete.seat(), complete.dealer(), complete.handSize(), complete.hand(), seats,
                complete.trick(), complete.history(), complete.turnedCard());

        atf.gradle.audit.GradleInputs.fixture("Only the observed auction prefix may condition deals",
                "complete", complete, "incomplete", incomplete);
        assertEquals("Only the first two opponent bids have a complete preceding prefix", 2,
                BidConditioning.evidenceCount(incomplete));
    }

    @Test public void forbiddenLastBidIsIgnoredButALegalLastBidRemainsEvidence() {
        MonteCarloAi.View forbidden = auctionView(2);
        MonteCarloAi.View missing = auctionView(null);
        MonteCarloAi.View legal = auctionView(3);

        atf.gradle.audit.GradleInputs.fixture("The final bidder's forbidden total is not treated as a behavioral clue",
                "forbidden options", ro.whistlab.core.rules.GameRules.allowedBids(4, 2, true),
                "forbidden final bid", forbidden, "missing final bid", missing,
                "legal final bid", legal);
        assertTrue("At four cards, the final bid that completes the hand-size total is excluded",
                !ro.whistlab.core.rules.GameRules.allowedBids(4, 2, true).contains(2));
        assertEquals("The two earlier legal bids still condition the hypothetical deal", 2,
                BidConditioning.evidenceCount(forbidden));
        assertEquals("An impossible final bid contributes exactly as no observation does",
                BidConditioning.likelihood(missing, deal(true, false)),
                BidConditioning.likelihood(forbidden, deal(true, false)), 1e-12);
        assertEquals("A legal final bid is counted after the same known prefix", 3,
                BidConditioning.evidenceCount(legal));
    }

    @Test public void aLegalLastBidUpdatesThePosteriorFromTheBidderHandNotThePositionAlone() {
        MonteCarloAi.View view = auctionView(3);
        List<List<Card>> strongFinalBid = List.of(
                deal(true, false).get(0), deal(true, false).get(1),
                List.of(c(Suit.SPADES, 14), c(Suit.SPADES, 13), c(Suit.SPADES, 12), c(Suit.SPADES, 11)),
                deal(true, false).get(3));
        List<List<Card>> weakFinalBid = List.of(
                deal(true, false).get(0), deal(true, false).get(1),
                List.of(c(Suit.DIAMONDS, 7), c(Suit.DIAMONDS, 8), c(Suit.HEARTS, 7), c(Suit.HEARTS, 8)),
                deal(true, false).get(3));
        double strongWeight = BidConditioning.likelihood(view, strongFinalBid);
        double weakWeight = BidConditioning.likelihood(view, weakFinalBid);

        atf.gradle.audit.GradleInputs.fixture("A legal last bid conditions on the cards attributed to its bidder",
                "view", view, "strong final bidder hand", strongFinalBid.get(2),
                "weak final bidder hand", weakFinalBid.get(2),
                "strong likelihood", strongWeight, "weak likelihood", weakWeight);
        assertTrue("A high legal last bid is better explained by the strong hand", strongWeight > weakWeight);
        assertTrue("The soft posterior retains the low-strength alternative", weakWeight > 0);
    }

    @Test public void romanianBidLikelihoodDoesNotLeakIntoOhHell() {
        MonteCarloAi.View romanian = beforeFirstTrick();
        MonteCarloAi.View ohHell = new MonteCarloAi.View(GameMode.OH_HELL, romanian.trump(),
                romanian.seat(), romanian.dealer(), romanian.handSize(), romanian.hand(),
                romanian.seats(), romanian.trick(), romanian.history(), romanian.turnedCard());

        atf.gradle.audit.GradleInputs.fixture("Romanian soft bid beliefs are scoped to Romanian Whist",
                "Romanian view", romanian, "other ruleset view", ohHell,
                "candidate hands", deal(true, false));
        assertEquals("The Romanian-only belief model reports no evidence in Oh Hell", 0,
                BidConditioning.evidenceCount(ohHell));
        assertEquals("Unsupported rulesets retain the neutral world weight", 1.0,
                BidConditioning.likelihood(ohHell, deal(true, false)), 0.0);
    }

    @Test public void auctionInferenceAndLateBurnStayScopedToCalibrated181() {
        MonteCarloAi.View source = beforeFirstTrick();
        List<List<Card>> candidates = deal(true, false);
        for (GameMode mode : GameMode.values()) {
            if (mode == GameMode.ROMANIAN_WHIST) continue;
            MonteCarloAi.View otherRules = new MonteCarloAi.View(mode, source.trump(),
                    source.seat(), source.dealer(), source.handSize(), source.hand(),
                    source.seats(), source.trick(), source.history(), source.turnedCard());

            atf.gradle.audit.GradleInputs.fixture("Romanian calibration cannot silently alter another ruleset",
                    "ruleset", mode, "observations", otherRules, "candidate deal", candidates);
            assertEquals("unsupported rulesets contribute no Romanian auction clues", 0,
                    BidConditioning.evidenceCount(otherRules));
            assertEquals("unsupported rulesets retain neutral deal likelihood", 1.0,
                    BidConditioning.likelihood(otherRules, candidates), 0.0);
            assertEquals("Romanian late-burn policy is not inherited by another ruleset",
                    HardStrategyProfile.CURRENT, MonteCarloAi.runtimeProfile(otherRules));
        }
    }

    @Test public void longRomanianAuctionEvidenceRemainsNeutralUntilItsOwnCalibrationExists() {
        List<Card> deck = GameRules.deck(GameMode.LONG_ROMANIAN, 4);
        for (int handSize = 1; handSize <= 8; handSize++) {
            List<List<Card>> possibleHands = new ArrayList<>();
            List<MonteCarloAi.Seat> seats = new ArrayList<>();
            for (int seat = 0; seat < 4; seat++) {
                possibleHands.add(List.copyOf(deck.subList(seat * handSize, (seat + 1) * handSize)));
                seats.add(new MonteCarloAi.Seat(handSize, seat == 0 ? 1 : 0, 0));
            }
            MonteCarloAi.View view = new MonteCarloAi.View(GameMode.LONG_ROMANIAN, Trump.HEARTS,
                    3, 3, handSize, possibleHands.get(3), seats, List.of(), List.of(), null);
            double likelihood = BidConditioning.likelihood(view, possibleHands);

            atf.gradle.audit.GradleInputs.fixture("Long Romanian is not inferred from a different schedule calibration",
                    "cards per hand", handSize, "view", view,
                    "candidate hands", possibleHands, "eligible bids", BidConditioning.evidenceCount(view),
                    "world likelihood", likelihood);
            assertEquals("Long Romanian contributes no uncalibrated auction clues", 0,
                    BidConditioning.evidenceCount(view));
            assertEquals("Long Romanian leaves every sampled world equally weighted", 1.0,
                    likelihood, 0.0);
        }
    }

    @Test public void absentOrIncompleteAuctionEvidenceLeavesWorldWeightsNeutral() {
        MonteCarloAi.View complete = beforeFirstTrick();
        List<MonteCarloAi.Seat> noBids = complete.seats().stream()
                .map(seat -> new MonteCarloAi.Seat(seat.remaining(), null, seat.won()))
                .toList();
        MonteCarloAi.View noEvidence = new MonteCarloAi.View(complete.mode(), complete.trump(),
                complete.seat(), complete.dealer(), complete.handSize(), complete.hand(), noBids,
                complete.trick(), complete.history(), complete.turnedCard());
        List<List<Card>> candidates = deal(true, false);

        atf.gradle.audit.GradleInputs.fixture("No observed legal auction evidence has zero posterior influence",
                "no-bid observation", noEvidence, "wrong-size world", candidates.subList(0, 3));
        assertEquals("a missing bid prefix is not inferred", 0,
                BidConditioning.evidenceCount(noEvidence));
        assertEquals("a missing bid prefix leaves all worlds equally weighted", 1.0,
                BidConditioning.likelihood(noEvidence, candidates), 0.0);
        assertEquals("incomplete sampled worlds are rejected as evidence, not assigned partial likelihood",
                1.0, BidConditioning.likelihood(complete, candidates.subList(0, 3)), 0.0);
    }

    @Test public void fourSeatBidConditioningStaysNeutralAtOtherRomanianTableSizes() {
        for (GameMode mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
          for (int playerCount : List.of(3, 5, 6)) {
            List<Card> deck = GameRules.deck(mode, playerCount);
            List<List<Card>> hands = new ArrayList<>();
            List<MonteCarloAi.Seat> seats = new ArrayList<>();
            for (int seat = 0; seat < playerCount; seat++) {
                hands.add(List.copyOf(deck.subList(seat * 4, seat * 4 + 4)));
                seats.add(new MonteCarloAi.Seat(4, 1, 0));
            }
            MonteCarloAi.View view = new MonteCarloAi.View(mode, Trump.SPADES,
                    0, playerCount - 1, 4, hands.get(0), seats, List.of(), List.of(), null);

            atf.gradle.audit.GradleInputs.fixture("Four-seat bid beliefs stay scoped to four-player Romanian Whist",
                    "ruleset", mode, "player count", playerCount, "public view", view,
                    "hypothetical hands", hands);
            assertEquals("Non-four-seat Romanian tables do not use this bid likelihood", 0,
                    BidConditioning.evidenceCount(view));
            assertEquals("Non-four-seat worlds retain neutral weight", 1.0,
                    BidConditioning.likelihood(view, hands), 0.0);
          }
        }
    }

    @Test public void weightedBidSamplesReportTheEffectiveRatherThanRawSampleCount() {
        MonteCarloAi.BidSamples samples = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
        samples.add(new double[]{10, 0}, new double[]{.9, .1}, 1);
        samples.add(new double[]{0, 10}, new double[]{.1, .9}, 3);
        List<MonteCarloAi.BidOption> options = samples.options();

        atf.gradle.audit.GradleInputs.fixture("Importance weights govern the estimate and its confidence budget",
                "options", options, "effective samples", samples.effectiveSamples());
        assertEquals("The 1:3 weighted mean for bid 1 is seven and a half", 7.5,
                options.get(1).expectedPoints(), 1e-12);
        assertEquals("Raw samples remain auditable", 2, options.get(1).samples());
        assertEquals("Effective samples account for weight concentration", 1.6,
                samples.effectiveSamples(), 1e-12);
    }

    @Test public void lowWeightWorldsCannotInflateTheBidStoppingConfidence() {
        MonteCarloAi.BidSamples samples = new MonteCarloAi.BidSamples(List.of(0, 1), 2, false);
        for (int index = 0; index < 32; index++) {
            boolean firstLeads = index < 18;
            samples.add(firstLeads ? new double[]{1, 0} : new double[]{0, 1},
                    new double[]{.5, .5}, 1.0);
        }
        for (int index = 0; index < 512; index++) {
            samples.add(new double[]{1, 0}, new double[]{.5, .5}, .001);
        }

        atf.gradle.audit.GradleInputs.fixture("Many negligible worlds do not impersonate independent evidence",
                "raw samples", samples.samples(), "effective samples", samples.effectiveSamples(),
                "weighted options", samples.options());
        assertTrue("the fixture contains many raw observations", samples.samples() > 128);
        assertTrue("the highly concentrated weights retain a small effective sample size",
                samples.effectiveSamples() < 40);
        assertTrue("the unresolved weighted margin keeps sampling instead of stopping on raw count",
                !samples.enough());
    }

    @Test public void productionHardBurnsOnlyInTheLastTwoTricksAfterPublicImpossibility() {
        MonteCarloAi.View impossibleLate = quotaView(2, 0);
        MonteCarloAi.View impossibleEarly = quotaView(4, 0);
        MonteCarloAi.View stillRecoverable = quotaView(2, 1);
        MonteCarloAi.View impossibleLateLong = withMode(impossibleLate, GameMode.LONG_ROMANIAN);
        MonteCarloAi.View otherMode = new MonteCarloAi.View(GameMode.OH_HELL, impossibleLate.trump(),
                impossibleLate.seat(), impossibleLate.dealer(), impossibleLate.handSize(),
                impossibleLate.hand(), impossibleLate.seats(), impossibleLate.trick(),
                impossibleLate.history(), impossibleLate.turnedCard());

        atf.gradle.audit.GradleInputs.fixture("Late Hard switch is gated by proof and remaining opportunities",
                "late quota", PublicSalvage.quota(impossibleLate), "early quota", PublicSalvage.quota(impossibleEarly),
                "recoverable quota", PublicSalvage.quota(stillRecoverable));
        assertEquals("Impossible contract at the last two tricks enables the burn profile",
                HardStrategyProfile.BURN_IF_DOOMED, MonteCarloAi.runtimeProfile(impossibleLate));
        assertEquals("Long Romanian keeps its own late-burn calibration boundary",
                HardStrategyProfile.CURRENT, MonteCarloAi.runtimeProfile(impossibleLateLong));
        assertEquals("An impossible contract earlier in the hand keeps the ordinary Hard order",
                HardStrategyProfile.CURRENT, MonteCarloAi.runtimeProfile(impossibleEarly));
        assertEquals("A recoverable contract never burns opponents to raise their miss count",
                HardStrategyProfile.CURRENT, MonteCarloAi.runtimeProfile(stillRecoverable));
        assertEquals("The Romanian-only salvage switch does not leak into Oh Hell",
                HardStrategyProfile.CURRENT, MonteCarloAi.runtimeProfile(otherMode));
        assertTrue("The production Hard decision records the activated late burn profile",
                MonteCarloAi.chooseCard(impossibleLate).method().contains("BURN_IF_DOOMED"));
    }

    private static MonteCarloAi.View beforeFirstTrick() {
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 3, 2, 4,
                List.of(c(Suit.CLUBS, 14), c(Suit.HEARTS, 10), c(Suit.HEARTS, 11), c(Suit.HEARTS, 12)),
                List.of(new MonteCarloAi.Seat(4, 2, 0), new MonteCarloAi.Seat(4, 0, 0),
                        new MonteCarloAi.Seat(4, 1, 0), new MonteCarloAi.Seat(4, 0, 0)),
                List.of(), List.of(), c(Suit.SPADES, 7));
    }

    private static MonteCarloAi.View withMode(MonteCarloAi.View source, GameMode mode) {
        return new MonteCarloAi.View(mode, source.trump(), source.seat(), source.dealer(),
                source.handSize(), source.hand(), source.seats(), source.trick(),
                source.history(), source.turnedCard());
    }

    private static MonteCarloAi.View quotaView(int remaining, int bid) {
        List<MonteCarloAi.Seat> seats = List.of(
                new MonteCarloAi.Seat(remaining, null, 0), new MonteCarloAi.Seat(remaining, null, 0),
                new MonteCarloAi.Seat(remaining, null, 0), new MonteCarloAi.Seat(remaining, bid, 0));
        List<Card> hand = remaining == 2
                ? List.of(c(Suit.SPADES, 14), c(Suit.HEARTS, 8))
                : List.of(c(Suit.SPADES, 14), c(Suit.HEARTS, 8), c(Suit.CLUBS, 9), c(Suit.DIAMONDS, 10));
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 3, 2, 4,
                hand, seats, List.of(), List.of(), c(Suit.SPADES, 7));
    }

    private static MonteCarloAi.View twoCardView() {
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 3, 2, 2,
                twoCardDeal(false).get(3),
                List.of(new MonteCarloAi.Seat(2, 1, 0), new MonteCarloAi.Seat(2, 0, 0),
                        new MonteCarloAi.Seat(2, 0, 0), new MonteCarloAi.Seat(2, 0, 0)),
                List.of(), List.of(), c(Suit.SPADES, 7));
    }

    private static MonteCarloAi.View auctionView(Integer lastBid) {
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 3, 2, 4,
                deal(true, false).get(3),
                List.of(new MonteCarloAi.Seat(4, 1, 0), new MonteCarloAi.Seat(4, 1, 0),
                        new MonteCarloAi.Seat(4, lastBid, 0), new MonteCarloAi.Seat(4, 0, 0)),
                List.of(), List.of(), c(Suit.SPADES, 7));
    }

    private static List<List<Card>> twoCardDeal(boolean strongSpades) {
        return List.of(
                strongSpades
                        ? List.of(c(Suit.SPADES, 14), c(Suit.CLUBS, 7))
                        : List.of(c(Suit.SPADES, 8), c(Suit.CLUBS, 9)),
                List.of(c(Suit.DIAMONDS, 13), c(Suit.HEARTS, 7)),
                List.of(c(Suit.DIAMONDS, 12), c(Suit.HEARTS, 8)),
                List.of(c(Suit.HEARTS, 10), c(Suit.CLUBS, 8)));
    }

    private static MonteCarloAi.View afterFirstTrick() {
        return new MonteCarloAi.View(GameMode.ROMANIAN_WHIST, Trump.SPADES, 3, 2, 4,
                List.of(c(Suit.HEARTS, 10), c(Suit.HEARTS, 11), c(Suit.HEARTS, 12)),
                List.of(new MonteCarloAi.Seat(3, 2, 0), new MonteCarloAi.Seat(3, 0, 0),
                        new MonteCarloAi.Seat(3, 1, 0), new MonteCarloAi.Seat(3, 0, 1)),
                List.of(), FIRST_TRICK, c(Suit.SPADES, 7));
    }

    private static List<List<Card>> deal(boolean strongSpades, boolean afterFirstTrick) {
        List<List<Card>> initial = List.of(
                strongSpades
                        ? List.of(c(Suit.CLUBS, 10), c(Suit.SPADES, 14), c(Suit.SPADES, 13), c(Suit.SPADES, 12))
                        : List.of(c(Suit.CLUBS, 10), c(Suit.SPADES, 8), c(Suit.SPADES, 9), c(Suit.SPADES, 11)),
                List.of(c(Suit.CLUBS, 7), c(Suit.HEARTS, 7), c(Suit.HEARTS, 8), c(Suit.HEARTS, 9)),
                List.of(c(Suit.CLUBS, 8), c(Suit.DIAMONDS, 7), c(Suit.DIAMONDS, 8), c(Suit.DIAMONDS, 9)),
                List.of(c(Suit.CLUBS, 14), c(Suit.HEARTS, 10), c(Suit.HEARTS, 11), c(Suit.HEARTS, 12)));
        if (!afterFirstTrick) return initial;
        List<List<Card>> remaining = new ArrayList<>();
        List<Card> playedBySeat = List.of(c(Suit.CLUBS, 10), c(Suit.CLUBS, 7),
                c(Suit.CLUBS, 8), c(Suit.CLUBS, 14));
        for (int seat = 0; seat < initial.size(); seat++) {
            List<Card> hand = new ArrayList<>(initial.get(seat));
            hand.remove(playedBySeat.get(seat));
            remaining.add(List.copyOf(hand));
        }
        return List.copyOf(remaining);
    }

    private static Card c(Suit suit, int rank) { return new Card(suit, rank); }
}
