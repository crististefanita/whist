package atf.release.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/** Checks that a local signing input and the produced APK still identify the preserved public signer. */
public final class SigningIdentityGate {
    private static final String IDENTITY_DIRECTORY = "release-tools/.atf-signing";
    private static final String KEY_FILE = "whist-android-test.keystore";
    private static final String FINGERPRINT_FILE = "whist-android-test.fingerprint";
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public Result verify(Path projectRoot, String observedCertificateSha256) throws IOException {
        Path root = projectRoot.toRealPath();
        Path identity = root.resolve(IDENTITY_DIRECTORY);
        Path key = identity.resolve(KEY_FILE);
        Path fingerprint = identity.resolve(FINGERPRINT_FILE);
        if (Files.isSymbolicLink(root.resolve("release-tools")) || Files.isSymbolicLink(identity)
                || Files.isSymbolicLink(key) || !Files.isRegularFile(key, LinkOption.NOFOLLOW_LINKS)
                || Files.size(key) == 0) throw new IllegalArgumentException("SIGNING_KEY_CONTINUITY_MISSING_OR_INVALID");
        if (Files.isSymbolicLink(fingerprint) || !Files.isRegularFile(fingerprint, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("SIGNING_FINGERPRINT_MISSING_OR_INVALID");
        }
        String expected = Files.readString(fingerprint).trim().toLowerCase(Locale.ROOT);
        String observed = observedCertificateSha256 == null ? "" : observedCertificateSha256.trim().toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(expected).matches() || !SHA_256.matcher(observed).matches()) {
            throw new IllegalArgumentException("SIGNING_CERTIFICATE_FINGERPRINT_INVALID");
        }
        if (!expected.equals(observed)) throw new IllegalArgumentException("APK_CERTIFICATE_CONTINUITY_MISMATCH");
        return new Result(true);
    }

    public record Result(boolean certificateContinuity) { }
}
