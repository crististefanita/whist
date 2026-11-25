package ro.whistlab.app.observation;

import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ro.cardtable.ui.observation.AndroidSurfaceObservation;
import ro.cardtable.ui.observation.RenderedSurface;
import ro.cardtable.ui.observation.SurfaceEpoch;

/** Binds the currently mounted Whist Views; portable geometry is never an observation input. */
public final class WhistSurfaceObservation {
    private static final Set<String> REQUIRED = Set.of("game-title", "game-progress", "game-bid-summary",
            "trump-indicator", "trump-heading", "trump-value");
    private final View root;
    private final SurfaceEpoch epoch = new SurfaceEpoch();
    private final SurfaceEpoch.Ticket ticket;

    public WhistSurfaceObservation(View root, String owner) {
        this.root = java.util.Objects.requireNonNull(root);
        ticket = epoch.begin(owner);
    }

    public RenderedSurface capture() {
        epoch.requireCurrent(ticket);
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper())
            throw new IllegalStateException("NATIVE_OBSERVATION_WRONG_THREAD");
        var bindings = new ArrayList<AndroidSurfaceObservation.Binding>();
        var ids = new HashSet<String>();
        collect(root, null, bindings, ids);
        if (!ids.containsAll(REQUIRED)) {
            var missing = new java.util.TreeSet<>(REQUIRED); missing.removeAll(ids);
            throw new IllegalStateException("MISSING_WHIST_BINDING: " + missing);
        }
        return AndroidSurfaceObservation.capture(root, bindings, epoch, ticket);
    }

    public void dispose() { epoch.dispose(); }

    private static void collect(View view, String card, List<AndroidSurfaceObservation.Binding> bindings,
            Set<String> ids) {
        String tag = view.getTag() instanceof String value ? value : "";
        if (tag.startsWith("hand-card:") || tag.startsWith("trick-face:")) card = tag;
        String id = REQUIRED.contains(tag) || tag.equals(card) ? tag
                : tag.equals("card-index") && card != null ? card + "/index" : null;
        if (id != null) {
            if (!ids.add(id)) throw new IllegalStateException("DUPLICATE_WHIST_BINDING: " + id);
            bindings.add(new AndroidSurfaceObservation.Binding(id, view));
        }
        if (view instanceof ViewGroup group)
            for (int index = 0; index < group.getChildCount(); index++)
                collect(group.getChildAt(index), card, bindings, ids);
    }
}
