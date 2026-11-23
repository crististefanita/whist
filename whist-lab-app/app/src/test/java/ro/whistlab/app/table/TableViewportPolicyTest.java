package ro.whistlab.app.table;

import atf.gradle.audit.input.CapturedValue;

import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public class TableViewportPolicyTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void tallPortraitsReclaimDecorationWhenLargerTextExhaustsTheRoomyBudget() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 818, "declared constant 2", 1f, "declared constant 3", 1.3f, "declared constant 4", 1.5f, "declared constant 5", "Only the physical short-window threshold changes card height", "declared constant 6", 70, "declared constant 7", 3, "declared constant 8", 6, "declared constant 9", 353, "declared constant 10", 42, "declared constant 11", 16, "declared constant 12", 20, "declared constant 13", 13, "declared constant 14", 90, "declared constant 15", 4, "declared constant 16", "Larger text retains the hand and all seat/action regions", "declared constant 17", true, "declared constant 18", 0, "declared constant 19", -1);
        for(float fontScale:new float[]{1.3f,1.5f}) {
            var viewport=new TableViewportPolicy(818,fontScale);
            for(int players=3;players<=6;players++) {
                var trick=TrickCardMetrics.forTable(players,353,fontScale,viewport);
                int reference=ro.whistlab.app.seat.PlayerSeatMetrics.referenceFrameHeightPx(
                        (int)Math.ceil(42*fontScale),(int)Math.ceil(16*fontScale),20);
                int seat=ro.whistlab.app.seat.PlayerSeatMetrics.arrange(reference,(int)Math.ceil(13*fontScale),3).heightPx();
                int available=818-(int)Math.ceil(90*fontScale)-HandLayoutMetrics.reservedHandHeightDp()-4;
                int minimum=TableGeometry.minimumHeight(players,seat,trick.centerHeightDp(),viewport.regionGapDp());
                assertTrue("Larger text retains the hand and all seat/action regions",minimum<=available);
                var plan=TableGeometry.arrangeDisplay(353,available,players,
                        TableLayoutMetrics.seatWidthDp(353,true),seat,trick.safeCenterWidthDp(),trick.centerHeightDp(),
                        viewport.regionGapDp(),true,TableGeometry.CenterAlignment.BALANCED);
                assertEquals("plan.seats().getFirst().bottom() must equal available", available,plan.seats().getFirst().bottom());
            }
        }
    }

    @Test public void tallerFramesAndLowerCompositionKeepEveryRegionInsideTheBoard() {
        var checks = new ExhaustiveMatrixChecks();
        atf.gradle.audit.GradleInputs.fixture("Table containment matrix", "widthsDp", new int[]{280, 313, 353, 560, 760},
                "players", "3–6", "naturalHeights", new int[]{48, 62, 82, 110, 156},
                "extraHeights", new int[]{0, 80, 240}, "expectedPairs", 6250);
        for (int width : new int[]{280, 313, 353, 560, 760}) for (int players = 3; players <= 6; players++) {
            for (int naturalHeight : new int[]{48, 62, 82, 110, 156}) {
                var seat = ro.whistlab.app.seat.PlayerSeatMetrics.arrange(naturalHeight, 12, 3);
                checks.at("width=" + width + ", players=" + players + ", naturalHeight=" + naturalHeight);
                checks.equal("Frame height keeps the 25 percent reserve", (int) Math.ceil(naturalHeight * 1.25), seat.frameHeightPx());
                var trick = TrickCardMetrics.forTable(players, width);
                int minimum = TableGeometry.minimumHeight(players, seat.heightPx(), trick.centerHeightDp(), 8);
                for (int extra : new int[]{0, 80, 240}) {
                    int height = minimum + extra;
                    var plan = TableGeometry.arrangeDisplay(width, height, players,
                            TableLayoutMetrics.seatWidthDp(width), seat.heightPx(),
                            trick.safeCenterWidthDp(), trick.centerHeightDp(), 8, true,
                            TableGeometry.CenterAlignment.BALANCED);
                    String location = "width=" + width + ", players=" + players + ", naturalHeight=" + naturalHeight
                            + ", extra=" + extra + ", plan=" + plan;
                    checks.at(location);
                    int inset = Math.min(Math.round(height * 0.05f), extra);
                    checks.equal("The top inset covers the required reserve", true, TableLayoutMetrics.topInsetPx(height) >= inset);
                    checks.equal("The human frame ends above the hand region", height, plan.seats().getFirst().bottom());
                    var boxes = new java.util.ArrayList<>(plan.seats());
                    boxes.add(plan.center());
                    for (int first = 0; first < boxes.size(); first++) {
                        var box = boxes.get(first);
                        checks.at(location + ", first=" + first);
                        checks.equal("Every occupied region stays inside the board", true,
                                box.top() >= inset && box.bottom() <= height);
                        for (int second = first + 1; second < boxes.size(); second++) {
                            checks.at(location + ", first=" + first + ", second=" + second);
                            checks.equal("Distinct occupied regions never intersect", false, box.overlaps(boxes.get(second)));
                        }
                    }
                }
            }
        }
        assertEquals("The independently enumerated containment population is complete", 6250L, checks.checkedPairs());
        checks.publish();
    }

    @Test public void shortWindowsReclaimDecorationAndReduceOnlyThePlayedCardFaces() {
        var compact = new TableViewportPolicy(497);
        atf.gradle.audit.GradleInputs.fixture("Whist adapter with compact shared policy", "heightDp", 497,
                "handHeightDp", HandLayoutMetrics.cardHeightDp(), "bidTargetDp", BidGridMetrics.CHOICE_TARGET_DP);
        assertEquals("The private hand keeps its original card height", 88, HandLayoutMetrics.cardHeightDp());
        assertEquals("Bidding keeps its original touch target", 48, BidGridMetrics.CHOICE_TARGET_DP);
        assertEquals("Whist consumes the neutral compact played-card height", 64,
                TrickCardMetrics.forTable(4, 353, 1f, compact).cardHeightDp());
    }

    @Test public void compactHandTrickAndEverySeatFitOneViewportWithoutIntersections() {
        var matrixChecks = new ExhaustiveMatrixChecks();
        atf.gradle.audit.GradleInputs.fixture("Exhaustive matrix scope", "scenario", "compactHandTrickAndEverySeatFitOneViewportWithoutIntersections", "expected pairs", 606);
        var viewport = new TableViewportPolicy(497);
        int availableBoard = 497 - 4 - 60 - 2 * viewport.outerVerticalPaddingDp()
                - HandLayoutMetrics.reservedHandHeightDp();
        for (int width : new int[]{280, 313, 353}) for (int players = 3; players <= 6; players++) {
            var trick = TrickCardMetrics.forTable(players, width, 1f, viewport);
            int seatWidth = TableLayoutMetrics.seatWidthDp(width, true);
            int reference = ro.whistlab.app.seat.PlayerSeatMetrics.referenceFrameHeightPx(43, 15, 20);
            int seatHeight = ro.whistlab.app.seat.PlayerSeatMetrics.arrange(reference, 12, 3).heightPx();
            int minimum = TableGeometry.minimumHeight(
                    players, seatHeight, trick.centerHeightDp(), viewport.regionGapDp());

            matrixChecks.at(CapturedValue.text(new Object[]{"Fixture before first assertion", "availableBoard", availableBoard, "minimum", minimum, "players", players, "reference", reference, "seatHeight", seatHeight, "seatWidth", seatWidth, "trick", trick, "viewport", viewport, "width", width}));
            matrixChecks.equal("The whole table must fit a 497 dp viewport: " + players + " players", true, minimum <= availableBoard);
            var plan = TableGeometry.arrangeDisplay(width, availableBoard, players, seatWidth, seatHeight,
                    trick.safeCenterWidthDp(), trick.centerHeightDp(), viewport.regionGapDp(), true,
                    TableGeometry.CenterAlignment.BALANCED);
            var occupied = new java.util.ArrayList<>(plan.seats());
            for (var slot : trick.slots()) {
                int top = plan.center().top() + slot.row() * trick.rowStrideDp();
                occupied.add(new TableGeometry.Rect(slot.leftDp(), top,
                        slot.leftDp() + trick.cardWidthDp(), top + trick.cellHeightDp()));
            }
            for (int first = 0; first < occupied.size(); first++) {
                var box = occupied.get(first);
                matrixChecks.equal("box.top() >= 0 && box.bottom() <= availableBoard must hold", true, box.top() >= 0 && box.bottom() <= availableBoard);
                for (int second = first + 1; second < occupied.size(); second++) {
                    matrixChecks.equal("Compact regions intersect at " + width + " dp, " + players + " players", false, box.overlaps(occupied.get(second)));
                }
            }
            matrixChecks.equal("trick.cardHeightDp() must equal 64", 64, trick.cardHeightDp());
            matrixChecks.equal("Names remain in the seat frames and accessible card descriptions", 0, trick.playerLabelHeightDp());
        }
        assertEquals("The original matrix population remains complete", 606L, matrixChecks.checkedPairs());
        matrixChecks.publish();
    }

    @Test public void bidZoneUsesTheLowerPartOfTheAvailableSafeBand() {
        for (int width : new int[]{280, 320, 353, 560}) {
            int seatWidth = TableLayoutMetrics.seatWidthDp(width);
            int columns = BidGridMetrics.columnCount(9, width);
            int centerWidth = Math.max(132, Math.min(9, columns) * 52);
            int centerHeight = BidGridMetrics.rowCount(9, width) * BidGridMetrics.CHOICE_ROW_DP
                    + BidGridMetrics.SUBMIT_TARGET_DP;
            int gap = 3;
            int seatHeight = 62;
            int minimum = TableGeometry.minimumHeight(4, seatHeight, centerHeight, gap);
            for (int extra : new int[]{0, 40, 120}) {
                var plan = TableGeometry.arrange(width, minimum + extra, 4, seatWidth,
                        seatHeight, centerWidth, centerHeight, gap, false,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN);
                var human = plan.seats().getFirst();
                int humanGap = human.top() - plan.center().bottom();
                int upperGap = plan.seats().stream().skip(1)
                        .filter(seat -> seat.bottom() <= plan.center().top())
                        .mapToInt(seat -> plan.center().top() - seat.bottom())
                        .min().orElseThrow();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "centerHeight", centerHeight, "centerWidth", centerWidth, "columns", columns, "extra", extra, "gap", gap, "human", human, "humanGap", humanGap, "minimum", minimum, "plan", plan, "seatHeight", seatHeight, "seatWidth", seatWidth, "upperGap", upperGap, "width", width);
                assertTrue("Bids must sit closer to the human seat at width=" + width
                                + ", extra=" + extra + ": upper=" + upperGap + ", human=" + humanGap,
                        humanGap < upperGap);
                assertTrue("The measured bid area must remain clear of every seat",
                        plan.seats().stream().noneMatch(plan.center()::overlaps));
            }
        }
    }

    @Test public void pixelRoundingKeepsTheFourSeatOpponentArcClearOfThePlayingBand() {
        for (int width : new int[]{280, 313, 353, 560, 760}) for (float density : new float[]{1, 1.5f, 2, 2.25f, 2.75f, 3.5f}) {
            int w = Math.round(width * density), seat = Math.round(TableLayoutMetrics.seatWidthDp(width) * density);
            var plan = TableGeometry.arrange(w, Math.round(400 * density), 4, seat, Math.round(70 * density),
                    Math.round(TableLayoutMetrics.centerPlayWidthDp(width) * density), Math.round(144 * density), Math.round(8 * density));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "plan", plan, "seat", seat, "w", w, "width", width);
            assertTrue("plan.seats().get(1).bottom() <= plan.center().top() must hold", plan.seats().get(1).bottom() <= plan.center().top());
            assertTrue("plan.seats().get(3).bottom() <= plan.center().top() must hold", plan.seats().get(3).bottom() <= plan.center().top());
            assertEquals("plan.seats().get(3).top() must equal plan.seats().get(1).top()", plan.seats().get(1).top(), plan.seats().get(3).top());
            assertEquals("plan.seats().getFirst().bottom() must equal Math.round(400 * density)", Math.round(400*density),plan.seats().getFirst().bottom());
        }
    }
}
