package ro.cardtable.testing;

import java.util.ArrayList;

/** One strict oracle for measurements supplied by native, DOM or policy adapters. */
public final class SurfaceContinuity {
    private SurfaceContinuity() { }

    public static void verify(SurfaceObservation before, SurfaceObservation after, double tolerance) {
        if (!Double.isFinite(tolerance) || tolerance < 0) throw new IllegalArgumentException("INVALID_TOLERANCE");
        if (!new ArrayList<>(before.regions().keySet()).equals(new ArrayList<>(after.regions().keySet())))
            throw new AssertionError("REGION_IDENTITY_OR_ORDER");
        before.regions().forEach((id, expected) -> {
            var actual = after.regions().get(id);
            if (Math.abs(expected.left() - actual.left()) > tolerance
                    || Math.abs(expected.top() - actual.top()) > tolerance
                    || Math.abs(expected.width() - actual.width()) > tolerance
                    || Math.abs(expected.height() - actual.height()) > tolerance)
                throw new AssertionError("REGION_DRIFT: " + id + "; expected=" + expected + "; actual=" + actual);
        });
    }
}
