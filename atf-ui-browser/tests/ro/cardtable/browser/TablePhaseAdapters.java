package ro.cardtable.browser;

import java.util.List;
import java.util.Map;
import ro.whistlab.app.table.NativePhaseAdapter;
import ro.whistlab.app.table.TablePhaseContract;
import ro.whistlab.browser.presentation.WhistBoardAdapter;
import ro.cardtable.ui.table.TableViewportPolicy;

/** Browser boundary only; native cases, transition rules and assertions are imported unchanged. */
public final class TablePhaseAdapters {
    private static final BrowserGame WEB = new BrowserGame() {
        public String title() { return "Whist"; }
        public boolean usesMeasuredSeatFrames() { return true; }
        public List<Field> fields() { return List.of(); }
        public Match start(Map<String, String> values) { throw new UnsupportedOperationException("Controlled geometry"); }
        @Override public BoardProfile boardProfile(String mode, int players, int width, int options, TableViewportPolicy viewport, float scale) {
            return WhistBoardAdapter.profile(mode, players, width, options, viewport, scale);
        }
    };
    private TablePhaseAdapters() { }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("negative")) { negativeControls(); return; }
        TablePhaseContract.Adapter nativeAdapter = new NativePhaseAdapter();
        TablePhaseContract.Adapter browser = (fixture, state) -> BrowserGeometry.measure(WEB, Map.of(
                "mode", fixture.mode().name(), "players", "" + fixture.players(), "cards", "" + fixture.cards(),
                "width", "" + fixture.width(), "height", "" + Math.max(200, fixture.viewport() - 280),
                "viewport", "" + fixture.viewport(), "fontScale", "" + fixture.font(),
                "seatHeight", "" + Math.round(52 + 30 * fixture.font()), "handWidth", "" + fixture.width(),
                "bidOptions", state.phase() == TablePhaseContract.Phase.BIDDING ? "" + (fixture.cards() + 1) : "0")).board();
        var result = TablePhaseContract.verify(nativeAdapter, browser);
        System.out.println(BrowserJson.encode(Map.of("status", "PASS", "contract", "TABLE-PHASE-CONTINUITY",
                "observed", result, "boundary", "Shared native policy and Web adapters; no native widget or pixel claim")));
    }

    private static void negativeControls() {
        int rejected = 0;
        for (int players = 3; players <= 6; players++) {
            var fixture = new TablePhaseContract.Case(ro.whistlab.core.rules.GameMode.OH_HELL, players, 8, 414, 896, 1);
            var before = new NativePhaseAdapter().measure(fixture, new TablePhaseContract.State(TablePhaseContract.Phase.BIDDING, 0, -1));
            for (int seat = 0; seat < players; seat++) for (var phase : TablePhaseContract.Phase.values()) {
                var frames = new java.util.ArrayList<>(before.seats()); var r = frames.get(seat);
                frames.set(seat, new ro.cardtable.ui.table.TableGeometry.Rect(r.left(), r.top() + 1, r.right(), r.bottom() + 1));
                var after = new ro.cardtable.ui.table.TableGeometry.Plan(frames, before.center());
                try { TablePhaseContract.assertStable(fixture, new TablePhaseContract.State(phase, seat, seat), before, after); }
                catch (AssertionError expected) { if (!expected.getMessage().startsWith("TABLE_PHASE_FRAME_DRIFT")) throw expected; rejected++; continue; }
                throw new AssertionError("A displaced seat escaped the common oracle");
            }
            var missing = new ro.cardtable.ui.table.TableGeometry.Plan(before.seats().subList(1, players), before.center());
            try { TablePhaseContract.assertStable(fixture, new TablePhaseContract.State(TablePhaseContract.Phase.PLAY, 0, -1), before, missing); }
            catch (AssertionError expected) { if (!expected.getMessage().startsWith("TABLE_PHASE_POPULATION")) throw expected; rejected++; continue; }
            throw new AssertionError("A missing player escaped the common oracle");
        }
        System.out.println(BrowserJson.encode(Map.of("status", "PASS", "rejected", rejected,
                "expected", "Every actor, phase and displaced seat, including the lower five/six-player frames, is rejected")));
    }
}
