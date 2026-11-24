package ro.cardtable.testing;

import java.util.List;
import org.junit.Test;
import ro.cardtable.ui.observation.RenderedSurface;
import ro.cardtable.ui.observation.SurfaceEpoch;
import static org.junit.Assert.*;

public class RenderedSurfaceUnitTest {
    private static RenderedSurface sample(double scale, double shift, boolean clipped, boolean ellipsis) {
        var frame = new RenderedSurface.Box(10*scale, 20*scale, 90*scale, 80*scale);
        var text = new RenderedSurface.Box(12*scale, 22*scale, (clipped ? 92 : 88)*scale, 40*scale);
        var moved = new RenderedSurface.Box(frame.left()+shift, frame.top(), frame.right()+shift, frame.bottom());
        return new RenderedSurface("match", 1, scale,
                List.of(new RenderedSurface.Region("seat:0", moved, moved, List.of(text), ellipsis)));
    }
    @Test public void normalizedMeasurementsRetainGeometryAcrossDensity() {
        for (double density : new double[]{1,1.25,1.5,2,2.75,3,4})
            RenderedSurfaceAudit.stable(sample(1,0,false,false), sample(density,0,false,false), 0.001);
    }
    @Test public void unchangedFramesCannotHideClippingEllipsisOrMovement() {
        assertThrows(AssertionError.class, () -> RenderedSurfaceAudit.visible(sample(1,0,true,false),0));
        assertThrows(AssertionError.class, () -> RenderedSurfaceAudit.visible(sample(1,0,false,true),0));
        assertThrows(AssertionError.class, () -> RenderedSurfaceAudit.stable(sample(1,0,false,false),sample(1,1,false,false),0));
    }
    @Test public void disposedAndReplacedOwnersCannotReplayEqualOrForgedTickets() {
        var epoch = new SurfaceEpoch(); var first = epoch.begin("match");
        assertThrows(IllegalStateException.class, () -> epoch.requireCurrent(new SurfaceEpoch.Ticket("match",first.revision())));
        epoch.dispose(); var next = epoch.begin("match");
        assertTrue(next.revision() > first.revision());
        assertThrows(IllegalStateException.class, () -> epoch.requireCurrent(first));
        epoch.requireCurrent(next);
    }
    @Test public void malformedValuesAndDuplicateIdsAreRejected() {
        for (double scale : new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,-1})
            assertThrows(IllegalArgumentException.class, () -> sample(scale,0,false,false));
        var valid=sample(1,0,false,false); var region=valid.regions().get(0);
        assertThrows(IllegalArgumentException.class, () -> new RenderedSurface("match",0,1,List.of(region,region)));
        assertThrows(IllegalArgumentException.class, () -> RenderedSurfaceAudit.visible(valid,Double.NaN));
        var foreign=new RenderedSurface("other",1,1,List.of(region));
        assertThrows(AssertionError.class, () -> RenderedSurfaceAudit.stable(valid,foreign,0));
        var stale=new RenderedSurface("match",0,1,List.of(region));
        assertThrows(AssertionError.class, () -> RenderedSurfaceAudit.stable(valid,stale,0));
    }
}
