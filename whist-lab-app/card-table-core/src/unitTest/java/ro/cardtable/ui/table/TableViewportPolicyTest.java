package ro.cardtable.ui.table;

import org.junit.Test;
import static org.junit.Assert.*;

/** Both adapters consume this single neutral decoration and card-height policy. */
public class TableViewportPolicyTest {
    @Test public void shortWindowsReclaimDecorationAndReduceOnlyThePlayedCardFaces() {
        var compact = new TableViewportPolicy(497);

        assertTrue("compact.isCompact() must hold", compact.isCompact());
        assertEquals("compact.regionGapDp() must equal 1", 1, compact.regionGapDp());
        assertEquals("compact.minimumBoardHeightDp() must equal 0", 0, compact.minimumBoardHeightDp());
        assertFalse("compact.showPlayHint() must remain false", compact.showPlayHint());
        assertEquals("compact.handClearanceDp(false) must equal 28", 28, compact.handClearanceDp(false));
        assertEquals("compact.handClearanceDp(true) must equal 12", 12, compact.handClearanceDp(true));
        assertEquals("compact.seatVerticalPaddingDp() must equal 1", 1, compact.seatVerticalPaddingDp());
        assertEquals("compact.outerVerticalPaddingDp() must equal 0", 0, compact.outerVerticalPaddingDp());
        assertEquals("compact.playedCardFaceHeightDp() must equal 64", 64, compact.playedCardFaceHeightDp());
        assertEquals("compact.playedCardItemVerticalPaddingDp() must equal 2", 2, compact.playedCardItemVerticalPaddingDp());
        var intermediate = new TableViewportPolicy(626);
        assertTrue("intermediate.isCompact() must hold", intermediate.isCompact());
        assertEquals("intermediate.playedCardFaceHeightDp() must equal 70", 70, intermediate.playedCardFaceHeightDp());
        var roomy = new TableViewportPolicy(818);
        assertFalse("roomy.isCompact() must remain false", roomy.isCompact());
        assertTrue("roomy.showPlayHint() must hold", roomy.showPlayHint());
        assertEquals("roomy.seatVerticalPaddingDp() must equal 4", 4, roomy.seatVerticalPaddingDp());
        assertEquals("roomy.outerVerticalPaddingDp() must equal 4", 4, roomy.outerVerticalPaddingDp());
        assertEquals("roomy.playedCardFaceHeightDp() must equal 70", 70, roomy.playedCardFaceHeightDp());
        assertEquals("roomy.playedCardItemVerticalPaddingDp() must equal PlayedCardMetrics.ITEM_VERTICAL_PADDING_DP", PlayedCardMetrics.ITEM_VERTICAL_PADDING_DP, roomy.playedCardItemVerticalPaddingDp());
    }


    @Test public void enlargedTextReclaimsDecorationWithoutShrinkingNormalCardFaces() {
        assertFalse(new TableViewportPolicy(818, 1f).isCompact());
        for (float scale : new float[]{1.3f, 1.5f}) {
            var policy = new TableViewportPolicy(818, scale);
            assertTrue(policy.isCompact());
            assertFalse(policy.showPlayHint());
            assertEquals(70, policy.playedCardFaceHeightDp());
        }
    }

    @Test public void physicalHeightAndTextThresholdsRemainIndependent() {
        assertEquals(64, new TableViewportPolicy(599).playedCardFaceHeightDp());
        assertEquals(70, new TableViewportPolicy(600).playedCardFaceHeightDp());
        assertTrue(new TableViewportPolicy(759).isCompact());
        assertFalse(new TableViewportPolicy(760).isCompact());
        assertTrue(new TableViewportPolicy(1519, 2f).isCompact());
        assertFalse(new TableViewportPolicy(1520, 2f).isCompact());
        assertFalse(new TableViewportPolicy(760, .5f).isCompact());
    }

    @Test public void invalidFontScalesRemainRejected() {
        for (float scale : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> new TableViewportPolicy(818, scale));
    }
}
