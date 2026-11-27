package atf.release.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Independently checks the byte inventory cited by a candidate release manifest. */
public final class ReleaseEvidenceGate {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public Result verify(Path candidateDirectory, Path manifestPath) throws IOException {
        Path root = candidateDirectory.toRealPath();
        Path manifest = manifestPath.isAbsolute() ? manifestPath : root.resolve(manifestPath);
        manifest = resolveFile(root, root.relativize(manifest.normalize()));
        byte[] manifestBytes = Files.readAllBytes(manifest);
        JsonNode document = JSON.readTree(manifestBytes);
        JsonNode inventory = document.at("/integrity/files");
        if (!inventory.isArray() || inventory.isEmpty()) throw new IllegalArgumentException("RELEASE_EVIDENCE_INVENTORY_MISSING");

        Set<String> seen = new HashSet<>();
        for (JsonNode row : inventory) {
            String relative = requiredText(row, "path");
            long expectedBytes = requiredLong(row, "bytes");
            String expectedHash = requiredText(row, "sha256");
            if (!SHA_256.matcher(expectedHash).matches() || expectedBytes < 0 || !seen.add(relative)) {
                throw new IllegalArgumentException("RELEASE_EVIDENCE_RECEIPT_INVALID");
            }
            Path file = resolveFile(root, safeRelative(relative));
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length != expectedBytes || !sha256(bytes).equals(expectedHash)) {
                throw new IllegalArgumentException("RELEASE_EVIDENCE_BYTES_OR_HASH:" + relative);
            }
        }
        return new Result(seen.size(), sha256(manifestBytes));
    }

    private static String requiredText(JsonNode row, String property) {
        JsonNode value = row.path(property);
        if (!value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("RELEASE_EVIDENCE_RECEIPT_INVALID");
        return value.asText();
    }

    private static long requiredLong(JsonNode row, String property) {
        JsonNode value = row.path(property);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) throw new IllegalArgumentException("RELEASE_EVIDENCE_RECEIPT_INVALID");
        return value.asLong();
    }

    private static Path safeRelative(String value) {
        if (value.indexOf('\\') >= 0 || value.indexOf(':') >= 0 || value.startsWith("/")) {
            throw new IllegalArgumentException("RELEASE_EVIDENCE_PATH_INVALID");
        }
        Path relative = Path.of(value);
        if (relative.isAbsolute() || relative.getNameCount() == 0) throw new IllegalArgumentException("RELEASE_EVIDENCE_PATH_INVALID");
        for (Path segment : relative) {
            String name = segment.toString();
            if (name.isBlank() || name.equals(".") || name.equals("..")) throw new IllegalArgumentException("RELEASE_EVIDENCE_PATH_INVALID");
        }
        String normalized = relative.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.contains(".atf-signing/") || normalized.endsWith(".keystore") || normalized.endsWith(".jks")
                || normalized.endsWith(".p12") || normalized.endsWith(".pfx") || normalized.contains("/private-key")) {
            throw new IllegalArgumentException("RELEASE_EVIDENCE_PRIVATE_SIGNING_PATH");
        }
        return relative;
    }

    private static Path resolveFile(Path root, Path relative) throws IOException {
        Path safe = safeRelative(relative.toString().replace('\\', '/'));
        Path current = root;
        for (Path segment : safe) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("RELEASE_EVIDENCE_LINK_REFUSED");
        }
        Path normalized = current.normalize();
        if (!normalized.startsWith(root) || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("RELEASE_EVIDENCE_FILE_MISSING");
        }
        return normalized;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public record Result(int filesVerified, String manifestSha256) { }
}
