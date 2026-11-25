package atf.android.e2e.driver.actions;

import atf.android.AdbClient;
import atf.android.evidence.component.ComponentInputs;
import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class AndroidUiActionsComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @TempDir Path output;

    @Test void facadePreservesCommandOrderSequenceAndDispatchOnlyDurations(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Real driver facade, command and hierarchy-transfer double, controlled monotonic clock; no device, screenshot or wall-clock sleep.");
        var ticks = new AtomicLong();
        var commands = new ArrayList<List<String>>();
        var pauses = new ArrayList<Long>();
        var records = new ArrayList<AtfUiActionRecord>();
        var device = new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) throws IOException {
                commands.add(List.of(args)); ticks.addAndGet(37_000_000);
                if (args[0].equals("pull")) Files.writeString(Path.of(args[2]), "<hierarchy/>");
                return new AdbClient.Result(0, List.of(args).contains("uiautomator") ? "dumped to: fixture" : "", List.of(args));
            }
            public AdbClient.BinaryResult runBinary(String... args) { throw new AssertionError("No screenshot in an action component"); }
        };
        var driver = new AndroidUiDriver(device, output, ticks::get, millis -> {
            pauses.add(millis); ticks.addAndGet(millis * 1_000_000);
        });
        driver.recordActionsAs(records::add);
        driver.tapTextIn("<hierarchy><node text=\"Play\" class=\"android.widget.Button\" bounds=\"[10,20][110,70]\"/></hierarchy>", "Play");
        driver.back();
        driver.restartToActivity("fixture.game", ".Home", "--ez", "resume", "true");
        driver.recordAction("Observe", "checkpoint", "Context retained", "Context retained", 9, AtfReportStatus.PASS);
        assertEquals(List.of(List.of("shell","input","tap","60","45"),List.of("shell","input","keyevent","4"),
                List.of("shell","am","force-stop","fixture.game"),List.of("shell","am","start","-W","-n","fixture.game/.Home","--ez","resume","true")), commands.subList(0,4), "The facade preserves resolved commands and intent order");
        assertEquals(List.of(1,2,3,4,5), records.stream().map(AtfUiActionRecord::sequence).toList());
        assertEquals(List.of(37L,37L,37L,37L,9L), records.stream().map(AtfUiActionRecord::durationMillis).toList(), "Settling and final hierarchy transfer stay outside dispatch durations");
        assertEquals(List.of(500L,350L), pauses);
        assertEquals(List.of("Tap","Press Back","Force-stop Android app","Launch Android activity","Observe"), records.stream().map(AtfUiActionRecord::operation).toList());
        assertTrue(records.stream().allMatch(row -> row.status() == AtfReportStatus.PASS));
    }

    @Test void commandTimeoutAndCancellationKeepOriginalCauseEvenWhenFailureReportingBreaks(ComponentInputs inputs) {
        inputs.add("Precondition", "Command port throws the original timeout or cancellation; failure sink also throws. Exactly one FAIL record is attempted and the sink cause remains suppressed.");
        for (Exception cause : List.of(new IOException("original command timeout"), new InterruptedException("original cancellation"))) {
            var ticks = new AtomicLong();
            var records = new ArrayList<AtfUiActionRecord>();
            var reporting = new IllegalStateException("report sink unavailable");
            var executor = new AndroidUiActions(args -> { ticks.addAndGet(11_000_000); throw cause; }, ticks::get);
            executor.recordAs(row -> { records.add(row); throw reporting; });
            Exception observed = assertThrows(Exception.class, () -> executor.dispatch("Tap", "target", "Accepted", "Accepted", "tap"));
            assertSame(cause, observed, "The command exception remains the primary cause");
            assertEquals(List.of(reporting), List.of(observed.getSuppressed()));
            assertEquals(1, records.size());
            assertEquals(AtfReportStatus.FAIL, records.getFirst().status());
            assertEquals(cause.toString(), records.getFirst().observed());
            assertEquals(11L, records.getFirst().durationMillis());
        }
    }

    @Test void aSuccessPublicationFailureCannotInventASecondFailedCommand(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "The command succeeds once but publication fails. The original publication exception must not be reclassified as command failure.");
        var calls = new AtomicLong();
        var records = new ArrayList<AtfUiActionRecord>();
        var failure = new IllegalStateException("original publication failure");
        var executor = new AndroidUiActions(args -> calls.incrementAndGet(), () -> 0);
        executor.recordAs(row -> { records.add(row); throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class, () -> executor.dispatch("Launch", "activity", "Accepted", "Accepted", "launch")));
        assertEquals(1L, calls.get(), "Publication does not redispatch or relabel the accepted operation");
        assertEquals(1, records.size());
        assertEquals(AtfReportStatus.PASS, records.getFirst().status());
    }

    @Test void callerOwnedSetupAndBodyKeepTheirActionsAtTheOriginalCheckpoint(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Actual AndroidUiFlowTrace and driver with a shared controlled clock; setup and test body publish separate checkpoints without images.");
        var ticks = new AtomicLong();
        var trace = new AndroidUiFlowTrace(output.resolve("trace"), "controlled-actions", "Controlled action ownership",
                getClass().getSimpleName(), getClass().getPackageName(), "fixture.game", List.of("component"), ticks::get);
        var device = new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) { ticks.addAndGet(7_000_000); return new AdbClient.Result(0,"",List.of(args)); }
            public AdbClient.BinaryResult runBinary(String... args) { throw new AssertionError("No capture in this contract"); }
        };
        var driver = new AndroidUiDriver(device, output.resolve("driver"), ticks::get, millis -> ticks.addAndGet(millis*1_000_000));
        driver.recordActionsAs(trace::recordAction);
        trace.beginPhase("SETUP"); driver.back();
        trace.checkpoint("SETUP","GIVEN","Close overlay","navigation",null,List.of(),
                List.of(new AtfAssertion("Overlay command accepted","accepted","accepted",true)),AtfReportStatus.PASS,List.of());
        trace.beginPhase("TEST BODY");
        driver.tapTextIn("<hierarchy><node text=\"Play\" class=\"android.widget.Button\" bounds=\"[0,0][100,100]\"/></hierarchy>","Play");
        trace.checkpoint("TEST BODY","THEN","Dispatch play","Play",null,List.of(),
                List.of(new AtfAssertion("Play command accepted","accepted","accepted",true)),AtfReportStatus.PASS,List.of());
        trace.finish(AtfReportStatus.PASS);
        var phases=trace.phaseSnapshots();
        assertEquals(List.of("SETUP","TEST BODY"),phases.stream().map(AndroidUiFlowTrace.PhaseSnapshot::name).toList());
        assertEquals("Press Back",phases.get(0).checkpoints().getFirst().actions().getFirst().operation());
        assertEquals("Tap",phases.get(1).checkpoints().getFirst().actions().getFirst().operation());
        assertEquals(7L,phases.get(1).checkpoints().getFirst().actions().getFirst().durationMillis(),"The caller owns phases; the executor owns only command duration");
    }
}
