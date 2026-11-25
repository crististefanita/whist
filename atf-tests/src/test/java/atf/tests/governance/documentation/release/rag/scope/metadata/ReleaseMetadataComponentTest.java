package atf.tests.governance.documentation.release.rag.scope.metadata;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.reporting.GameReportAudit;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class ReleaseMetadataComponentTest {
    @Test void metadataIsIndependentFromArtifactQualification(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Real metadata reader, controlled seven-flow catalog and missing APK/HTML/execution files. A failed primary group remains failed; source-bound metadata rechecks reject changed bytes.");
        String observed=NodeReleaseContracts.runContracts(Duration.ofSeconds(60), value->inputs.add("Controlled metadata and isolated snapshot observations",value),
                "release-metadata-preflight.test.mjs", "component-source-check.test.mjs");
        assertTrue(observed.contains("primary-failure=preserved; artifacts=unread"), "Metadata success cannot replace a failed primary group or prove absent HTML bytes");
        assertTrue(observed.contains("metadata rejection cases=11"), "Inventory, path and decision refusals retain their controlled population");
        assertTrue(observed.contains("the real snapshot checker captures rejected observations"), "Consumers of snapshot primitives do not acquire the Java/editorial launcher dependency");
    }
}
