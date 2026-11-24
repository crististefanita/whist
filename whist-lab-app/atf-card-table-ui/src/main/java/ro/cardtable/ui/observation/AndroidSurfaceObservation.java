package ro.cardtable.ui.observation;

import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Reads laid-out Views and Android Layout lines; expected geometry is deliberately unavailable. */
public final class AndroidSurfaceObservation {
    public record Binding(String id, View view) {
        public Binding {
            if (id == null || id.isBlank() || view == null) throw new IllegalArgumentException("INVALID_NATIVE_BINDING");
        }
    }
    private AndroidSurfaceObservation() { }

    public static RenderedSurface capture(View root, List<Binding> bindings,
            SurfaceEpoch epoch, SurfaceEpoch.Ticket ticket) {
        epoch.requireCurrent(ticket);
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper())
            throw new IllegalStateException("NATIVE_OBSERVATION_WRONG_THREAD");
        if (root == null || !root.isLaidOut() || root.isLayoutRequested())
            throw new IllegalStateException("NATIVE_LAYOUT_NOT_READY");
        int[] origin = new int[2]; root.getLocationOnScreen(origin);
        var regions = new ArrayList<RenderedSurface.Region>();
        for (var binding : List.copyOf(bindings)) regions.add(measure(root, binding, origin));
        epoch.requireCurrent(ticket);
        return new RenderedSurface(ticket.owner(), ticket.revision(),
                root.getResources().getDisplayMetrics().density, regions);
    }
    private static RenderedSurface.Region measure(View root, Binding binding, int[] origin) {
        var view = binding.view();
        if (view.getRootView() != root.getRootView()) throw new IllegalStateException("FOREIGN_NATIVE_VIEW: " + binding.id());
        var visible = new Rect();
        if (!view.isLaidOut() || view.isLayoutRequested() || !view.isShown() || !view.getGlobalVisibleRect(visible))
            throw new IllegalStateException("NATIVE_VIEW_NOT_VISIBLE: " + binding.id());
        int[] location = new int[2]; view.getLocationOnScreen(location);
        double x = location[0] - origin[0], y = location[1] - origin[1];
        var frame = new RenderedSurface.Box(x, y, x + view.getWidth(), y + view.getHeight());
        var viewport = new RenderedSurface.Box(visible.left-origin[0], visible.top-origin[1],
                visible.right-origin[0], visible.bottom-origin[1]);
        var lines = new ArrayList<RenderedSurface.Box>(); boolean ellipsized = false;
        if (view instanceof TextView text && text.length() > 0) {
            var layout = text.getLayout();
            if (layout == null || layout.getLineCount() == 0) throw new IllegalStateException("NATIVE_TEXT_NOT_LAID_OUT: " + binding.id());
            double textX = x + text.getCompoundPaddingLeft() - text.getScrollX();
            double textY = y + text.getBaseline() - layout.getLineBaseline(0) - text.getScrollY();
            for (int line = 0; line < layout.getLineCount(); line++) {
                ellipsized |= layout.getEllipsisCount(line) > 0;
                double left = textX + layout.getLineLeft(line), right = textX + layout.getLineRight(line);
                if (right > left) lines.add(new RenderedSurface.Box(left, textY + layout.getLineTop(line),
                        right, textY + layout.getLineBottom(line)));
            }
        }
        return new RenderedSurface.Region(binding.id(), frame, viewport, lines, ellipsized);
    }
}
