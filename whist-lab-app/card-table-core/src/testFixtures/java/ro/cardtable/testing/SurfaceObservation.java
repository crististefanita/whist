package ro.cardtable.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Platform measurements expressed in logical, unscaled coordinates. No game or ATF dependency. */
public record SurfaceObservation(Map<String, Bounds> regions) {
    public SurfaceObservation {
        if (regions == null || regions.isEmpty()) throw new IllegalArgumentException("EMPTY_OBSERVATION");
        regions = Collections.unmodifiableMap(new LinkedHashMap<>(regions));
        regions.forEach((id, bounds) -> {
            if (id == null || id.isBlank() || bounds == null) throw new IllegalArgumentException("INVALID_REGION");
        });
    }

    public record Bounds(double left, double top, double width, double height) {
        public Bounds {
            if (!Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(width)
                    || !Double.isFinite(height) || width <= 0 || height <= 0)
                throw new IllegalArgumentException("INVALID_BOUNDS");
        }
        public static Bounds measured(double left, double top, double width, double height,
                                      double originX, double originY, double scale) {
            if (!Double.isFinite(scale) || scale <= 0) throw new IllegalArgumentException("INVALID_SCALE");
            return new Bounds((left - originX) / scale, (top - originY) / scale, width / scale, height / scale);
        }
    }
}
