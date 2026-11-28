package atf.release.tools;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@AtfComponentTest
class ReleaseToolsScriptBoundaryComponentTest {
    @Test void adaptersUseTheOwningCheckoutAndPortableSigningIdentity() throws Exception {
        Path module = Path.of("").toAbsolutePath().normalize();
        Path root = module.getParent();
        String resolver = Files.readString(module.resolve("scripts/Resolve-ReleaseToolsProject.ps1"));
        String signer = Files.readString(module.resolve("scripts/Build-SignedReleaseApk.ps1"));
        AuditedAssertions.assertTrue(resolver.contains("$scriptProjectRoot") && resolver.contains("$moduleRoot"),
                "Root resolution must distinguish checkout and module directories");
        AuditedAssertions.assertTrue(resolver.contains("release/index.json")
                        && resolver.contains("release-tools/config/release-platform-rag-policy.json"),
                "The selected checkout is anchored to stable project markers");
        AuditedAssertions.assertTrue(signer.contains("Join-Path $root 'release-tools/.atf-signing'"),
                "The existing module identity is the signing input");
        AuditedAssertions.assertTrue(signer.contains("'--project-prop', \"android.injected.signing.store.file=$preservedKey\"")
                        && signer.contains("'--project-prop', 'android.injected.signing.store.password=android'")
                        && signer.contains("'--project-prop', 'android.injected.signing.key.alias=androiddebugkey'")
                        && signer.contains("'--project-prop', 'android.injected.signing.key.password=android'"),
                "Gradle receives every signing property as a distinct option and value");
        AuditedAssertions.assertTrue(signer.contains("APK_CERTIFICATE_CONTINUITY_MISMATCH")
                        && signer.contains("whist-android-test.fingerprint")
                        && signer.contains("SIGNING_KEY_LINK_REFUSED")
                        && signer.contains("FileAttributes]::ReparsePoint"),
                "The produced signer is matched to the public identity");
        AuditedAssertions.assertFalse(signer.contains(".android/debug.keystore")
                        || signer.contains("GetFolderPath('UserProfile')"),
                "Signing must not depend on a machine-local debug keystore");
        AuditedAssertions.assertTrue(Files.isRegularFile(root.resolve("pom.xml")));
    }

    @Test void windowsBatchArgumentsDoNotUseTheBrokenCombinedProjectPropertyForm() throws Exception {
        Path module = Path.of("").toAbsolutePath().normalize();
        String signer = Files.readString(module.resolve("scripts/Build-SignedReleaseApk.ps1"));
        AuditedAssertions.assertTrue(signer.contains("'--project-prop'"),
                "The Windows wrapper receives the Gradle project-property option separately");
        AuditedAssertions.assertFalse(signer.contains("-Pandroid.injected.signing."),
                "The ambiguous combined -P form must not return to the Windows batch invocation");
    }

    @Test void operationAdaptersStaySeparateFromRagAndGameTestOwners() throws Exception {
        Path module = Path.of("").toAbsolutePath().normalize();
        Path root = module.getParent();
        List<String> paths = List.of(
                "scripts/Invoke-ReleaseRag.ps1", "scripts/Build-SignedReleaseApk.ps1", "scripts/Invoke-ReleaseToolsJavaGate.ps1",
                "scripts/Publish-GithubRelease.ps1", "scripts/Verify-GithubReleaseDownload.ps1",
                "scripts/Sync-LocalRelease.ps1");
        for (String relative : paths) {
            String content = Files.readString(module.resolve(relative));
            if (!relative.endsWith("Invoke-ReleaseToolsJavaGate.ps1")) {
                AuditedAssertions.assertTrue(content.contains("Resolve-ReleaseToolsProject.ps1"), relative);
                AuditedAssertions.assertTrue(content.contains("Assert-ReleaseToolsExternalPath"), relative);
            } else {
                AuditedAssertions.assertTrue(content.contains("release-tools/pom.xml")
                                && content.contains("AtfQualificationSnapshot.ps1")
                                && content.contains("Get-AtfQualificationTempRoot")
                                && content.contains("-Datf.releaseTools.buildDirectory=$buildDirectory")
                                && content.contains("RELEASE_TOOLS_BUILD_DIRECTORY_IN_CHECKOUT")
                                && !content.contains("'pom.xml') -pl release-tools"),
                        "The release gate compiles only its owner and writes build output outside the checkout");
            }
        }
        AuditedAssertions.assertTrue(Files.isRegularFile(root.resolve("atf-tests/tools/evaluate-release-rag.mjs")));
        AuditedAssertions.assertTrue(Files.isRegularFile(root.resolve("scripts/run-component-suites.ps1")));
        AuditedAssertions.assertFalse(Files.exists(module.resolve("game-tests")),
                "Android game and device tests remain with their owners");
    }
}
