package ro.whistlab.app.table;

import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableSizing;
import ro.cardtable.ui.table.TableViewportPolicy;

/** Uses the portable policies consumed by Android BoardLayout, without Android Views. */
public final class NativePhaseAdapter implements TablePhaseContract.Adapter {
    @Override public TableGeometry.Plan measure(TablePhaseContract.Case fixture, TablePhaseContract.State state) {
        var viewport = new TableViewportPolicy(fixture.viewport(), fixture.font());
        var reservation = TableLayoutMetrics.matchCenterReservation(fixture.mode(), fixture.players(),
                fixture.width(), fixture.font(), viewport);
        boolean bidding = state.phase() == TablePhaseContract.Phase.BIDDING;
        var center = reservation.center(fixture.width(), fixture.font(), bidding, bidding ? fixture.cards() + 1 : 0);
        var board = new TableGeometry.BoardSpec(fixture.players(), TableSizing.seatWidthDp(fixture.width(), viewport.isCompact()),
                Math.round(52 + 30 * fixture.font()), center, viewport.regionGapDp(), viewport.minimumBoardHeightDp(),
                TableLayoutMetrics.HUMAN_HAND_CLEARANCE_DP, true);
        int height = Math.max(Math.max(200, fixture.viewport() - 280), board.requiredHeight(0));
        return board.plan(fixture.width(), height, 0);
    }
}
