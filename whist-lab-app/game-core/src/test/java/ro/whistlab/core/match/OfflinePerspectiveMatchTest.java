package ro.whistlab.core.match;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;

/** Replays one fixed deal sequence in a selected ruleset with each seat acting as the sole Hard player. */
public final class OfflinePerspectiveMatchTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    record Perspective(String name, String roster, int hardPoints, int normalPoints,
            int hardExactContracts, int normalExactContracts, OfflineMockMatchTest.MockMatch match) { }
    record PerspectiveMatrix(int schemaVersion, String mode, long seed, int runs,
            int handsPerRun, int opaqueDealCommitmentsCompared, boolean sameDistributionAndOrder,
            String opaqueDealCommitmentsSha256, List<Perspective> perspectives,
            boolean privateDealsIncluded) { }

    @Test public void replaysTheSameDealsAndTurnOrderAcrossHardSeatPerspectives() throws Exception {
        long seed = Long.getLong("whist.offlinePerspective.seed", 20261104L);
        GameMode mode = GameMode.valueOf(System.getProperty(
                "whist.offlinePerspective.mode", GameMode.ROMANIAN_WHIST.name()));
        int handsPerRun = GameRules.handSizes(mode, 4).size();
        List<List<AiDifficulty>> rosters = new ArrayList<>();
        rosters.add(List.of(AiDifficulty.NORMAL, AiDifficulty.NORMAL, AiDifficulty.NORMAL, AiDifficulty.NORMAL));
        for (int hardSeat = 0; hardSeat < 4; hardSeat++) {
            List<AiDifficulty> roster = new ArrayList<>(List.of(
                    AiDifficulty.NORMAL, AiDifficulty.NORMAL, AiDifficulty.NORMAL, AiDifficulty.NORMAL));
            roster.set(hardSeat, AiDifficulty.HARD);
            rosters.add(List.copyOf(roster));
        }
        rosters.add(List.of(AiDifficulty.HARD, AiDifficulty.HARD, AiDifficulty.HARD, AiDifficulty.HARD));

        List<Perspective> results = new ArrayList<>();
        List<String> referenceCommitments = null;
        boolean sameDeals = true;
        int compared = 0;
        for (int index = 0; index < rosters.size(); index++) {
            List<AiDifficulty> roster = rosters.get(index);
            OfflineMockMatchTest.MatchRun run = OfflineMockMatchTest.playMatch(seed, roster, mode);
            OfflineMockMatchTest.MockMatch match = run.match();

            atf.gradle.audit.GradleInputs.fixture("Fixed deal comparison", "seed", seed, "perspective", index,
                    "roster", roster, "hands completed", match.handsPlayed(), "deals compared so far", compared);
            assertTrue("match.complete() must hold", match.complete());
            assertEquals("each perspective commits every scheduled deal", handsPerRun,
                    run.privateDealCommitments().size());
            if (referenceCommitments == null) {
                referenceCommitments = run.privateDealCommitments();
            } else {
                assertEquals("run.privateDealCommitments() must equal referenceCommitments", referenceCommitments, run.privateDealCommitments());
                compared += run.privateDealCommitments().size();
            }
            sameDeals &= referenceCommitments.equals(run.privateDealCommitments());

            int hardPoints = 0;
            int normalPoints = 0;
            int hardExact = 0;
            int normalExact = 0;
            for (OfflineMockMatchTest.HandRecord hand : match.hands()) {
                for (OfflineMockMatchTest.SeatResult seat : hand.results()) {
                    if ("HARD".equals(seat.policy())) {
                        hardPoints += seat.points();
                        if (seat.bid() == seat.tricksWon()) hardExact++;
                    } else {
                        normalPoints += seat.points();
                        if (seat.bid() == seat.tricksWon()) normalExact++;
                    }
                }
            }
            String name = index == 0 ? "normal-all"
                    : index == rosters.size() - 1 ? "hard-all" : "hard-seat-" + (index - 1);
            results.add(new Perspective(name, match.policiesBySeat(), hardPoints, normalPoints,
                    hardExact, normalExact, match));
        }
        assertTrue("sameDeals must hold", sameDeals);
        assertEquals("results.size() must equal 6", 6, results.size());
        String dealDigest = digestCommitments(referenceCommitments);
        assertTrue("the opaque deal-order digest is a SHA-256 value", dealDigest.matches("[0-9a-f]{64}"));
        atf.gradle.audit.GradleInputs.fixture("Only an aggregate opaque commitment escapes the fixed-deal matrix",
                "ruleset", mode, "scheduled hands", handsPerRun,
                "commitments compared", compared, "opaque digest", dealDigest,
                "private deal data exported", false);
        PerspectiveMatrix matrix = new PerspectiveMatrix(1, mode.name(), seed,
                results.size(), handsPerRun, compared, sameDeals, dealDigest,
                List.copyOf(results), false);
        writeIfRequested(matrix);
    }

    private static String digestCommitments(List<String> commitments) {
        try {
            byte[] canonical = String.join("\n", commitments).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static void writeIfRequested(PerspectiveMatrix matrix) throws Exception {
        String configured = System.getProperty("whist.offlinePerspective.output", "").strip();
        if (configured.isEmpty()) return;
        Path output = Path.of(configured);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(matrix) + "\n",
                StandardOpenOption.CREATE_NEW);
    }
}
