package ro.whistlab.core.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import org.junit.Test;

/** Paired A/B/C matches; only one experimental Hard seat changes in each run. */
public final class OfflineAbcModeMatchTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<Arm> ARMS = List.of(
            new Arm("A_CURRENT", HardStrategyProfile.CURRENT),
            new Arm("B_SCORE_FIRST", HardStrategyProfile.SCORE_FIRST),
            new Arm("C_PRUDENT_BID_08", HardStrategyProfile.PRUDENT_BID));
    private static final String DEFAULT_MODES =
            "LONG_ROMANIAN,OH_HELL,DESCENDING,DOWN_AND_UP,REVERSE_OH_HELL";

    record Arm(String name, HardStrategyProfile profile) { }
    record FocalResult(String arm, String profile, int seat, int points, int finalScore,
            int exactContracts, int overtricks, int undertricks, boolean topTwo,
            int bidChangesVsA, int cardChangesVsA) { }
    record ModeResult(String mode, long seed, int handsPerMatch, int matches,
            int opaqueDealCommitmentsCompared, String opaqueDealDigest,
            boolean sameDistributionAndOrder, boolean privateDealsIncluded,
            List<FocalResult> focalResults) { }
    record AbcMatrix(int schemaVersion, String design, List<String> arms,
            List<String> modes, List<Long> baseSeeds, List<ModeResult> results,
            boolean privateDealsIncluded) { }
    record SeatMetrics(int points, int finalScore, int exact, int over, int under,
            boolean topTwo) { }

    @Test public void comparesOneFocalHardPlayerAtATimeAcrossNon181Modes() throws Exception {
        List<Long> baseSeeds = readSeeds();
        List<GameMode> modes = readModes();
        if (modes.isEmpty()) throw new IllegalArgumentException("At least one non-1-8-1 mode is required");

        List<ModeResult> results = new ArrayList<>();
        for (GameMode mode : modes) {
            for (long baseSeed : baseSeeds) {
                long seed = baseSeed + 1_000L * mode.ordinal();
                int handsPerMatch = GameRules.handSizes(mode, 4).size();
                Map<String, OfflineMockMatchTest.MockMatch> matches = new LinkedHashMap<>();
                List<String> referenceDeals = null;
                List<FocalResult> focalResults = new ArrayList<>();
                int compared = 0;

                for (int seat = 0; seat < 4; seat++) {
                    List<AiDifficulty> roster = new ArrayList<>(List.of(
                            AiDifficulty.NORMAL, AiDifficulty.NORMAL,
                            AiDifficulty.NORMAL, AiDifficulty.NORMAL));
                    roster.set(seat, AiDifficulty.HARD);

                    for (Arm arm : ARMS) {
                        OfflineMockMatchTest.MatchRun run =
                                OfflineMockMatchTest.playMatch(seed, roster, arm.profile(), mode);
                        OfflineMockMatchTest.MockMatch match = run.match();
                        String key = key(seat, arm.name());

                        atf.gradle.audit.GradleInputs.fixture("One focal Hard player; fixed deal sequence",
                                "mode", mode, "seed", seed, "arm", arm.name(), "seat", seat,
                                "focal points", seatMetrics(match, seat).points(),
                                "hands completed", match.handsPlayed(), "private deal exported", false);
                        assertTrue("Every focal-player run reaches the final score", match.complete());
                        assertEquals("Run uses the complete mode schedule", handsPerMatch, match.handsPlayed());
                        assertEquals("Every scheduled deal has an opaque commitment", handsPerMatch,
                                run.privateDealCommitments().size());
                        if (referenceDeals == null) {
                            referenceDeals = run.privateDealCommitments();
                        } else {
                            assertEquals("All A/B/C arms and focal seats use the same dealt cards and order",
                                    referenceDeals, run.privateDealCommitments());
                            compared += run.privateDealCommitments().size();
                        }
                        matches.put(key, match);
                    }
                }

                for (int seat = 0; seat < 4; seat++) {
                    OfflineMockMatchTest.MockMatch armA = matches.get(key(seat, "A_CURRENT"));
                    OfflineMockMatchTest.MockMatch armC = matches.get(key(seat, "C_PRUDENT_BID_08"));
                    if (!mode.isRomanian()) {
                        assertEquals("Romanian cautious-bid profile must stay neutral outside Romanian variants",
                                0, bidChanges(armA, armC, seat));
                        assertEquals("Romanian cautious-bid profile must not change non-Romanian card play",
                                0, cardChanges(armA, armC, seat));
                    }
                    for (Arm arm : ARMS) {
                        OfflineMockMatchTest.MockMatch match = matches.get(key(seat, arm.name()));
                        SeatMetrics metrics = seatMetrics(match, seat);
                        int bidChanges = arm.profile() == HardStrategyProfile.CURRENT
                                ? 0 : bidChanges(armA, match, seat);
                        int cardChanges = arm.profile() == HardStrategyProfile.CURRENT
                                ? 0 : cardChanges(armA, match, seat);
                        focalResults.add(new FocalResult(arm.name(), arm.profile().name(), seat,
                                metrics.points(), metrics.finalScore(), metrics.exact(), metrics.over(),
                                metrics.under(), metrics.topTwo(), bidChanges, cardChanges));
                    }
                }

                assertEquals("There are four seats times three profiles", 12, focalResults.size());
                assertEquals("All 11 repeated runs compare every scheduled deal", handsPerMatch * 11, compared);
                atf.gradle.audit.GradleInputs.fixture("Mode-level A/B/C comparison",
                        "mode", mode, "seed", seed, "hand schedule", GameRules.handSizes(mode, 4),
                        "matched opaque deal digest", digest(referenceDeals),
                        "profile arms", ARMS.stream().map(Arm::name).toList(),
                        "one Hard seat at a time", true);
                ModeResult modeResult = new ModeResult(mode.name(), seed, handsPerMatch,
                        focalResults.size(), compared, digest(referenceDeals), true, false,
                        List.copyOf(focalResults));
                results.add(modeResult);
                writeModeEvidenceIfRequested(mode, matches, modeResult);
            }
        }

        AbcMatrix matrix = new AbcMatrix(1,
                "A=current Hard; B=score-first; C=0.08 prudent-bid; one Hard player versus three Normal players, rotated over all seats",
                ARMS.stream().map(Arm::name).toList(), modes.stream().map(Enum::name).toList(), baseSeeds,
                List.copyOf(results), false);
        writeMatrixIfRequested(matrix);
    }

    private static List<GameMode> readModes() {
        String configured = System.getProperty("whist.offlineAbc.modes", DEFAULT_MODES);
        return java.util.Arrays.stream(configured.split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(GameMode::valueOf)
                .peek(mode -> {
                    if (mode == GameMode.ROMANIAN_WHIST) {
                        throw new IllegalArgumentException("This matrix deliberately excludes 1-8-1");
                    }
                })
                .toList();
    }

    private static List<Long> readSeeds() {
        String fallback = Long.toString(Long.getLong("whist.offlineAbc.seed", 20262121L));
        String configured = System.getProperty("whist.offlineAbc.seeds", "").strip();
        if (configured.isEmpty()) configured = fallback;
        return java.util.Arrays.stream(configured.split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(Long::parseLong)
                .distinct()
                .toList();
    }

    private static SeatMetrics seatMetrics(OfflineMockMatchTest.MockMatch match, int seat) {
        int points = 0, exact = 0, over = 0, under = 0;
        for (OfflineMockMatchTest.HandRecord hand : match.hands()) {
            OfflineMockMatchTest.SeatResult result = hand.results().stream()
                    .filter(row -> row.seat() == seat).findFirst().orElseThrow();
            points += result.points();
            if (result.bid() == result.tricksWon()) exact++;
            else if (result.tricksWon() > result.bid()) over++;
            else under++;
        }
        int finalScore = match.finalScores().get(seat);
        long playersAhead = match.finalScores().stream().filter(score -> score > finalScore).count();
        return new SeatMetrics(points, finalScore, exact, over, under, playersAhead < 2);
    }

    private static int bidChanges(OfflineMockMatchTest.MockMatch baseline,
            OfflineMockMatchTest.MockMatch candidate, int seat) {
        int changes = 0;
        for (int hand = 0; hand < baseline.hands().size(); hand++) {
            if (bid(baseline.hands().get(hand), seat).bid()
                    != bid(candidate.hands().get(hand), seat).bid()) changes++;
        }
        return changes;
    }

    private static int cardChanges(OfflineMockMatchTest.MockMatch baseline,
            OfflineMockMatchTest.MockMatch candidate, int seat) {
        int changes = 0;
        for (int hand = 0; hand < baseline.hands().size(); hand++) {
            List<String> expected = cards(baseline.hands().get(hand), seat);
            List<String> actual = cards(candidate.hands().get(hand), seat);
            int common = Math.min(expected.size(), actual.size());
            for (int action = 0; action < common; action++) {
                if (!expected.get(action).equals(actual.get(action))) changes++;
            }
            changes += Math.abs(expected.size() - actual.size());
        }
        return changes;
    }

    private static OfflineMockMatchTest.BidEvent bid(OfflineMockMatchTest.HandRecord hand, int seat) {
        return hand.bids().stream().filter(event -> event.seat() == seat).findFirst().orElseThrow();
    }

    private static List<String> cards(OfflineMockMatchTest.HandRecord hand, int seat) {
        return hand.plays().stream().filter(event -> event.seat() == seat)
                .map(OfflineMockMatchTest.CardEvent::card).toList();
    }

    private static String key(int seat, String arm) { return "seat" + seat + "_" + arm; }

    private static String digest(List<String> commitments) {
        try {
            byte[] canonical = String.join("\n", commitments).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static Path externalOutputRoot() {
        String configured = System.getProperty("whist.offlineAbc.output", "").strip();
        if (configured.isEmpty()) return null;
        Path output = Path.of(configured).toAbsolutePath().normalize();
        Path module = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path checkout = module.getParent().getParent();
        if (!output.isAbsolute() || output.startsWith(checkout)) {
            throw new IllegalArgumentException("A/B/C evidence must be outside the ATF checkout");
        }
        return output;
    }

    private static void writeModeEvidenceIfRequested(GameMode mode,
            Map<String, OfflineMockMatchTest.MockMatch> matches, ModeResult result) throws Exception {
        Path root = externalOutputRoot();
        if (root == null) return;
        Path modeRoot = root.resolve(mode.name() + "-seed" + result.seed());
        Files.createDirectories(modeRoot);
        for (Map.Entry<String, OfflineMockMatchTest.MockMatch> entry : matches.entrySet()) {
            Path trace = modeRoot.resolve(entry.getKey() + ".json");
            try (var writer = Files.newBufferedWriter(trace, StandardOpenOption.CREATE_NEW)) {
                JSON.writerWithDefaultPrettyPrinter().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                        .writeValue(writer, entry.getValue());
                writer.write('\n');
            }
        }
        Path summary = modeRoot.resolve("summary.json");
        try (var writer = Files.newBufferedWriter(summary, StandardOpenOption.CREATE_NEW)) {
            JSON.writerWithDefaultPrettyPrinter().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .writeValue(writer, result);
            writer.write('\n');
        }
    }

    private static void writeMatrixIfRequested(AbcMatrix matrix) throws Exception {
        Path root = externalOutputRoot();
        if (root == null) return;
        Files.createDirectories(root);
        Path output = root.resolve("abc-mode-matrix.json");
        try (var writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE_NEW)) {
            JSON.writerWithDefaultPrettyPrinter().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .writeValue(writer, matrix);
            writer.write('\n');
        }
    }
}
