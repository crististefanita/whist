package ro.cardtable.ui.observation;

/** A callback retains its original ticket; a new owner or repaint invalidates it. */
public final class SurfaceEpoch {
    public record Ticket(String owner, long revision) { }
    private Ticket current;
    private long sequence;

    public synchronized Ticket begin(String owner) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("MISSING_RENDER_OWNER");
        current = new Ticket(owner, sequence);
        sequence = Math.addExact(sequence, 1);
        return current;
    }
    public synchronized void requireCurrent(Ticket ticket) {
        if (ticket == null || ticket != current) throw new IllegalStateException("STALE_RENDER_OWNER");
    }
    public synchronized void dispose() { current = null; }
}
