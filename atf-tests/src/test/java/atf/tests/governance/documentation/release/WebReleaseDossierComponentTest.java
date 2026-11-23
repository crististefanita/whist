package atf.tests.governance.documentation.release;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class WebReleaseDossierComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @TempDir Path temporary;

    @Test void portableWebInventoryChecksHashesPathsAndEvidenceOwnership(ComponentInputs inputs) throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(30),
                value -> inputs.add("Synthetic positive and negative dossier controls", value), "web-release-dossier.test.mjs");
        assertTrue(output.contains("Web dossier observations:"), "Portable links and original bytes are checked alongside deliberate corruptions");
    }

    @Test void currentWebDossierHasOneIllustratedFlowAndNoComponentCaptures(ComponentInputs inputs) throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", root.resolve("atf-tests/tools/web-release-dossier.mjs").toString(), root.toString()),
                root, Duration.ofSeconds(30), value -> inputs.add("Current Web report inventory and immutable index digest", value));
        assertTrue(output.contains("\"status\":\"PASS\""), "Current relative report inventory is complete and hash-bound");
        assertEquals(2, ReleaseArtifactCatalog.folders(root).size(), "The separate Web dossier is not counted as an Android edition");
    }

    @Test void onlyTheExplicitTypedWebFolderIsAcceptedBesideAndroidVersions(ComponentInputs inputs) throws Exception {
        Path release = Files.createDirectory(temporary.resolve("release"));
        Path version = Files.createDirectory(release.resolve("1.2.0"));
        Files.createDirectory(version.resolve("reports"));
        Files.writeString(version.resolve("README.md"), "Synthetic release");
        Files.writeString(version.resolve("reports/README.md"), "Synthetic reports");
        Files.writeString(version.resolve("release-validation.json"), "{\"release\":{\"versionName\":\"1.2.0\"}}");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var index = json.createObjectNode().put("schemaVersion", 2).put("currentVersion", "1.2.0");
        index.putArray("versions").add("1.2.0");
        Path web = Files.createDirectory(release.resolve("WEB"));
        Files.writeString(web.resolve("README.md"), "Synthetic Web index");
        Files.writeString(web.resolve("index.json"), "{\"schemaVersion\":1,\"kind\":\"WHIST_WEB_TEST_DOSSIER\"}");
        json.writeValue(release.resolve("index.json").toFile(), index);
        inputs.add("Negative controls", "An unregistered Web folder, wrong registration, malformed type and missing index remain rejected; a valid dossier does not add an Android version.");
        assertThrows(java.io.IOException.class, () -> ReleaseArtifactCatalog.folders(temporary));
        index.put("webDossier", "OTHER/index.json"); json.writeValue(release.resolve("index.json").toFile(), index);
        assertThrows(java.io.IOException.class, () -> ReleaseArtifactCatalog.folders(temporary));
        index.put("webDossier", "WEB/index.json"); json.writeValue(release.resolve("index.json").toFile(), index);
        assertEquals(1, ReleaseArtifactCatalog.folders(temporary).size(), "Typed Web scope stays separate from Android versions");
        Files.writeString(web.resolve("index.json"), "{\"schemaVersion\":1,\"kind\":\"OTHER\"}");
        assertThrows(java.io.IOException.class, () -> ReleaseArtifactCatalog.folders(temporary));
    }
}
