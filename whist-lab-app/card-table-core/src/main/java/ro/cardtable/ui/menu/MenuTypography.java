package ro.cardtable.ui.menu;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Logical text sizes; platform adapters apply sp or rem exactly once. */
public enum MenuTypography {
    TITLE(25), EYEBROW(12), SECTION(13), ACTION(16), SELECTION(14), HELPER(14), DEDICATION(15);

    private final int logicalPixels;
    MenuTypography(int logicalPixels) { this.logicalPixels = logicalPixels; }
    public int logicalPixels() { return logicalPixels; }
    public double rem() { return logicalPixels / 16d; }
    public static Map<String, Double> browserRoles() {
        var roles = new LinkedHashMap<String, Double>();
        for (var role : values()) if (role != EYEBROW) roles.put(role.name().toLowerCase(Locale.ROOT), role.rem());
        return Map.copyOf(roles);
    }
}
