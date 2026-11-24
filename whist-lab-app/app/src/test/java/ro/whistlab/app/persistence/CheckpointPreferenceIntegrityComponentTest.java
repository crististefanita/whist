package ro.whistlab.app.persistence;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public class CheckpointPreferenceIntegrityComponentTest extends GradleAuditedTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private static final String KEY = "current_session_v1";

    @Test public void absentAndValidEmptyPreferencesHaveNoCheckpointOrWrite() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("game.xml");
        GradleInputs.fixture("Preconditions", "state", "missing document, then a valid empty preferences map");
        assertNull("Missing storage is an absent checkpoint", CheckpointPreferenceIntegrity.read(() -> null, file.toFile(), KEY));
        assertFalse("Reading absence creates no preference file", Files.exists(file));
        Files.write(file, "<?xml version='1.0' encoding='utf-8'?><map><int name='other' value='4'/></map>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String original = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
        assertNull("A valid map without the checkpoint key is legitimate absence", CheckpointPreferenceIntegrity.read(() -> null, file.toFile(), KEY));
        assertEquals("Read leaves unrelated preferences intact", original, new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test public void corruptMissingOrInconsistentValuesAreExplicitAndPreserveOriginalBytes() throws Exception {
        for (String xml : new String[]{"<map><string", "<other/>", "<map><string name='current_session_v1'>saved</string></map>",
                "<!DOCTYPE map [<!ENTITY external SYSTEM 'file:///missing'>]><map>&external;</map>"}) {
            Path file = Files.write(temporary.newFile().toPath(), xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            GradleInputs.fixture("Controlled unreadable preference document", "xml", xml);
            var failure = assertThrows("Corruption cannot silently become a missing game", IllegalStateException.class,
                    () -> CheckpointPreferenceIntegrity.read(() -> null, file.toFile(), KEY));
            assertTrue("Failure identifies the checkpoint document", failure.getMessage().contains("checkpoint preference document"));
            assertEquals("Diagnosis preserves the original bytes", xml, new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @Test public void typedValueAndUnexpectedFailuresPreserveTheirOriginalCause() {
        var wrongType = new ClassCastException("integer cannot become checkpoint JSON");
        var original = new IllegalArgumentException("Unexpected preference provider defect");
        GradleInputs.fixture("Controlled providers", "wrongType", wrongType.toString(), "unexpected", original.toString());
        var failure = assertThrows("Wrong preference types are storage diagnostics", IllegalStateException.class,
                () -> CheckpointPreferenceIntegrity.read(() -> { throw wrongType; }, temporary.getRoot(), KEY));
        assertSame("The original wrong-type cause survives", wrongType, failure.getCause());
        assertSame("Unrelated defects are not masked as corruption", original,
                assertThrows("Unexpected provider failure remains exact", IllegalArgumentException.class,
                        () -> CheckpointPreferenceIntegrity.read(() -> { throw original; }, temporary.getRoot(), KEY)));
    }

    @Test public void nonemptyEncodedValuesRemainExactButBlankValuesCannotResume() {
        String encoded = "  {\"player\":\"Ștef ♠\"}  ";
        GradleInputs.fixture("Stored values", "encoded", encoded, "blank", "  ");
        assertEquals("The codec receives the original JSON without rewriting", encoded,
                CheckpointPreferenceIntegrity.read(() -> encoded, temporary.getRoot(), KEY));
        assertThrows("Blank stored checkpoint is explicit corruption", IllegalStateException.class,
                () -> CheckpointPreferenceIntegrity.read(() -> "  ", temporary.getRoot(), KEY));
    }
}
