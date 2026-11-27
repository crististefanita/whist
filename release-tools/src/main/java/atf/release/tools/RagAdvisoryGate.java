package atf.release.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Admits the read-only RAG verdict for review; it never promotes, signs, or publishes. */
public final class RagAdvisoryGate {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern VERSION = Pattern.compile("0\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)");

    public Assessment verify(Path projectDirectory, Path candidateDirectory, Path sourceDirectory, Path verdictFile) throws IOException {
        Path project = projectDirectory.toRealPath();
        Path candidate = candidateDirectory.toRealPath();
        Path source = sourceDirectory.toRealPath();
        Path verdict = verdictFile.toAbsolutePath().normalize();
        Path realVerdict = verdict.toRealPath();
        if (!Files.isRegularFile(realVerdict, LinkOption.NOFOLLOW_LINKS)
                || inside(project, realVerdict) || inside(candidate, realVerdict) || inside(source, realVerdict)) {
            throw new IllegalArgumentException("RAG_VERDICT_OUTPUT_BOUNDARY");
        }
        JsonNode report = JSON.readTree(Files.readAllBytes(realVerdict));
        if (report.path("schemaVersion").asInt(-1) != 1 || !"ATF_RELEASE_RAG_VERDICT".equals(text(report, "kind"))) {
            throw new IllegalArgumentException("RAG_VERDICT_SCHEMA");
        }
        String status = text(report, "verdict");
        if (!status.equals("READY_FOR_HUMAN_REVIEW") && !status.equals("BLOCKED")) {
            throw new IllegalArgumentException("RAG_VERDICT_STATUS");
        }
        if (!"NONE".equals(text(report, "lifecycleEffect"))) throw new IllegalArgumentException("RAG_VERDICT_SIDE_EFFECT");
        rejectCredentialFields(report);
        verifyCandidateBinding(report, candidate);
        verifyPlanSnapshot(report);
        verifyChecks(report, status);

        String expectedOutput = project.relativize(realVerdict).toString().replace('\\', '/');
        if (!expectedOutput.equals(text(report, "writesOnly"))) throw new IllegalArgumentException("RAG_VERDICT_OUTPUT_IDENTITY");
        return new Assessment(status, text(report, "releaseVersion"), digest(realVerdict),
                text(report, "candidateManifestSha256"), report.path("checks").size());
    }

    private static void verifyCandidateBinding(JsonNode report, Path candidate) throws IOException {
        Path manifestPath = candidate.resolve("release-validation.json");
        if (Files.isSymbolicLink(manifestPath) || !Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("RAG_CANDIDATE_MANIFEST_MISSING");
        }
        JsonNode manifest = JSON.readTree(Files.readAllBytes(manifestPath));
        String version = text(report, "releaseVersion");
        if (!VERSION.matcher(version).matches() || !version.equals(text(manifest.path("release"), "versionName"))
                || !digest(manifestPath).equals(text(report, "candidateManifestSha256"))
                || !text(manifest.path("source"), "sourceCommit").equals(text(report, "sourceCommit"))
                || !text(manifest.path("source"), "workingTreeFingerprint").equals(text(report, "workingTreeFingerprint"))) {
            throw new IllegalArgumentException("RAG_CANDIDATE_IDENTITY_MISMATCH");
        }
    }

    private static void verifyPlanSnapshot(JsonNode report) {
        JsonNode snapshot = report.path("qualificationPlan");
        if (!"base64-utf8".equals(text(snapshot, "encoding"))) throw new IllegalArgumentException("RAG_PLAN_SNAPSHOT_ENCODING");
        String expected = text(snapshot, "sha256");
        if (!SHA_256.matcher(expected).matches() || !expected.equals(text(report, "planSha256"))) {
            throw new IllegalArgumentException("RAG_PLAN_SNAPSHOT_DIGEST");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(text(snapshot, "contentBase64")); }
        catch (IllegalArgumentException invalidBase64) { throw new IllegalArgumentException("RAG_PLAN_SNAPSHOT_ENCODING", invalidBase64); }
        if (!sha256(bytes).equals(expected)) throw new IllegalArgumentException("RAG_PLAN_SNAPSHOT_DIGEST");
    }

    private static void verifyChecks(JsonNode report, String status) {
        JsonNode checks = report.path("checks");
        if (!checks.isArray() || checks.isEmpty()) throw new IllegalArgumentException("RAG_CHECKS_MISSING");
        boolean blocked = false;
        for (JsonNode check : checks) {
            String checkStatus = text(check, "status");
            if (!checkStatus.equals("PASS") && !checkStatus.equals("BLOCKED")) {
                throw new IllegalArgumentException("RAG_CHECK_STATUS_INVALID");
            }
            blocked |= checkStatus.equals("BLOCKED");
        }
        if (status.equals("READY_FOR_HUMAN_REVIEW") && blocked || status.equals("BLOCKED") && !blocked) {
            throw new IllegalArgumentException("RAG_AGGREGATE_STATUS_MISMATCH");
        }
    }

    private static void rejectCredentialFields(JsonNode value) {
        if (value.isObject()) {
            for (Map.Entry<String, JsonNode> entry : iterable(value.fields())) {
                String name = entry.getKey().toLowerCase(Locale.ROOT);
                if (name.contains("token") || name.contains("password") || name.contains("credential") || name.equals("secret")) {
                    throw new IllegalArgumentException("RAG_VERDICT_CREDENTIAL_FIELD");
                }
                rejectCredentialFields(entry.getValue());
            }
        } else if (value.isArray()) {
            value.forEach(RagAdvisoryGate::rejectCredentialFields);
        }
    }

    private static <T> Iterable<T> iterable(Iterator<T> iterator) { return () -> iterator; }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("RAG_VERDICT_FIELD:" + field);
        return value.asText();
    }

    private static boolean inside(Path root, Path file) {
        return file.equals(root) || file.startsWith(root);
    }

    private static String digest(Path file) throws IOException {
        return sha256(Files.readAllBytes(file));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public record Assessment(String verdict, String releaseVersion, String verdictSha256,
                             String candidateManifestSha256, int checks) {
        public boolean humanReviewRequired() { return true; }
        public boolean mayPromote() { return false; }
        public boolean maySign() { return false; }
        public boolean mayPublish() { return false; }
    }
}
