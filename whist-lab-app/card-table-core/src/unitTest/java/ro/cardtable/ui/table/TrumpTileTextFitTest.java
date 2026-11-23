package ro.cardtable.ui.table;

import org.junit.Test;
import ro.cardtable.ui.table.trump.TrumpTileMetrics;
import static org.junit.Assert.*;

/** The original density/font/glyph oracle, without a platform or report producer. */
public class TrumpTileTextFitTest {
    @Test public void narrowerTrumpTileFitsLongValuesWithoutEnlargingTheFrame() {

        assertEquals("TrumpTileMetrics.WIDTH_DP must equal 57", 57, TrumpTileMetrics.WIDTH_DP);
        for (float density : new float[]{1, 1.5f, 2.625f, 2.75f, 3.5f}) {
            int available = Math.round(57 * density) - 2 * Math.round(4 * density)
                    - 2 * Math.round(5 * density);
            for (float fontScale : new float[]{1, 1.3f, 1.5f, 2}) {
                float nominal = 20 * fontScale * density;
                for (float measuredGlyphWidth : new float[]{20, 35, 48}) {
                    float measured = measuredGlyphWidth * fontScale * density;
                    float fitted = TrumpTileMetrics.fitTextSizePx(nominal, measured, available);
                    assertTrue("fitted <= nominal must hold", fitted <= nominal);
                    assertTrue("measured * fitted / nominal <= available + 0.01F must hold", measured * fitted / nominal <= available + 0.01f);
                    assertEquals("Repeated layout must start from the nominal size", fitted,
                            TrumpTileMetrics.fitTextSizePx(nominal, measured, available), 0);
                }
            }
        }
    }

}
