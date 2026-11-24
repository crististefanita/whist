package ro.cardtable.ui.observation;

import java.util.List;
import java.util.Objects;

/** Measured renderer output, never an expected layout plan. Units are platform pixels. */
public record RenderedSurface(String owner, long revision, double scale, List<Region> regions) {
    public record Box(double left, double top, double right, double bottom) {
        public Box {
            if (!Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(right)
                    || !Double.isFinite(bottom) || right <= left || bottom <= top)
                throw new IllegalArgumentException("INVALID_RENDERED_BOX");
        }
        public boolean contains(Box other, double tolerance) {
            return other.left >= left - tolerance && other.top >= top - tolerance
                    && other.right <= right + tolerance && other.bottom <= bottom + tolerance;
        }
    }
    public record Region(String id, Box frame, Box visible, List<Box> textLines, boolean ellipsized) {
        public Region {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("MISSING_RENDERED_ID");
            Objects.requireNonNull(frame); Objects.requireNonNull(visible);
            textLines = List.copyOf(textLines);
        }
    }
    public RenderedSurface {
        if (owner == null || owner.isBlank() || revision < 0 || !Double.isFinite(scale) || scale <= 0)
            throw new IllegalArgumentException("INVALID_RENDERED_OWNER");
        regions = List.copyOf(regions);
        if (regions.isEmpty() || regions.stream().map(Region::id).distinct().count() != regions.size())
            throw new IllegalArgumentException("INVALID_RENDERED_POPULATION");
    }
}
