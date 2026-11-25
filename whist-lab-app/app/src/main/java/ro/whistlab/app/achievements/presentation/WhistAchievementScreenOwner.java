package ro.whistlab.app.achievements.presentation;

import android.os.Bundle;
import ro.cardtable.ui.achievements.AchievementScreen;

/** Activity-owned navigation snapshot; durable achievement progress remains in its repository. */
public final class WhistAchievementScreenOwner {
    private static final String KEY = "whistAchievementScreen";
    private final AchievementScreen.State state = new AchievementScreen.State();
    private boolean reopen;

    public AchievementScreen.State state() { return state; }
    public boolean reopen() { return reopen; }

    public void restore(Bundle activity) {
        Bundle saved = activity == null ? null : activity.getBundle(KEY);
        if (saved == null) return;
        state.restore(saved);
        reopen = saved.getBoolean("visible", false);
    }

    public void save(Bundle activity, boolean visible) {
        Bundle snapshot = new Bundle();
        state.save(snapshot);
        snapshot.putBoolean("visible", visible);
        activity.putBundle(KEY, snapshot);
    }
}
