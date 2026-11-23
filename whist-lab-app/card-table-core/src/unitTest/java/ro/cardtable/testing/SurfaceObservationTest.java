package ro.cardtable.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SurfaceObservationTest {
    @Test public void normalizesPlatformScaleAndOriginWithoutChangingLayout() {
        var logical = new SurfaceObservation.Bounds(10, 20, 80, 60);
        assertEquals(logical, SurfaceObservation.Bounds.measured(37, 74, 240, 180, 7, 14, 3));
    }

    @Test public void observationOwnsItsSnapshot() {
        var mutable = new LinkedHashMap<>(Map.of("seat:0", new SurfaceObservation.Bounds(1, 2, 3, 4)));
        var observation = new SurfaceObservation(mutable);
        mutable.clear();
        assertEquals(1, observation.regions().size());
        assertThrows(UnsupportedOperationException.class, () -> observation.regions().clear());
    }

    @Test public void rejectsInvalidMeasurementsInsteadOfHidingMissingRegions() {
        assertThrows(IllegalArgumentException.class, () -> new SurfaceObservation(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SurfaceObservation.Bounds(Double.NaN, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> SurfaceObservation.Bounds.measured(0, 0, 1, 1, 0, 0, 0));
    }
}
