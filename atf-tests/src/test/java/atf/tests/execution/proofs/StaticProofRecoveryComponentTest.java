package atf.tests.execution.proofs;

import atf.android.evidence.component.ComponentInputs;
import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.tests.execution.recovery.InvocationRecovery;
import atf.tests.reporting.GameReportAudit;
import atf.tests.reporting.GameReportTextAudit;
import atf.tests.reporting.aggregate.ComponentReportCoverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class StaticProofRecoveryComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> IDENTITY = List.of("testChangeReview", "testChangeChecks", "testChangeCompiler");
    @TempDir Path root;

    @Test void failedProofRetriesOnlyItsFailedBodyAndRetainsOriginalComparisons(ComponentInputs inputs) throws Exception {
        prepare();
        var first = fork("initial", null);
        assertNotEquals(0, first.exitCode(), "The original controlled proof must actually fail");
        byte[] original = Files.readAllBytes(root.resolve("initial/component-report.html"));
        Files.writeString(root.resolve("allow-pass"), "ready");
        var next = fork("retry", "initial");
        inputs.add("Original and retry diagnostics", output(first) + "\n" + output(next));
        assertEquals(0, next.exitCode(), "Only the failed static body is retried");
        assertEquals(1, count("preserved"));
        assertEquals(2, count("resumable"));
        assertEquals(1, count("pending"));
        assertEquals(ComponentCaptureJournal.sha256(original),
                ComponentCaptureJournal.sha256(Files.readAllBytes(root.resolve("initial/component-report.html"))),
                "The original failure report remains byte-identical without duplicating its HTML in assertion inputs");
        var receipt = JSON.readTree(root.resolve("retry/execution.json").toFile());
        assertEquals(2, receipt.at("/recovery/reusedInvocationIds").size());
        assertEquals(1, receipt.at("/recovery/executedInvocationIds").size());
        var rows = JSON.convertValue(receipt.path("invocations"), new com.fasterxml.jackson.core.type.TypeReference<List<ComponentReportCoverage.Invocation>>() { });
        String html = Files.readString(root.resolve("retry/component-report.html"));
        assertEquals(List.of(), ComponentReportCoverage.violations(rows, html));
        assertTrue(new GameReportTextAudit(html).contains("original comparison"));
        inputs.add("Reconciled receipt", receipt.toPrettyString());
    }

    @Test void interruptionResumesOnlyMissingBodiesFromTheKnownStaticPopulation(ComponentInputs inputs) throws Exception {
        prepare();
        Files.writeString(root.resolve("halt"), "controlled interruption");
        var first = fork("initial", null);
        assertEquals(23, first.exitCode(), "The child process stops before original report publication");
        assertFalse(Files.exists(root.resolve("initial/component-report.html")));
        Files.writeString(root.resolve("allow-pass"), "ready");
        var next = fork("retry", "initial");
        inputs.add("Interrupted and resumed diagnostics", output(first) + "\n" + output(next));
        assertEquals(0, next.exitCode());
        assertEquals(1, count("preserved"), "Completed body is not replayed");
        assertEquals(2, count("resumable"), "Only the interrupted body starts again");
        assertEquals(1, count("pending"), "The unseen static body starts once");
        var receipt = JSON.readTree(root.resolve("retry/execution.json").toFile());
        assertEquals(3, receipt.path("invocations").size());
        assertEquals(1, receipt.at("/recovery/reusedInvocationIds").size());
        assertEquals(2, receipt.at("/recovery/executedInvocationIds").size());
    }

    @Test void unresolvedRootCauseRemainsAFailedProof(ComponentInputs inputs) throws Exception {
        prepare();
        assertNotEquals(0, fork("initial", null).exitCode());
        var next = fork("retry", "initial");
        inputs.add("Unresolved retry diagnostic", output(next));
        assertNotEquals(0, next.exitCode(), "Recovery never converts an unresolved failed assertion into PASS");
        assertEquals(1, count("preserved"));
        assertEquals(2, count("resumable"));
        assertEquals(1, count("pending"));
        assertFalse(Files.exists(root.resolve("retry/proof.pending.json")));
        assertTrue(Files.readString(root.resolve("retry/execution.json")).contains("FAIL"));
    }

    @Test void foreignReviewSelectionCompilerOrToolchainCannotReuseAProof(ComponentInputs inputs) throws Exception {
        prepare();
        assertNotEquals(0, fork("initial", null).exitCode());
        byte[] original = Files.readAllBytes(root.resolve("provenance.json"));
        for (String field : List.of("sourceFingerprint", "releaseFingerprint", "qualificationContext",
                "testChangeReview", "testChangeChecks", "testChangeCompiler")) {
            var changed = (ObjectNode) JSON.readTree(original);
            changed.put(field, "foreign identity");
            var error = assertThrows(IOException.class, () -> admit(JSON.writeValueAsBytes(changed)));
            assertTrue(error.getMessage().contains("SOURCE"), field);
        }
        for (String field : IDENTITY) {
            var changed = (ObjectNode) JSON.readTree(original);
            changed.remove(field);
            assertThrows(IOException.class, () -> admit(JSON.writeValueAsBytes(changed)), "Missing identity: " + field);
        }
        assertEquals(1, count("resumable"), "Admission failures execute no test bodies");
        inputs.add("Required proof identity", "Same source, catalog, control ID, base, commit, diff, positive/negative selectors, compiler bytes, classpath, JDK and tooling; missing or foreign fields reject reuse before discovery");
    }

    @Test void alteredCapturesAndOriginalReportsCannotAuthorizeReuse(ComponentInputs inputs) throws Exception {
        prepare();
        assertNotEquals(0, fork("initial", null).exitCode());
        byte[] provenance = Files.readAllBytes(root.resolve("provenance.json"));
        Path report = root.resolve("initial/component-report.html");
        byte[] original = Files.readAllBytes(report);
        Files.writeString(report, "altered report");
        assertThrows(IOException.class, () -> admit(provenance));
        Files.write(report, original);
        var checkpoint = JSON.readTree(root.resolve("initial/invocation-checkpoint.json").toFile());
        String id = checkpoint.path("invocations").get(0).path("id").asText();
        Path capture = root.resolve("initial/captures").resolve(ComponentCaptureJournal.name(id));
        Files.writeString(capture, "altered original comparison");
        assertThrows(IOException.class, () -> admit(provenance));
        assertEquals(1, count("preserved"));
        inputs.add("Tamper boundaries", "Original report hash and captured comparison bytes are checked independently; rejected admission cannot run bodies");
    }

    private InvocationRecovery.Plan admit(byte[] provenance) throws IOException {
        return InvocationRecovery.admit(root.resolve("initial"), "testChanges:" + ProofRecoveryFixture.CONTROL,
                provenance, root.resolve("initial/" + StaticProofRun.PROVENANCE), IDENTITY);
    }

    private void prepare() throws Exception {
        var context = JSON.createObjectNode();
        context.put("sourceFingerprint", "a".repeat(64));
        context.put("releaseFingerprint", "b".repeat(64));
        context.putObject("qualificationContext").put("identitySha256", "c".repeat(64));
        context.put("inputPartCharacters", 16384);
        context.putObject("testChangeReview").put("id", ProofRecoveryFixture.CONTROL).put("base", "d".repeat(40))
                .put("commit", "e".repeat(40)).put("diffSha256", "f".repeat(64));
        context.putObject("testChangeChecks").putPOJO("positive", ProofRecoveryFixture.selectors().subList(0, 2))
                .putPOJO("negative", ProofRecoveryFixture.selectors().subList(2, 3));
        context.putObject("testChangeCompiler").put("buildFingerprint", "1".repeat(64)).put("toolFingerprint", "2".repeat(64));
        Files.write(root.resolve("provenance.json"), JSON.writeValueAsBytes(context));
    }

    private CommandProcess.Output fork(String attempt, String previous) throws Exception {
        var command = new java.util.ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "-Xmx256m", "-XX:ActiveProcessorCount=2", "-cp", System.getProperty("java.class.path"),
                ProofRecoveryFixture.class.getName(), root.toString(), attempt));
        if (previous != null) command.add(previous);
        return CommandProcess.execute(command, Map.of(), Duration.ofSeconds(60), false);
    }

    private long count(String name) throws Exception { return Files.readAllLines(root.resolve(name + ".count")).size(); }
    private static String output(CommandProcess.Output value) {
        return new String(value.stdout(), StandardCharsets.UTF_8) + value.stderr();
    }
}
