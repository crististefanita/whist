package ro.whistlab.app.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.testing.SurfaceObservation;
import ro.cardtable.testing.SurfaceContinuity;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

/** One executable contract for native and browser policy adapters; no widget or screenshot dependency. */
public final class TablePhaseContract {
    public enum Phase { BIDDING, PLAY, COLLECTION, AFTER_COLLECTION }
    public record Case(GameMode mode, int players, int cards, int width, int viewport, float font) { }
    public record State(Phase phase, int actor, int winner) { }
    public interface Adapter { TableGeometry.Plan measure(Case fixture, State state); }
    public record Result(int configurations, int transitions, long seatComparisons, List<String> modes,
                         List<Map<String, Object>> examples) { }

    private TablePhaseContract() { }

    public static Result verify(Adapter... adapters) {
        if (adapters.length == 0) throw new IllegalArgumentException("At least one platform adapter is required");
        int configurations = 0, transitions = 0; long comparisons = 0;
        var examples = new ArrayList<Map<String, Object>>();
        for (var mode : GameMode.values()) for (int players = 3; players <= 6; players++)
            for (int width : new int[]{280, 320, 414, 560, 760}) for (int viewport : new int[]{568, 896, 1280})
                for (float font : new float[]{1f, 1.3f, 2f}) {
                    var first = new Case(mode, players, 1, width, viewport, font);
                    var baseline = adapters[0].measure(first, new State(Phase.BIDDING, 0, -1));
                    for (int cards : GameRules.handSizes(mode, players).stream().distinct().toList()) {
                        var fixture = new Case(mode, players, cards, width, viewport, font);
                        configurations++;
                        for (int seat = 0; seat < players; seat++) for (var phase : Phase.values()) {
                            var state = new State(phase, phase == Phase.BIDDING || phase == Phase.PLAY ? seat : -1,
                                    phase == Phase.COLLECTION || phase == Phase.AFTER_COLLECTION ? seat : -1);
                            for (var adapter : adapters) {
                                var measured = adapter.measure(fixture, state);
                                assertStable(fixture, state, baseline, measured);
                                comparisons += players; transitions++;
                            }
                        }
                        if (width == 414 && viewport == 896 && font == 1 && cards == 8)
                            examples.add(Map.of("fixture", fixture.toString(), "expectedSeats", baseline.seats(),
                                    "actorsAndWinners", players, "phases", java.util.Arrays.stream(Phase.values()).map(Enum::name).toList()));
                    }
                }
        return new Result(configurations, transitions, comparisons,
                java.util.Arrays.stream(GameMode.values()).map(Enum::name).toList(), List.copyOf(examples));
    }

    public static void assertStable(Case fixture, State state, TableGeometry.Plan before, TableGeometry.Plan after) {
        if (before == null || after == null || before.seats().size() != fixture.players()
                || after.seats().size() != fixture.players()) fail("POPULATION", fixture, state);
        try { SurfaceContinuity.verify(observation(before), observation(after), 0); }
        catch (AssertionError error) { fail("FRAME_DRIFT", fixture, state); }
        if (after.seats().stream().anyMatch(rect -> rect.right() <= rect.left() || rect.bottom() <= rect.top()
                || rect.left() < 0 || rect.right() > fixture.width() || rect.top() < 0)) fail("FRAME_BOUNDS", fixture, state);
        if (after.seats().stream().anyMatch(after.center()::overlaps)) fail("CONTROL_OVERLAP", fixture, state);
    }

    private static SurfaceObservation observation(TableGeometry.Plan plan) {
        var measured = new java.util.LinkedHashMap<String, SurfaceObservation.Bounds>();
        for (int id = 0; id < plan.seats().size(); id++) {
            var rect = plan.seats().get(id);
            measured.put("seat:" + id, new SurfaceObservation.Bounds(rect.left(), rect.top(),
                    rect.right() - rect.left(), rect.bottom() - rect.top()));
        }
        return new SurfaceObservation(measured);
    }

    private static void fail(String code, Case fixture, State state) {
        throw new AssertionError("TABLE_PHASE_" + code + ": " + fixture + "; " + state);
    }
}
