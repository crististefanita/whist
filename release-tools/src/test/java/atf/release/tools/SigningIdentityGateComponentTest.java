package atf.release.tools;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

@AtfComponentTest
class SigningIdentityGateComponentTest {
    private static final String FINGERPRINT = "a".repeat(64);

    @Test void preservedKeyAndProducedCertificateMustMatchThePublicFingerprint(@TempDir Path scratch) throws Exception {
        Path root = project(scratch);
        AuditedAssertions.assertTrue(new SigningIdentityGate().verify(root, FINGERPRINT).certificateContinuity());
    }

    @Test void missingKeyMalformedIdentityAndCertificateRotationFailClosed(@TempDir Path scratch) throws Exception {
        Path root = project(scratch);
        Path key = root.resolve("release-tools/.atf-signing/whist-android-test.keystore");
        Files.delete(key);
        AuditedAssertions.assertEquals("SIGNING_KEY_CONTINUITY_MISSING_OR_INVALID",
                AuditedAssertions.assertThrows(IllegalArgumentException.class,
                        () -> new SigningIdentityGate().verify(root, FINGERPRINT)).getMessage());
        Files.write(key, new byte[] {1, 2, 3});
        AuditedAssertions.assertEquals("APK_CERTIFICATE_CONTINUITY_MISMATCH",
                AuditedAssertions.assertThrows(IllegalArgumentException.class,
                        () -> new SigningIdentityGate().verify(root, "b".repeat(64))).getMessage());
        Files.writeString(root.resolve("release-tools/.atf-signing/whist-android-test.fingerprint"), "not-a-fingerprint");
        AuditedAssertions.assertEquals("SIGNING_CERTIFICATE_FINGERPRINT_INVALID",
                AuditedAssertions.assertThrows(IllegalArgumentException.class,
                        () -> new SigningIdentityGate().verify(root, FINGERPRINT)).getMessage());
    }

    @Test void aNonDirectoryModuleParentCannotSubstituteAnExternalIdentity(@TempDir Path scratch) throws Exception {
        Path root = Files.createDirectories(scratch.resolve("project"));
        Files.writeString(root.resolve("release-tools"), "not a module directory");
        AuditedAssertions.assertEquals("SIGNING_KEY_CONTINUITY_MISSING_OR_INVALID",
                AuditedAssertions.assertThrows(IllegalArgumentException.class,
                        () -> new SigningIdentityGate().verify(root, FINGERPRINT)).getMessage());
    }

    private static Path project(Path root) throws Exception {
        Path identity = Files.createDirectories(root.resolve("release-tools/.atf-signing"));
        Files.write(identity.resolve("whist-android-test.keystore"), new byte[] {4, 5, 6});
        Files.writeString(identity.resolve("whist-android-test.fingerprint"), FINGERPRINT + "\n");
        return root;
    }
}
