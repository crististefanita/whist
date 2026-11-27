package atf.release.tools;

import atf.testkit.assertions.AuditedAssertions;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static atf.testkit.assertions.AuditedAssertions.*;

@Tag("component")
@AtfComponentTest
class ReleaseEvidenceGateComponentTest {
    @Test void verifiesTheExactBytesForEveryDeclaredEvidenceFile(@TempDir Path scratch) throws Exception {
        Path candidate = Files.createDirectory(scratch.resolve("candidate"));
        byte[] receipt = "component receipt".getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(candidate.resolve("reports"));
        Files.write(candidate.resolve("reports/component.json"), receipt);
        Path manifest = writeManifest(candidate, "reports/component.json", receipt.length, sha256(receipt));
        byte[] original = Files.readAllBytes(manifest);

        ReleaseEvidenceGate.Result result = new ReleaseEvidenceGate().verify(candidate, manifest);

        assertEquals(1, result.filesVerified());
        assertEquals(sha256(original), result.manifestSha256());
        assertArrayEquals(original, Files.readAllBytes(manifest));
    }

    @Test void changedBytesAndEscapingOrPrivatePathsAreRejected(@TempDir Path scratch) throws Exception {
        Path candidate = Files.createDirectory(scratch.resolve("candidate"));
        Files.createDirectories(candidate.resolve("reports"));
        Files.writeString(candidate.resolve("reports/component.json"), "changed bytes");
        Path wrongHash = writeManifest(candidate, "reports/component.json", 12, "0".repeat(64));
        assertEquals("RELEASE_EVIDENCE_BYTES_OR_HASH:reports/component.json",
                assertThrows(IllegalArgumentException.class, () -> new ReleaseEvidenceGate().verify(candidate, wrongHash)).getMessage());

        Path traversal = writeManifest(candidate, "../outside.json", 1, "0".repeat(64));
        assertEquals("RELEASE_EVIDENCE_PATH_INVALID",
                assertThrows(IllegalArgumentException.class, () -> new ReleaseEvidenceGate().verify(candidate, traversal)).getMessage());

        Path privateKey = writeManifest(candidate, "release-tools/.atf-signing/whist-android-test.keystore", 1, "0".repeat(64));
        assertEquals("RELEASE_EVIDENCE_PRIVATE_SIGNING_PATH",
                assertThrows(IllegalArgumentException.class, () -> new ReleaseEvidenceGate().verify(candidate, privateKey)).getMessage());
    }

    private static Path writeManifest(Path candidate, String path, long bytes, String hash) throws Exception {
        String json = "{\"integrity\":{\"files\":[{\"path\":\"" + path + "\",\"bytes\":" + bytes
                + ",\"sha256\":\"" + hash + "\"}]}}";
        Path manifest = candidate.resolve("release-validation.json");
        Files.writeString(manifest, json);
        return manifest;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
