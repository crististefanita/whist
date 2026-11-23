package ro.cardtable.ui.boundary;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import ro.cardtable.testing.SurfaceContinuity;
import ro.cardtable.testing.SurfaceObservation;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class PortableSurfaceComponentTest extends GradleAuditedTest {
    @Test public void nativeAndDomMeasurementsUseOneScaleIndependentOracle() {
        for (double scale : new double[]{0.75, 1, 1.25, 1.3, 2, 3}) {
            var logical = new SurfaceObservation(Map.of("seat:0", new SurfaceObservation.Bounds(10, 20, 80, 60)));
            var measured = new SurfaceObservation(Map.of("seat:0", SurfaceObservation.Bounds.measured(
                    7 + 10 * scale, 14 + 20 * scale, 80 * scale, 60 * scale, 7, 14, scale)));
            GradleInputs.fixture("Independent adapter coordinates", "scale", scale, "expected", logical, "observed", measured);
            SurfaceContinuity.verify(logical, measured, 0.00001);
            assertEquals("Normalized native/DOM region retains its width", 80d, measured.regions().get("seat:0").width(), 0.00001);
        }
    }

    @Test public void rejectsDriftMissingRegionsAndReorderedIdentities() {
        var baseline = new LinkedHashMap<String, SurfaceObservation.Bounds>();
        baseline.put("seat:0", new SurfaceObservation.Bounds(10, 20, 80, 60));
        baseline.put("seat:1", new SurfaceObservation.Bounds(100, 20, 80, 60));
        var before = new SurfaceObservation(baseline);
        var shifted = new LinkedHashMap<>(baseline);
        shifted.put("seat:1", new SurfaceObservation.Bounds(101, 20, 80, 60));
        GradleInputs.fixture("Unchanged identities and a one-pixel mutation", "before", before, "after", shifted);
        var drift = assertThrows("A one-pixel shift cannot pass continuity", AssertionError.class,
                () -> SurfaceContinuity.verify(before, new SurfaceObservation(shifted), 0.02));
        assertTrue("The failure identifies the changed region", drift.getMessage().contains("REGION_DRIFT: seat:1"));
        var missing = assertThrows("Missing actors cannot silently pass", AssertionError.class,
                () -> SurfaceContinuity.verify(before, new SurfaceObservation(Map.of("seat:0", baseline.get("seat:0"))), 0.02));
        assertEquals("Missing identity has a specific cause", "REGION_IDENTITY_OR_ORDER", missing.getMessage());
        var reversed = new LinkedHashMap<String, SurfaceObservation.Bounds>();
        reversed.put("seat:1", baseline.get("seat:1")); reversed.put("seat:0", baseline.get("seat:0"));
        assertThrows("Changed seat order cannot pass as equal geometry", AssertionError.class,
                () -> SurfaceContinuity.verify(before, new SurfaceObservation(reversed), 0.02));
    }
}
