package atf.release.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import atf.testkit.assertions.AuditedAssertions;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import static atf.testkit.assertions.AuditedAssertions.*;

@Tag("component")
@AtfComponentTest
class RagAdvisoryGateComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String VERSION = "0.26.0";
    private static final String COMMIT = "a".repeat(40);
    private static final String FINGERPRINT = "b".repeat(64);

    @Test void admitsOnlyAHashBoundReadOnlyReviewRecommendation(@TempDir Path scratch) throws Exception {
        Fixture fixture = fixture(scratch, "READY_FOR_HUMAN_REVIEW", "NONE");
        byte[] manifestBefore = Files.readAllBytes(fixture.candidate.resolve("release-validation.json"));
        byte[] sourceBefore = Files.readAllBytes(fixture.source.resolve("source.txt"));

        RagAdvisoryGate.Assessment assessment = new RagAdvisoryGate().verify(
                fixture.project, fixture.candidate, fixture.source, fixture.verdict);

        assertEquals("READY_FOR_HUMAN_REVIEW", assessment.verdict());
        assertEquals(VERSION, assessment.releaseVersion());
        assertTrue(assessment.humanReviewRequired());
        assertFalse(assessment.mayPromote());
        assertFalse(assessment.maySign());
        assertFalse(assessment.mayPublish());
        assertArrayEquals(manifestBefore, Files.readAllBytes(fixture.candidate.resolve("release-validation.json")));
        assertArrayEquals(sourceBefore, Files.readAllBytes(fixture.source.resolve("source.txt")));
    }

    @Test void blocksPromotionStateWrongHashesAndVerdictsInsideInputs(@TempDir Path scratch) throws Exception {
        Fixture promoted = fixture(scratch.resolve("promoted"), "PUBLISHED", "PUBLISHED");
        assertEquals("RAG_VERDICT_STATUS", assertThrows(IllegalArgumentException.class,
                () -> new RagAdvisoryGate().verify(promoted.project, promoted.candidate, promoted.source, promoted.verdict)).getMessage());

        Fixture sideEffect = fixture(scratch.resolve("side-effect"), "READY_FOR_HUMAN_REVIEW", "PROMOTED_TO_MAIN");
        assertEquals("RAG_VERDICT_SIDE_EFFECT", assertThrows(IllegalArgumentException.class,
                () -> new RagAdvisoryGate().verify(sideEffect.project, sideEffect.candidate, sideEffect.source, sideEffect.verdict)).getMessage());

        Fixture nestedOutput = fixture(scratch.resolve("nested-output"), "READY_FOR_HUMAN_REVIEW", "NONE");
        Path movedInside = nestedOutput.candidate.resolve("verdict.json");
        Files.move(nestedOutput.verdict, movedInside);
        assertEquals("RAG_VERDICT_OUTPUT_BOUNDARY", assertThrows(IllegalArgumentException.class,
                () -> new RagAdvisoryGate().verify(nestedOutput.project, nestedOutput.candidate, nestedOutput.source, movedInside)).getMessage());
    }

    private static Fixture fixture(Path base, String verdictStatus, String lifecycleEffect) throws Exception {
        Path project = Files.createDirectories(base.resolve("project"));
        Path candidate = Files.createDirectories(base.resolve("candidate"));
        Path source = Files.createDirectories(base.resolve("source"));
        Files.createDirectories(project.resolve("release-tools/config"));
        Files.createDirectories(project.resolve("release"));
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        Files.writeString(project.resolve("release-tools/config/release-platform-rag-policy.json"), "{}");
        Files.writeString(project.resolve("release/index.json"), "{}");
        Files.writeString(source.resolve("source.txt"), "pinned source");

        ObjectNode manifest = JSON.createObjectNode();
        ObjectNode release = manifest.putObject("release");
        release.put("versionName", VERSION);
        ObjectNode sourceIdentity = manifest.putObject("source");
        sourceIdentity.put("sourceCommit", COMMIT);
        sourceIdentity.put("workingTreeFingerprint", FINGERPRINT);
        Path manifestPath = candidate.resolve("release-validation.json");
        byte[] manifestBytes = JSON.writeValueAsBytes(manifest);
        Files.write(manifestPath, manifestBytes);

        byte[] plan = "{\"releaseVersion\":\"0.26.0\"}".getBytes(StandardCharsets.UTF_8);
        String planHash = sha256(plan);
        ObjectNode report = JSON.createObjectNode();
        report.put("schemaVersion", 1);
        report.put("kind", "ATF_RELEASE_RAG_VERDICT");
        report.put("verdict", verdictStatus);
        report.put("releaseVersion", VERSION);
        report.put("sourceCommit", COMMIT);
        report.put("workingTreeFingerprint", FINGERPRINT);
        report.put("candidateManifestSha256", sha256(manifestBytes));
        report.put("planSha256", planHash);
        ObjectNode planSnapshot = report.putObject("qualificationPlan");
        planSnapshot.put("encoding", "base64-utf8");
        planSnapshot.put("sha256", planHash);
        planSnapshot.put("contentBase64", Base64.getEncoder().encodeToString(plan));
        ArrayNode checks = report.putArray("checks");
        checks.addObject().put("code", "SOURCE_SNAPSHOT").put("status", verdictStatus.equals("BLOCKED") ? "BLOCKED" : "PASS");
        report.put("lifecycleEffect", lifecycleEffect);
        report.put("writesOnly", "../verdict.json");
        Path verdict = base.resolve("verdict.json");
        Files.write(verdict, JSON.writeValueAsBytes(report));
        return new Fixture(project, candidate, source, verdict);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record Fixture(Path project, Path candidate, Path source, Path verdict) { }
}
