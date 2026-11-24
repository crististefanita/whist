package atf.android.e2e.support.journal;

import atf.android.e2e.support.AndroidUiFlowTrace.CheckpointSnapshot;
import atf.android.e2e.support.AndroidUiFlowTrace.PhaseSnapshot;
import atf.android.e2e.support.flow.AndroidUiFlowEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfUnitTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AndroidUiFlowJournalUnitTest {
    @Test void allStatusesAndCompleteUnicodeFieldsRoundTripWithoutChangingOwners() throws Exception {
        for (var status : AtfReportStatus.values()) {
            var flow = new Fixture(status);
            var encoded = AndroidUiFlowJournal.encode(flow, "BODY", Set.of());
            var bytes = new ByteArrayOutputStream();
            encoded.store(bytes, null);
            var decoded = new Properties();
            decoded.load(new ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(encoded, decoded);
            assertEquals(status.name(), decoded.getProperty("flow.status"));
            assertEquals(status.name(), decoded.getProperty("phase.0.step.0.status"));
            assertEquals(status.name(), decoded.getProperty("phase.0.step.0.action.0.status"));
            assertEquals("Captured\nșir 😀 <xml> = final", decoded.getProperty("phase.0.step.0.action.0.observed"));
            assertEquals("OBSERVE", decoded.getProperty("phase.0.step.0.action.0.source"));
            assertEquals("ACTION", decoded.getProperty("flow.pendingAction.0.source"));
            assertEquals("31", decoded.getProperty("flow.pendingAction.0.durationMillis"));
            assertEquals("14", decoded.getProperty("phase.0.step.0.action.0.sequence"));
            assertEquals("captured.png", decoded.getProperty("phase.0.step.0.screenshot"));
            assertEquals("inputs/state.xml", decoded.getProperty("phase.0.step.0.artifact.0"));
            assertEquals("complete actual", decoded.getProperty("phase.0.step.0.assertion.0.actual"));
            assertEquals("FAIL", decoded.getProperty("phase.0.step.0.assertion.0.status"));
            assertEquals("BODY", decoded.getProperty("flow.failurePhase"));
        }
    }

    @Test void phaseOrderAndRunningIdentityRemainExplicitWithoutDeviceOrFilesystem() {
        var encoded = AndroidUiFlowJournal.encode(new Fixture(AtfReportStatus.PASS), "", Set.of("BODY"));
        assertEquals("2", encoded.getProperty("flow.phaseCount"));
        assertEquals("BODY", encoded.getProperty("phase.0.name"));
        assertEquals("TEARDOWN", encoded.getProperty("phase.1.name"));
        assertEquals("0", encoded.getProperty("phase.1.stepCount"));
        assertEquals("RUNNING", encoded.getProperty("phase.0.timing.state"));
        assertEquals("fixture", encoded.getProperty("phase.0.timing.flowId"));
        assertEquals("0", encoded.getProperty("phase.0.timing.ordinal"));
        assertNull(encoded.getProperty("phase.1.timing.state"));
    }

    private record Fixture(AtfReportStatus status) implements AndroidUiFlowEvidence {
        public Path manifest() { return Path.of("journal-unit-root").toAbsolutePath().resolve("flow.properties"); }
        public String id() { return "fixture"; }
        public String name() { return "Pure journal fixture"; }
        public String testClass() { return "Fixture"; }
        public String testPackage() { return "atf.android.journal"; }
        public String appPackage() { return "ro.whistlab.app"; }
        public List<String> tags() { return List.of("unit"); }
        public String failure() { return status == AtfReportStatus.FAIL ? "Original cause" : ""; }
        public boolean hasOpenPhase() { return false; }
        public List<AtfUiActionRecord> pendingActions() { return List.of(new AtfUiActionRecord(13,
                "Tap", "Continue", "accepted", "accepted", 31, status)); }
        public List<PhaseSnapshot> phaseSnapshots() {
            var root = manifest().getParent();
            var observation = new AtfUiActionRecord(14, "Inspect", "score", "expected",
                    "Captured\nșir 😀 <xml> = final", 0, status, AtfUiEvidenceSource.OBSERVE);
            var checkpoint = new CheckpointSnapshot("THEN", "Captured state", "score", root.resolve("captured.png"),
                    List.of(observation), List.of(new AtfAssertion("Complete comparison", "complete expected",
                    "complete actual", false)), status, List.of(root.resolve("inputs/state.xml")));
            return List.of(new PhaseSnapshot("BODY", List.of(checkpoint)), new PhaseSnapshot("TEARDOWN", List.of()));
        }
    }
}
