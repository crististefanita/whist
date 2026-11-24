package ro.cardtable.ui.menu;

/** Semantic action sizes, independent of filled/quiet styling and applied once by each adapter. */
public enum ActionTypography {
    COMMAND(16), SUPPORTING(14);

    private final int logicalPixels;
    ActionTypography(int logicalPixels) { this.logicalPixels = logicalPixels; }
    public int logicalPixels() { return logicalPixels; }
}
