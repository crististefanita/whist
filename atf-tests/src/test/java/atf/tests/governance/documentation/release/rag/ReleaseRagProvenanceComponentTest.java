package atf.tests.governance.documentation.release.rag;

import atf.testkit.AtfComponentTest;
import atf.android.evidence.component.ComponentInputs;
import atf.tests.reporting.GameReportAudit;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.BASELINE;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.VERSION;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.capturePayload;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.create;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.gate;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hash;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hashText;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.refreshPayload;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.check;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.details;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.evaluate;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.findCheck;

@AtfComponentTest
class ReleaseRagProvenanceComponentTest {
    @TempDir Path temporary;

    @Test @ExtendWith(GameReportAudit.class)
    void documentationDriftReadmissionKeepsTheRejectedAggregateAndOriginalReports(ComponentInputs inputs) throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path component = fixture.candidate().resolve("reports/component");
        Path rejected = RagComponentEvidenceFixture.documentationDrift(component);
        byte[] original = Files.readAllBytes(rejected);
        inputs.add("Original rejected aggregate", new String(original, StandardCharsets.UTF_8));
        inputs.add("Bound snapshot observations", Files.readString(component.resolve("source-snapshot-check.json")));
        String reportHash = hash(component.resolve("atf-validation/component-report.html"));
        refreshPayload(fixture.candidate());
        evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        inputs.add("RAG component-source verdict", findCheck(verdict, "COMPONENT_SOURCE_BINDING").toString());
        AuditedAssertions.assertEquals("PASS", check(verdict, "COMPONENT_SOURCE_BINDING"),
                details(verdict, "COMPONENT_SOURCE_BINDING").toString());
        for (JsonNode group : verdict.path("componentGroups")) {
            AuditedAssertions.assertEquals("FAIL", group.path("sourceEvidence").path("readmission").path("rejectedRunStatus").asText());
        }
        AuditedAssertions.assertEquals(Base64.getEncoder().encodeToString(original),
                Base64.getEncoder().encodeToString(Files.readAllBytes(rejected)), "Evaluation does not rewrite a rejected receipt");
        AuditedAssertions.assertEquals(reportHash, hash(component.resolve("atf-validation/component-report.html")));
        AuditedAssertions.assertEquals("FAIL", JSON.readTree(rejected.toFile()).path("status").asText());
    }

    @Test @ExtendWith(GameReportAudit.class)
    void documentationDriftRequiresBoundObservationsInTheReleasePayload(ComponentInputs inputs) throws Exception {
        for (String mutation : List.of("missing-observation", "changed-observation", "changed-check")) {
            RagCandidateFixture fixture = create(Files.createDirectories(temporary.resolve(mutation)));
            Path component = fixture.candidate().resolve("reports/component");
            RagComponentEvidenceFixture.documentationDrift(component);
            inputs.add("Controlled mutation", mutation);
            inputs.add("Original snapshot-check binding: " + mutation, Files.readString(component.resolve("source-snapshot-check.json")));
            Path observation = component.resolve("observed-live-source.json");
            if (mutation.equals("missing-observation")) Files.delete(observation);
            else if (mutation.equals("changed-observation")) Files.writeString(observation, "[]");
            else Files.writeString(component.resolve("source-snapshot-check.json"), "{}");
            refreshPayload(fixture.candidate());
            evaluate(fixture, fixture.output());
            JsonNode verdict = JSON.readTree(fixture.output().toFile());
            inputs.add("RAG rejection and payload citations: " + mutation, findCheck(verdict, "COMPONENT_SOURCE_BINDING").toString());
            AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"), mutation);
            AuditedAssertions.assertEquals("INVALID", componentDecision(verdict, "atf-validation"), mutation);
        }
    }

    @Test void aPinnedComponentCandidateProducesCitedHumanReviewWithoutChangingInputsOrCallingE2e() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        String candidateBefore = treeHash(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit);
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("NOT_RUN", verdict.path("e2e").path("status").asText());
        AuditedAssertions.assertEquals("NONE", verdict.path("lifecycleEffect").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "MAIN_README_SURFACE"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(verdict.path("checks").findValuesAsText("status").contains("PASS"));
        AuditedAssertions.assertEquals(candidateBefore, treeHash(fixture.candidate()), "Evaluator must not mutate the candidate");
        JsonNode citation = findCheck(verdict, "SOURCE_SNAPSHOT").path("evidence").get(0);
        AuditedAssertions.assertTrue(citation.path("sha256").asText().matches("[0-9a-f]{64}"));
        AuditedAssertions.assertTrue(citation.path("line").asInt() > 0);
        AuditedAssertions.assertEquals("/source", citation.path("jsonPointer").asText());
    }

    @Test void reportSchemaAdmissionPreservesLegacyHtmlAndRejectsXmlModeMissingModernSchemaAndMixedPaths() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        JsonNode legacyHtml = evaluateAndRead(fixture);
        AuditedAssertions.assertEquals("PASS", check(legacyHtml, "REPORT_SCHEMA_ADMISSION"),
                "Qualification schema 2 may omit the new declaration only while every declared report is HTML.");

        ObjectNode manifest = readCandidate(fixture);
        ObjectNode qualification = (ObjectNode) manifest.path("qualification");
        qualification.put("reportSchema", "JUNIT_XML_ONLY");
        writeCandidate(fixture, manifest);
        int legacyXmlExit = evaluate(fixture, fixture.output());
        JsonNode declaredLegacyXml = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, legacyXmlExit);
        AuditedAssertions.assertEquals("BLOCKED", check(declaredLegacyXml, "REPORT_SCHEMA_ADMISSION"));
        AuditedAssertions.assertTrue(details(declaredLegacyXml, "REPORT_SCHEMA_ADMISSION").stream()
                .anyMatch(value -> value.contains("LEGACY_XML_MODE_CANNOT_QUALIFY_EXTENT_HTML")));

        qualification.remove("reportSchema");
        qualification.put("schemaVersion", 3);
        writeCandidate(fixture, manifest);
        JsonNode missingModernDeclaration = evaluateAndRead(fixture);
        AuditedAssertions.assertEquals("BLOCKED", check(missingModernDeclaration, "REPORT_SCHEMA_ADMISSION"));
        AuditedAssertions.assertTrue(details(missingModernDeclaration, "REPORT_SCHEMA_ADMISSION").stream()
                .anyMatch(value -> value.contains("REPORT_SCHEMA_REQUIRED_FOR_QUALIFICATION_SCHEMA:3")));

        qualification.put("reportSchema", "ATF_EXTENT_HTML_INLINE_BASE64_V1");
        ((ObjectNode) qualification.path("auditedGroups").path("whist-lab-app-history"))
                .put("report", "reports/component/whist-lab-app-history/component-report.xml");
        writeCandidate(fixture, manifest);
        JsonNode mixedFormats = evaluateAndRead(fixture);
        AuditedAssertions.assertEquals("BLOCKED", check(mixedFormats, "REPORT_SCHEMA_ADMISSION"));
        AuditedAssertions.assertTrue(details(mixedFormats, "REPORT_SCHEMA_ADMISSION").stream()
                .anyMatch(value -> value.contains("NON_HTML_REPORT:component/whist-lab-app-history")));

        ((ObjectNode) qualification.path("auditedGroups").path("whist-lab-app-history"))
                .put("report", "reports/component/whist-lab-app-history/component-report.html");
        qualification.putObject("e2eGroups").putObject("responsive-flow")
                .put("report", "reports/e2e/responsive-flow/report.xml");
        writeCandidate(fixture, manifest);
        JsonNode e2eXml = evaluateAndRead(fixture);
        AuditedAssertions.assertEquals("BLOCKED", check(e2eXml, "REPORT_SCHEMA_ADMISSION"));
        AuditedAssertions.assertTrue(details(e2eXml, "REPORT_SCHEMA_ADMISSION").stream()
                .anyMatch(value -> value.contains("NON_HTML_REPORT:e2e/responsive-flow")));

        ((ObjectNode) qualification).remove("e2eGroups");
        writeCandidate(fixture, manifest);
        JsonNode canonical = evaluateAndRead(fixture);
        AuditedAssertions.assertEquals("PASS", check(canonical, "REPORT_SCHEMA_ADMISSION"));
    }

    private static ObjectNode readCandidate(RagCandidateFixture fixture) throws Exception {
        return (ObjectNode) JSON.readTree(fixture.candidate().resolve("release-validation.json").toFile());
    }

    private static void writeCandidate(RagCandidateFixture fixture, ObjectNode manifest) throws Exception {
        JSON.writerWithDefaultPrettyPrinter().writeValue(fixture.candidate().resolve("release-validation.json").toFile(), manifest);
    }

    private static JsonNode evaluateAndRead(RagCandidateFixture fixture) throws Exception {
        evaluate(fixture, fixture.output());
        return JSON.readTree(fixture.output().toFile());
    }

    @Test void verdictEmbedsExactPlanBytesAndCitesTheEmbeddedReceipt() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        byte[] originalPlan = Files.readAllBytes(fixture.plan());
        String originalHash = hash(fixture.plan());

        AuditedAssertions.assertEquals(0, evaluate(fixture, fixture.output()));
        String serialized = Files.readString(fixture.output());
        JsonNode verdict = JSON.readTree(serialized);
        JsonNode embeddedPlan = verdict.path("qualificationPlan");
        AuditedAssertions.assertEquals("qualification-plan.json", embeddedPlan.path("fileName").asText());
        AuditedAssertions.assertEquals("base64-utf8", embeddedPlan.path("encoding").asText());
        AuditedAssertions.assertEquals(originalHash, embeddedPlan.path("sha256").asText());
        AuditedAssertions.assertArrayEquals(originalPlan,
                Base64.getDecoder().decode(embeddedPlan.path("contentBase64").asText()));
        AuditedAssertions.assertFalse(serialized.contains(fixture.plan().toString()),
                "The verdict must not depend on the temporary plan path");

        for (String checkCode : List.of("PLAN_SCHEMA", "DIFF_SCOPE", "E2E_SCOPE")) {
            JsonNode citations = findCheck(verdict, checkCode).path("evidence");
            AuditedAssertions.assertTrue(citations.isArray() && citations.size() > 0);
            AuditedAssertions.assertTrue(citations.findValuesAsText("path").contains("$report.qualificationPlan.contentBase64"));
            AuditedAssertions.assertTrue(citations.findValuesAsText("embedded").contains("true"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"whist-lab-app-achievements", "whist-lab-app-atf-card-table-ui"})
    void aCollectionIntroducedForTheCandidateVersionCannotBeOmittedFromItsPlan(String collection) throws Exception {
        RagCandidateFixture fixture = create(temporary);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        var groups = (com.fasterxml.jackson.databind.node.ArrayNode) plan.path("componentGroups");
        for (int index = groups.size() - 1; index >= 0; index--)
            if (groups.get(index).asText().equals(collection)) groups.remove(index);
        JSON.writeValue(fixture.plan().toFile(), plan);

        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream()
                .anyMatch(value -> value.contains(collection)));
    }

    @Test void changedSourceAndArtifactBytesBlockWithSeparateDiagnostics() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Files.writeString(fixture.source().resolve("src/Game.java"), "class Changed {}\n");
        Files.writeString(fixture.candidate().resolve("WhistLab-" + VERSION + ".apk"), "tampered apk");
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PAYLOAD_INTEGRITY"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "APK_IDENTITY"));
        AuditedAssertions.assertTrue(details(verdict, "SOURCE_SNAPSHOT").stream().anyMatch(value -> value.contains("src/Game.java") && value.contains("Hash diferit")));
        AuditedAssertions.assertTrue(details(verdict, "PAYLOAD_INTEGRITY").stream().anyMatch(value -> value.contains("PAYLOAD_MISMATCH")));
    }

    @Test void anApkDigestInThePinnedMainReadmeBlocksRagWithoutMaskingTheSourceCheck() throws Exception {
        RagCandidateFixture fixture = create(temporary, "# Whist Lab\nSHA-256: " + "a".repeat(64) + "\n");
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "MAIN_README_SURFACE"));
        AuditedAssertions.assertTrue(details(verdict, "MAIN_README_SURFACE").stream().anyMatch(value -> value.contains("linia 2")));
    }

    @Test void aMalformedReleaseVersionPatternBlocksPolicyWithoutCrashingTheEvaluator() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path policyFile = fixture.project().resolve("config/release-platform-rag-policy.json");
        ObjectNode policy = (ObjectNode) JSON.readTree(policyFile.toFile());
        ((ObjectNode) policy.path("releaseVersioning")).put("newReleasePattern", "[");
        JSON.writeValue(policyFile.toFile(), policy);

        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "RAG_POLICY"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
    }

    @Test void componentReportsFromAnotherSourceInventoryCannotQualifyTheApk() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path inputs = fixture.candidate().resolve("reports/component/source-inputs.json");
        var componentRows = (com.fasterxml.jackson.databind.node.ArrayNode) JSON.readTree(inputs.toFile());
        for (JsonNode row : componentRows) {
            if (row.path("path").asText().equals("src/Game.java")) { ((ObjectNode) row).put("sha256", "b".repeat(64)); break; }
        }
        JSON.writeValue(inputs.toFile(), componentRows); refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
        AuditedAssertions.assertEquals("INVALID", componentDecision(verdict, "atf-validation"));
    }

    @Test void reusesAnOriginalReceiptOnlyOutsideItsSuiteInputs() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        String added = "whist-lab-app/app/src/main/java/ro/whistlab/app/screens/Screen.java";
        Path file = fixture.source().resolve(added);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Screen {}\n");
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        var manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        Path inputsFile = fixture.candidate().resolve("reports/apk-inputs.json");
        var inputs = (ObjectNode) JSON.readTree(inputsFile.toFile());
        var rows = (com.fasterxml.jackson.databind.node.ArrayNode) inputs.path("files");
        rows.addObject().put("path", added).put("sha256", hash(file));
        var contents = new StringBuilder();
        rows.forEach(row -> contents.append(row.path("path").asText()).append('\t')
                .append(row.path("sha256").asText()).append('\n'));
        String fingerprint = hashText(contents.toString());
        inputs.put("fingerprint", fingerprint);
        ((ObjectNode) manifest.path("source")).put("workingTreeFingerprint", fingerprint);
        JSON.writeValue(inputsFile.toFile(), inputs);

        Path component = fixture.candidate().resolve("reports/component");
        Path original = Files.createDirectories(component.resolve("snapshots/original"));
        for (String name : List.of("source-inputs.json", "source-provenance.json", "source-component-run.json", "release-inputs.json")) {
            Files.copy(component.resolve(name), original.resolve(name));
        }
        Path originalRunFile = original.resolve("source-component-run.json");
        var originalRun = (ObjectNode) JSON.readTree(originalRunFile.toFile());
        originalRun.put("status", "FAIL");
        originalRun.path("results").forEach(row -> {
            if (row.path("module").asText().equals("atf-tests")) ((ObjectNode) row).put("exitCode", 1);
        });
        JSON.writeValue(originalRunFile.toFile(), originalRun);
        var group = (ObjectNode) manifest.at("/qualification/auditedGroups/atf-validation");
        group.put("componentEvidenceOrigin", "original");
        capturePayload(fixture.candidate(), manifest);
        JSON.writeValue(manifestFile.toFile(), manifest);

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("REUSE", componentDecision(verdict, "atf-validation"));
        AuditedAssertions.assertEquals("REUSE", componentDecision(verdict, "atf-test-android"));
        AuditedAssertions.assertEquals("RUN", componentDecision(verdict, "whist-lab-app-display"));
        AuditedAssertions.assertEquals("RUN", componentDecision(verdict, "atf-tests"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_SOURCE_BINDING").stream()
                .anyMatch(value -> value.contains("APK-source-only: " + added)));
    }

    @Test void preparedOriginalReceiptsSatisfyEveryComponentGateWithoutANewRun() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path component = fixture.candidate().resolve("reports/component");
        Path prepared = Files.createDirectory(temporary.resolve("prepared"));
        Files.copy(component.resolve("source-inputs.json"), prepared.resolve("source-inputs.json"));
        Files.copy(component.resolve("release-inputs.json"), prepared.resolve("release-inputs.json"));
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        var manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        var groups = (ObjectNode) manifest.at("/qualification/auditedGroups");
        Path root = GovernanceProject.locateProjectRoot();
        var command = new java.util.ArrayList<>(List.of("node",
                root.resolve("atf-tests/tools/prepare-component-reuse.mjs").toString(), prepared.toString(), component.toString()));
        groups.fieldNames().forEachRemaining(command::add);
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Component reuse preparation exceeded 45 seconds");
        }
        AuditedAssertions.assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        var plan = JSON.readTree(prepared.resolve("component-reuse.json").toFile());
        String id = plan.path("decisions").get(0).path("componentEvidenceOrigin").asText();
        for (var row : plan.path("decisions")) {
            AuditedAssertions.assertEquals("MATCH", row.path("decision").asText());
            ((ObjectNode) groups.path(row.path("group").asText())).put("componentEvidenceOrigin", id);
        }
        Files.move(prepared.resolve("snapshots"), component.resolve("snapshots"));
        Files.delete(component.resolve("source-component-run.json"));
        ((ObjectNode) manifest.at("/qualification/platforms/android/gates")).set("androidComponents",
                gate("reports/component/snapshots/" + id + "/source-component-run.json"));
        capturePayload(fixture.candidate(), manifest);
        JSON.writeValue(manifestFile.toFile(), manifest);

        AuditedAssertions.assertEquals(0, evaluate(fixture, fixture.output()));
        var verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PLATFORM_GATES"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "COMPONENT_SOURCE_BINDING"));
    }

    @Test void aNewCandidateCannotReuseGovernanceFromADifferentReleaseInventory() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path added = fixture.project().resolve("release/added.json");
        Files.writeString(added, "{}");
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
        AuditedAssertions.assertEquals("INVALID", componentDecision(verdict, "atf-tests"));
        AuditedAssertions.assertEquals("MATCH", componentDecision(verdict, "atf-validation"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_SOURCE_BINDING").stream()
                .anyMatch(value -> value.contains("COMPONENT_RELEASE_SNAPSHOT_CHANGED") && value.contains("release/added.json")));
    }

    @Test void aMissingApplicationExtentGroupCannotBeReplacedByTheGovernanceSuite() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("qualification").path("auditedGroups")).remove("whist-lab-app-history");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream().anyMatch(value -> value.contains("whist-lab-app-history")));
    }

    @Test void provenanceReceiptMustBindTheSameComponentManifestAndAggregateRun() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path file = fixture.candidate().resolve("reports/component/source-provenance.json");
        ObjectNode provenance = (ObjectNode) JSON.readTree(file.toFile());
        provenance.put("sourceFingerprint", "b".repeat(64));
        JSON.writeValue(file.toFile(), provenance);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_SOURCE_BINDING").stream()
                .anyMatch(value -> value.contains("proveniență") && value.contains("manifestul")));
    }

    @Test void componentSelectionsCannotReuseOneCanonicalExtentForTwoSuites() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        String sharedReport = manifest.path("qualification").path("auditedGroups").path("atf-tests").path("report").asText();
        ((ObjectNode) manifest.path("qualification").path("auditedGroups").path("whist-lab-app-history")).put("report", sharedReport);
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream().anyMatch(value -> value.contains("cale Extent canonică/unică")));
    }

    @Test void evaluatorRefusesToWriteItsVerdictInsideAnyInputTree() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path forbidden = fixture.candidate().resolve("rag-verdict.json");
        AssertionError refusal = AuditedAssertions.assertThrows(AssertionError.class, () -> evaluate(fixture, forbidden));
        AuditedAssertions.assertTrue(refusal.getMessage().contains("OUTPUT_MUST_BE_OUTSIDE_INPUTS"));
        AuditedAssertions.assertFalse(Files.exists(forbidden));
    }

    @Test void docsOnlyRequiresGovernanceAndKeepsE2eNotRun() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        AuditedAssertions.assertEquals(0, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        JsonNode scope = verdict.path("testScope");
        AuditedAssertions.assertEquals("PASS", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals("DOCUMENTATION_ONLY", scope.path("kind").asText());
        AuditedAssertions.assertEquals(1, scope.path("runComponentGroups").size());
        AuditedAssertions.assertEquals("atf-tests", scope.path("runComponentGroups").get(0).asText());
        AuditedAssertions.assertEquals(0, scope.path("requiredE2eGroups").size());
        AuditedAssertions.assertEquals("NOT_RUN", verdict.path("e2e").path("status").asText());
    }

    @Test void refusesAPlanThatSubstitutesItsOwnBaselineOrOmitsSnapshotFiles() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        ((ObjectNode) plan.path("scope")).put("baselineVersion", "1.0.0");
        JSON.writeValue(fixture.plan().toFile(), plan);
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals(BASELINE, verdict.path("testScope").path("scope").path("baselineVersion").asText());

        Files.writeString(fixture.source().resolve("src/Unlisted.java"), "class Unlisted {}\n");
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertTrue(details(verdict, "SOURCE_SNAPSHOT").stream().anyMatch(value -> value.contains("src/Unlisted.java")));
    }

    private static String componentDecision(JsonNode verdict, String group) {
        for (JsonNode entry : verdict.path("componentGroups")) {
            if (group.equals(entry.path("name").asText())) return entry.path("sourceEvidence").path("decision").asText();
        }
        throw new IllegalArgumentException("Missing group " + group);
    }

    private static String treeHash(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            String input = files.filter(Files::isRegularFile).sorted().map(file -> root.relativize(file).toString() + "\t" + hashUnchecked(file) + "\n").reduce("", String::concat);
            return hashText(input);
        }
    }
    private static String hashUnchecked(Path file) { try { return hash(file); } catch (Exception error) { throw new RuntimeException(error); } }
}
