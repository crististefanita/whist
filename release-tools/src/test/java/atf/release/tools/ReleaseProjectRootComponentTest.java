package atf.release.tools;

import atf.testkit.assertions.AuditedAssertions;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static atf.testkit.assertions.AuditedAssertions.*;

@Tag("component")
@AtfComponentTest
class ReleaseProjectRootComponentTest {
    @Test void explicitAndEnvironmentOverridesResolveTheSameQualifiedRoot(@TempDir Path scratch) throws Exception {
        Path root = project(scratch.resolve("checkout"));
        Path nested = Files.createDirectories(root.resolve("release-tools/scripts"));

        assertEquals(root.toRealPath(), ReleaseProjectRoot.resolve(nested, root.toString(), Map.of()));
        assertEquals(root.toRealPath(), ReleaseProjectRoot.resolve(nested, "../..", Map.of()));
        assertEquals(root.toRealPath(), ReleaseProjectRoot.resolve(nested, " ", Map.of("ATF_PROJECT_ROOT", root.toString())));
        assertEquals(root.toRealPath(), ReleaseProjectRoot.resolve(nested, "", Map.of()));
    }

    @Test void invalidOverrideAndMissingAncestorFailClosed(@TempDir Path scratch) throws Exception {
        Path start = Files.createDirectories(scratch.resolve("unrelated/child"));

        IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
                () -> ReleaseProjectRoot.resolve(start, scratch.resolve("unrelated").toString(), Map.of()));
        assertEquals("RELEASE_PROJECT_ROOT_INVALID", invalid.getMessage());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> ReleaseProjectRoot.resolve(start, "", Map.of()));
        assertEquals("RELEASE_PROJECT_ROOT_NOT_FOUND", missing.getMessage());
    }

    private static Path project(Path root) throws Exception {
        Files.createDirectories(root.resolve("release-tools/config"));
        Files.createDirectories(root.resolve("release"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.writeString(root.resolve("release-tools/config/release-platform-rag-policy.json"), "{}");
        Files.writeString(root.resolve("release/index.json"), "{}");
        return root;
    }
}
