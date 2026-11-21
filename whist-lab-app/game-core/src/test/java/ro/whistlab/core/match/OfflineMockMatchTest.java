package ro.whistlab.core.match;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.PublicSalvage;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;

/** Offline mock of every bid and public card in a complete four-seat match. */
public final class OfflineMockMatchTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    record BidEvent(int seat, String policy, int bid, int publicBidSumBefore, String strategyProfile,
            String decision, List<MonteCarloAi.BidOption> candidates) { }
    record CardEvent(int sequence, int trick, int seat, String policy, String card,
            int wonBefore, int bid, int needBefore, int opportunities,
            List<String> tableBefore, String decision, Boolean hardContractImpossible,
            int forcedMasterWins, List<MonteCarloAi.Candidate> candidateEstimates,
            int legalOptionCount) { }
    record SeatResult(int seat, String policy, int bid, int tricksWon, int points, int total) { }
    record HandRecord(int hand, int size, String trump, String turnedCard, int dealer,
            List<BidEvent> bids, List<CardEvent> plays, List<SeatResult> results) { }
    record MockMatch(int schemaVersion, String mode, long seed, String policiesBySeat,
            int handsPlayed, int publicCardDecisions, boolean complete, List<HandRecord> hands,
            List<Integer> finalScores, int hardQuotaImpossibleDecisions,
            int hardSalvageFallbackDecisions, int hardQuotaImpossibleNoChoice,
            int hardSalvageCertificateDecisions) { }
    record MatchSummary(long seed, List<Integer> finalScores, int hardExactBids,
            int hardOverbids, int hardUnderbids, int hardTrickPoints,
            int hardImpossibleQuotaDecisions, int hardBurnDecisions,
            int normalExactBids, int normalOverbids, int normalUnderbids,
            int normalTrickPoints, int normalBurnDecisions,
            List<BidSizeSummary> hardBySize, List<BidSizeSummary> normalBySize) { }
    record BidSizeSummary(int handSize, int bids, int exact, int over, int under, int points) { }
    record MatchBatch(int schemaVersion, String mode, String policiesBySeat, long firstSeed,
            int matchCount, int handsPlayed, int publicCardDecisions,
            int hardExactBids, int hardOverbids, int hardUnderbids, int hardTrickPoints,
            int hardImpossibleQuotaDecisions, int hardBurnDecisions,
            int normalExactBids, int normalOverbids, int normalUnderbids,
            int normalTrickPoints, int normalBurnDecisions,
            List<BidSizeSummary> hardBySize, List<BidSizeSummary> normalBySize,
            List<MatchSummary> matches) { }
    record MatchRun(MockMatch match, List<String> privateDealCommitments) { }

    @Test public void runsAndCapturesACompleteFourSeatMockMatch() throws Exception {
        long seed = Long.getLong("whist.offlineMatch.seed", 20261104L);
        int count = Integer.getInteger("whist.offlineMatch.count", 1);
        if (count < 1 || count > 100) throw new IllegalArgumentException("offline match count must be 1..100");
        GameMode mode = GameMode.valueOf(System.getProperty(
                "whist.offlineMatch.mode", GameMode.ROMANIAN_WHIST.name()));
        List<AiDifficulty> roster = readRoster();
        if (count == 1) {
            MatchRun run = playMatch(seed, roster, mode);
            MockMatch match = run.match();
            atf.gradle.audit.GradleInputs.fixture("Observed policy metrics", "seed", seed, "roster", roster,
                    "final scores", match.finalScores(), "impossible quota decisions", match.hardQuotaImpossibleDecisions(),
                    "fallback decisions", match.hardSalvageFallbackDecisions(), "forced plays", match.hardQuotaImpossibleNoChoice(),
                    "salvage certificates", match.hardSalvageCertificateDecisions());
            assertTrue("The match reaches the last score screen", match.complete());
            writeIfRequested(match);
            return;
        }
        List<MatchSummary> summaries = new ArrayList<>();
        int hands = 0, cardDecisions = 0, exact = 0, over = 0, under = 0,
                points = 0, impossible = 0, burns = 0,
                normalExact = 0, normalOver = 0, normalUnder = 0, normalPoints = 0, normalBurns = 0;
        for (int index = 0; index < count; index++) {
            MockMatch match = playMatch(seed + index, roster, mode).match();
            assertTrue("Every batch match reaches the last score screen", match.complete());
            MatchSummary summary = summarize(match);
            summaries.add(summary);
            hands += match.handsPlayed();
            cardDecisions += match.publicCardDecisions();
            exact += summary.hardExactBids();
            over += summary.hardOverbids();
            under += summary.hardUnderbids();
            points += summary.hardTrickPoints();
            impossible += summary.hardImpossibleQuotaDecisions();
            burns += summary.hardBurnDecisions();
            normalExact += summary.normalExactBids();
            normalOver += summary.normalOverbids();
            normalUnder += summary.normalUnderbids();
            normalPoints += summary.normalTrickPoints();
            normalBurns += summary.normalBurnDecisions();
            assertTrue("Normal never enters the Hard-only burn branch", summary.normalBurnDecisions() == 0);
        }
        MatchBatch batch = new MatchBatch(1, mode.name(), rosterLabel(roster), seed,
                count, hands, cardDecisions, exact, over, under, points, impossible, burns,
                normalExact, normalOver, normalUnder, normalPoints, normalBurns,
                aggregateBySize(summaries, true), aggregateBySize(summaries, false),
                List.copyOf(summaries));
        atf.gradle.audit.GradleInputs.fixture("Offline full-match batch", "first seed", seed,
                "matches", count, "roster", roster, "complete hands", hands,
                "public card decisions", cardDecisions, "Hard exact/over/under bids", exact + "/" + over + "/" + under,
                "Hard points", points, "impossible-quota decisions", impossible, "late burns", burns,
                "Hard exact by hand size", batch.hardBySize());
        writeIfRequested(batch);
    }

    private static MatchSummary summarize(MockMatch match) {
        int exact = 0, over = 0, under = 0, points = 0, burns = 0,
                normalExact = 0, normalOver = 0, normalUnder = 0, normalPoints = 0, normalBurns = 0;
        for (HandRecord hand : match.hands()) {
            for (BidEvent bid : hand.bids()) {
                SeatResult result = hand.results().stream().filter(row -> row.seat() == bid.seat())
                        .findFirst().orElseThrow();
                if ("HARD".equals(bid.policy())) {
                    if (result.tricksWon() == bid.bid()) exact++;
                    else if (result.tricksWon() > bid.bid()) over++;
                    else under++;
                    points += result.points();
                } else {
                    if (result.tricksWon() == bid.bid()) normalExact++;
                    else if (result.tricksWon() > bid.bid()) normalOver++;
                    else normalUnder++;
                    normalPoints += result.points();
                }
            }
            burns += (int) hand.plays().stream().filter(play -> "HARD".equals(play.policy())
                    && play.decision().contains("BURN_IF_DOOMED")).count();
            normalBurns += (int) hand.plays().stream().filter(play -> "NORMAL".equals(play.policy())
                    && play.decision().contains("BURN_IF_DOOMED")).count();
        }
        return new MatchSummary(match.seed(), match.finalScores(), exact, over, under, points,
                match.hardQuotaImpossibleDecisions(), burns,
                normalExact, normalOver, normalUnder, normalPoints, normalBurns,
                summarizeBySize(match, "HARD"), summarizeBySize(match, "NORMAL"));
    }

    private static List<BidSizeSummary> aggregateBySize(List<MatchSummary> matches, boolean hard) {
        java.util.Map<Integer, int[]> totals = new java.util.TreeMap<>();
        for (MatchSummary match : matches) {
            List<BidSizeSummary> source = hard ? match.hardBySize() : match.normalBySize();
            for (BidSizeSummary size : source) {
                int[] value = totals.computeIfAbsent(size.handSize(), ignored -> new int[5]);
                value[0] += size.bids();
                value[1] += size.exact();
                value[2] += size.over();
                value[3] += size.under();
                value[4] += size.points();
            }
        }
        return totals.entrySet().stream()
                .map(entry -> new BidSizeSummary(entry.getKey(), entry.getValue()[0],
                        entry.getValue()[1], entry.getValue()[2], entry.getValue()[3], entry.getValue()[4]))
                .toList();
    }

    private static List<BidSizeSummary> summarizeBySize(MockMatch match, String policy) {
        java.util.Map<Integer, int[]> totals = new java.util.TreeMap<>();
        for (HandRecord hand : match.hands()) {
            for (BidEvent bid : hand.bids()) {
                if (!policy.equals(bid.policy())) continue;
                SeatResult result = hand.results().stream().filter(row -> row.seat() == bid.seat())
                        .findFirst().orElseThrow();
                int[] value = totals.computeIfAbsent(hand.size(), ignored -> new int[5]);
                value[0]++;
                if (result.tricksWon() == bid.bid()) value[1]++;
                else if (result.tricksWon() > bid.bid()) value[2]++;
                else value[3]++;
                value[4] += result.points();
            }
        }
        return totals.entrySet().stream()
                .map(entry -> new BidSizeSummary(entry.getKey(), entry.getValue()[0],
                        entry.getValue()[1], entry.getValue()[2], entry.getValue()[3], entry.getValue()[4]))
                .toList();
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster) {
        return playMatch(seed, roster, HardStrategyProfile.CURRENT, GameMode.ROMANIAN_WHIST);
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster, HardStrategyProfile hardProfile) {
        return playMatch(seed, roster, hardProfile, GameMode.ROMANIAN_WHIST);
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster, GameMode mode) {
        return playMatch(seed, roster, HardStrategyProfile.CURRENT, mode);
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster,
            HardStrategyProfile hardProfile, GameMode mode) {
        if (roster.size() != 4 || roster.stream().anyMatch(p -> p != AiDifficulty.NORMAL && p != AiDifficulty.HARD)) {
            throw new IllegalArgumentException("A mock roster must contain four Normal/Hard players");
        }
        List<Integer> expectedHandSizes = GameRules.handSizes(mode, roster.size());
        GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, mode,
                AiDifficulty.NORMAL, seed, mode.isRomanian(), 10)));
        List<HandRecord> hands = new ArrayList<>();
        List<String> privateDealCommitments = new ArrayList<>();
        int totalPlays = 0;
        while (true) {
            GameSession.Snapshot start = game.snapshot();
            privateDealCommitments.add(privateDealCommitment(start));
            List<BidEvent> bids = new ArrayList<>();
            List<CardEvent> plays = new ArrayList<>();
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.biddingSeat();
                AiDifficulty policy = roster.get(seat);
                String profile=policy==AiDifficulty.HARD?hardProfile.name():"NORMAL_BASELINE";
                List<MonteCarloAi.BidOption> bidCandidates=List.of();
                String bidDecision="Normal isolated strength estimate";
                int bid;
                if(policy==AiDifficulty.HARD){
                    MonteCarloAi.BidDecision choice=MonteCarloAi.chooseBid(MonteCarloAi.observe(before,seat),hardProfile);
                    bid=choice.bid();bidCandidates=choice.candidates();bidDecision=choice.method();
                }else bid=AiPolicy.chooseBid(before, seat, policy);
                bids.add(new BidEvent(seat, policy.name(), bid, before.bidTotal(),profile,bidDecision,bidCandidates));
                game.submitBid(seat, bid);
            }
            while (game.snapshot().phase() == GamePhase.PLAYING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.currentSeat();
                AiDifficulty policy = roster.get(seat);
                GameSession.PlayerSnapshot actor = before.players().get(seat);
                List<String> table = before.currentTrick().stream()
                        .map(play -> play.seat() + ":" + play.card().shortLabel()).toList();
                int need = actor.bid() - actor.tricksWon();
                int opportunities = (before.players().stream().mapToInt(p -> p.hand().size()).sum()
                        + before.currentTrick().size()) / before.players().size();
                int legalOptions = GameRules.legalCards(actor.hand(),
                        before.currentTrick().isEmpty() ? null : before.currentTrick().get(0).card().suit(),
                        before.trump(), before.mode()).size();
                Boolean impossible = null;
                int forced = 0;
                List<MonteCarloAi.Candidate> candidateEstimates = List.of();
                Card card;
                String decision;
                if (policy == AiDifficulty.HARD) {
                    MonteCarloAi.View view = MonteCarloAi.observe(before, seat);
                    PublicSalvage.Quota quota = PublicSalvage.quota(view);
                    impossible = quota.impossible();
                    forced = quota.forcedMasterTrumps();
                    MonteCarloAi.Decision choice = hardProfile == HardStrategyProfile.CURRENT
                            ? MonteCarloAi.chooseCard(view)
                            : MonteCarloAi.chooseCard(view,hardProfile);
                    card = choice.card();
                    candidateEstimates = choice.candidates();
                    decision = choice.method();
                } else {
                    card = AiPolicy.chooseCard(before, seat, policy);
                    decision = "Normal contract-first legal heuristic";
                }
                plays.add(new CardEvent(totalPlays + plays.size() + 1,
                        before.history().size() / before.players().size() + 1, seat, policy.name(),
                        card.shortLabel(), actor.tricksWon(), actor.bid(), need, opportunities,
                        table, decision, impossible, forced, candidateEstimates, legalOptions));
                game.playCard(seat, card);
            }
            GameSession.Snapshot scored = game.snapshot();
            if (scored.phase() != GamePhase.SCORE) throw new IllegalStateException("Hand did not score");
            int handNumber = scored.handNumber();
            List<SeatResult> results = new ArrayList<>();
            for (GameSession.PlayerSnapshot player : scored.players()) {
                results.add(new SeatResult(player.seat(), roster.get(player.seat()).name(),
                        game.scoreHistory().bid(handNumber, player.seat()), player.tricksWon(),
                        game.scoreHistory().points(handNumber, player.seat()), player.totalScore()));
            }
            totalPlays += plays.size();
            hands.add(new HandRecord(handNumber, scored.handSize(), scored.trump().name(),
                    scored.turnedCard() == null ? null : scored.turnedCard().shortLabel(),
                    scored.dealerSeat(), bids, plays, results));
            if (scored.finalHand()) break;
            game.startNextHand();
        }
        GameSession.Snapshot finished = game.snapshot();
        assertEquals("hands.size() must equal the configured schedule", expectedHandSizes.size(), hands.size());
        assertEquals("totalPlays must equal the configured schedule times four seats",
                expectedHandSizes.stream().mapToInt(Integer::intValue).sum() * roster.size(), totalPlays);
        for (int index = 0; index < expectedHandSizes.size(); index++) {
            assertEquals("each match hand follows the ruleset's declared schedule",
                    expectedHandSizes.get(index).intValue(), hands.get(index).size());
        }
        assertTrue("hands.stream().allMatch((hand)->hand.bids().size() == 4 && hand.plays().size() == hand.size() * 4 && hand.results().size() == 4) must hold", hands.stream().allMatch(hand -> hand.bids().size() == 4
                && hand.plays().size() == hand.size() * 4 && hand.results().size() == 4));
        List<Integer> finalScores = finished.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
        List<CardEvent> hardPlays = hands.stream().flatMap(hand -> hand.plays().stream())
                .filter(play -> "HARD".equals(play.policy())).toList();
        int impossibleCount = (int) hardPlays.stream().filter(play -> Boolean.TRUE.equals(play.hardContractImpossible())).count();
        int fallbackCount = (int) hardPlays.stream().filter(play -> play.decision().contains("publicly unreachable contract")).count();
        int noChoiceCount = (int) hardPlays.stream().filter(play -> Boolean.TRUE.equals(play.hardContractImpossible())
                && play.legalOptionCount() == 1).count();
        int certificateCount = (int) hardPlays.stream().filter(play -> play.decision().contains("public salvage dominance certificate")).count();
        MockMatch result = new MockMatch(3, mode.name(), seed,
                rosterLabel(roster), hands.size(), totalPlays,
                finished.phase() == GamePhase.SCORE && finished.finalHand(), hands, finalScores,
                impossibleCount, fallbackCount, noChoiceCount, certificateCount);
        return new MatchRun(result, List.copyOf(privateDealCommitments));
    }

    private static List<AiDifficulty> readRoster() {
        String configured = System.getProperty("whist.offlineMatch.roster", "NORMAL,HARD,NORMAL,HARD");
        List<AiDifficulty> roster = java.util.Arrays.stream(configured.split(","))
                .map(String::strip).map(AiDifficulty::valueOf).toList();
        if (roster.size() != 4) throw new IllegalArgumentException("whist.offlineMatch.roster needs four seats");
        return roster;
    }

    static String rosterLabel(List<AiDifficulty> roster) {
        return java.util.stream.IntStream.range(0, roster.size())
                .mapToObj(seat -> "seat" + seat + "=" + roster.get(seat).name())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String privateDealCommitment(GameSession.Snapshot game) {
        StringBuilder canonical = new StringBuilder().append(game.handNumber()).append('|')
                .append(game.dealerSeat()).append('|').append(game.trump().name()).append('|');
        if (game.turnedCard() != null) canonical.append(game.turnedCard().shortLabel());
        for (GameSession.PlayerSnapshot player : game.players()) {
            canonical.append('|').append(player.seat()).append(':');
            player.hand().stream().sorted(Comparator.comparing(Card::suit).thenComparingInt(Card::rank))
                    .forEach(card -> canonical.append(card.suit().name()).append(card.rank()).append(','));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static void writeIfRequested(Object result) throws Exception {
        String configured = System.getProperty("whist.offlineMatch.output", "").strip();
        if (configured.isEmpty()) return;
        Path output = Path.of(configured);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n",
                StandardOpenOption.CREATE_NEW);
    }
}
