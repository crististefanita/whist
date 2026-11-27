package atf.release.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Resolves a checkout from an explicit path, environment override, or bounded ancestor search. */
public final class ReleaseProjectRoot {
    private static final String PROJECT_POM = "pom.xml";
    private static final String RELEASE_POLICY = "release-tools/config/release-platform-rag-policy.json";
    private static final String RELEASE_CATALOG = "release/index.json";

    private ReleaseProjectRoot() { }

    public static Path resolve(Path start, String explicitRoot, Map<String, String> environment) throws IOException {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(environment, "environment");
        String configured = firstNonBlank(explicitRoot, environment.get("ATF_PROJECT_ROOT"));
        if (configured != null) {
            Path requested = Path.of(configured);
            if (!requested.isAbsolute()) requested = start.toAbsolutePath().normalize().resolve(requested);
            return validate(requested);
        }

        Path cursor = start.toAbsolutePath().normalize();
        if (!Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS)) cursor = cursor.getParent();
        while (cursor != null) {
            if (isProjectRoot(cursor)) return validate(cursor);
            cursor = cursor.getParent();
        }
        throw new IllegalArgumentException("RELEASE_PROJECT_ROOT_NOT_FOUND");
    }

    private static String firstNonBlank(String explicit, String environment) {
        if (explicit != null && !explicit.isBlank()) return explicit.trim();
        if (environment != null && !environment.isBlank()) return environment.trim();
        return null;
    }

    private static Path validate(Path candidate) throws IOException {
        Path real = candidate.toRealPath();
        if (!Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS) || !isProjectRoot(real)) {
            throw new IllegalArgumentException("RELEASE_PROJECT_ROOT_INVALID");
        }
        return real;
    }

    private static boolean isProjectRoot(Path candidate) {
        return Files.isRegularFile(candidate.resolve(PROJECT_POM), LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(candidate.resolve(RELEASE_POLICY), LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(candidate.resolve(RELEASE_CATALOG), LinkOption.NOFOLLOW_LINKS);
    }
}
