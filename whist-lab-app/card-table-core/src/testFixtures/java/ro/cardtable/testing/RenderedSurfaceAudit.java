package ro.cardtable.testing;

import ro.cardtable.ui.observation.RenderedSurface;

/** Independent oracle for native and DOM measurements; does not call a layout policy. */
public final class RenderedSurfaceAudit {
    private RenderedSurfaceAudit() { }
    public static void visible(RenderedSurface surface, double tolerancePixels) {
        tolerance(tolerancePixels);
        for (var region : surface.regions()) {
            if (!region.visible().contains(region.frame(), tolerancePixels))
                throw new AssertionError("CLIPPED_FRAME: " + region.id());
            if (region.ellipsized()) throw new AssertionError("ELLIPSIZED_TEXT: " + region.id());
            for (var line : region.textLines())
                if (!region.visible().contains(line, tolerancePixels) || !region.frame().contains(line, tolerancePixels))
                    throw new AssertionError("CLIPPED_TEXT: " + region.id() + "; line=" + line);
        }
    }
    public static void stable(RenderedSurface before, RenderedSurface after, double toleranceLogical) {
        tolerance(toleranceLogical);
        visible(before, toleranceLogical * before.scale());
        visible(after, toleranceLogical * after.scale());
        if (!before.owner().equals(after.owner()) || after.revision() < before.revision())
            throw new AssertionError("FOREIGN_OR_STALE_SURFACE");
        if (!before.regions().stream().map(RenderedSurface.Region::id).collect(java.util.stream.Collectors.toList())
                .equals(after.regions().stream().map(RenderedSurface.Region::id).collect(java.util.stream.Collectors.toList())))
            throw new AssertionError("RENDERED_POPULATION_CHANGED");
        var left = new java.util.LinkedHashMap<String, SurfaceObservation.Bounds>();
        var right = new java.util.LinkedHashMap<String, SurfaceObservation.Bounds>();
        for (int index = 0; index < before.regions().size(); index++) {
            var first = before.regions().get(index); var second = after.regions().get(index);
            left.put(first.id(), logical(first.frame(), before.scale()));
            right.put(second.id(), logical(second.frame(), after.scale()));
        }
        SurfaceContinuity.verify(new SurfaceObservation(left), new SurfaceObservation(right), toleranceLogical);
    }
    private static SurfaceObservation.Bounds logical(RenderedSurface.Box box, double scale) {
        return new SurfaceObservation.Bounds(box.left()/scale, box.top()/scale,
                (box.right()-box.left())/scale, (box.bottom()-box.top())/scale);
    }
    private static void tolerance(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("INVALID_RENDER_TOLERANCE");
    }
}
