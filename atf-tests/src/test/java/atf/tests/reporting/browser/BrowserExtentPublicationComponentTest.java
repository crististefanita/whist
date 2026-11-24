package atf.tests.reporting.browser;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class BrowserExtentPublicationComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    private static URLClassLoader publisher;
    @TempDir Path root;
    @BeforeAll static void compilePublisher(@TempDir Path classes) throws Exception { publisher = BrowserReportFixture.compile(classes); }
    @AfterAll static void closePublisher() throws Exception { if (publisher != null) publisher.close(); }

    @Test void standardOverviewKeepsEnglishNarrativeAndScenarioOwnedCaptures(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root);
        inputs.add("Publisher fixture", "Three synthetic cases: PASS and FAIL, three distinct PNGs, long text and JSON with a large integer.");
        BrowserReportFixture.publish(publisher, root);
        String html = Files.readString(root.resolve("report.html"));
        Document doc = Jsoup.parse(html);
        Element overview = doc.selectFirst("#dashboard-view");
        assertNotNull(overview, "Standard Overview remains available");
        assertTrue(overview.children().first().hasClass("row"), "Overview begins with the native summary cards");
        assertTrue(overview.text().contains("Started"), "Native Overview keeps English summary captions");
        assertEquals(0, overview.select("#atf-ui-evidence, img, a[href^=data:image]").size(), "No custom capture gallery in Overview");
        assertFalse(overview.text().contains("First captured frame") || overview.text().contains("Last captured frame"),
                "Capture captions belong to their tests, independently of native Overview disclosures");
        assertFalse(html.contains("Playwright exploratory evidence"), "The global exploratory gallery is not injected");
        assertTrue(doc.text().contains("Browser components"), "Browser suite is named in English");
        assertTrue(doc.text().contains("Android components"), "Native consumer evidence has its own English suite");
        assertTrue(doc.text().contains("Recorded comparisons"), "Comparison phase is named in English");
        assertFalse(html.contains("Rezultatul verificării") || html.contains("Componente browser") || html.contains("Comparație cu Android"),
                "Publisher narration does not use the previous Romanian labels");
        Element first = scenario(doc, BrowserReportFixture.FIRST), last = scenario(doc, BrowserReportFixture.LAST);
        String a = BrowserReportFixture.payload(root.resolve("first.png")), b = BrowserReportFixture.payload(root.resolve("second.png")), c = BrowserReportFixture.payload(root.resolve("last.png"));
        assertTrue(first.outerHtml().contains(a) && first.outerHtml().contains(b), "Both frames stay in their original case");
        assertFalse(first.outerHtml().contains(c), "Another case's frame cannot be assigned to the first case");
        assertTrue(last.outerHtml().contains(c), "The final case retains its own frame");
        assertFalse(last.outerHtml().contains(a), "The final case does not inherit the first capture");
        assertTrue(last.text().contains("EXPECTED_FINAL_♠") && last.text().contains("OBSERVED_FINAL_♥"), "Long expected and observed values remain complete");
        assertTrue(last.text().contains("Mâna următoare"), "English report narration preserves original Romanian application evidence");
        var structured = new atf.tests.reporting.GameReportTextAudit(scenario(doc, BrowserReportFixture.LARGE_CASE).outerHtml());
        assertTrue(structured.contains(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(BrowserReportFixture.LARGE)),
                "Structured input beyond 12000 characters retains the final scalar and exact large integer");
        assertTrue(last.outerHtml().contains("fail"), "A failed recorded comparison retains its outcome");
        assertEquals(0, doc.select("img[src]:not([src^=data:]), a[data-img]:not([data-img^=data:])").size(), "Captures need no external files");
    }

    @Test void missingCaptureIsRejectedBeforeReportCreation(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root); Files.delete(root.resolve("last.png"));
        inputs.add("Negative control", "The final case refers to a missing PNG after valid earlier cases.");
        assertThrows(java.io.IOException.class, () -> BrowserReportFixture.publish(publisher, root));
        assertFalse(Files.exists(root.resolve("report.html")), "Missing late evidence cannot leave an apparently qualified report");
    }

    @Test void passingTableFlowsCannotUnderstateTheirHandOrCaptureBudget(ComponentInputs inputs) throws Exception {
        inputs.add("Synthetic flow admission", "Publisher independently checks rounds, deal order and actual image references before creating HTML.");
        for (String mutation : java.util.List.of("long", "sequence", "captures", "missing", "owner", "omitted-step")) {
            Path evidence = root.resolve(mutation);
            var test = BrowserReportFixture.sanity(evidence);
            var observed = (com.fasterxml.jackson.databind.node.ObjectNode) test.get("observed");
            var rounds = (com.fasterxml.jackson.databind.node.ArrayNode) observed.get("rounds");
            var budget = (com.fasterxml.jackson.databind.node.ObjectNode) observed.get("flowBudget");
            switch (mutation) {
                case "long" -> rounds.addObject().put("hand", 3).put("cards", 1);
                case "sequence" -> ((com.fasterxml.jackson.databind.node.ObjectNode) rounds.get(1)).put("cards", 2);
                case "captures" -> budget.put("normalCaptures", 1);
                case "missing" -> observed.remove("flowBudget");
                case "owner" -> ((com.fasterxml.jackson.databind.node.ObjectNode) test.get("steps").get(0).get("images").get(0)).put("role", "foreign");
                case "omitted-step" -> ((com.fasterxml.jackson.databind.node.ArrayNode) budget.get("frames")).remove(3);
            }
            BrowserReportFixture.writeSteps(evidence, test);
            var failure = assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
            assertTrue(failure.getMessage().contains(switch (mutation) {
                case "sequence" -> "E2E_EXECUTION_SEQUENCE";
                case "omitted-step" -> "E2E_EXECUTION_SEQUENCE";
                case "owner" -> "E2E_CAPTURE_OWNERSHIP";
                case "captures" -> "E2E_CAPTURE_BUDGET";
                case "missing" -> "flowBudget";
                default -> "E2E_HAND_BUDGET";
            }), "Original rejection cause is retained for " + mutation);
            assertFalse(Files.exists(evidence.resolve("report.html")), "Invalid flow cannot leave a report");
        }
    }

    @Test void pairedSanityPublishesEverySemanticRoleAndItsOwnFourImages(ComponentInputs inputs) throws Exception {
        var test = BrowserReportFixture.sanity(root);
        inputs.add("Synthetic paired scenario", test.get("observed").toString());
        BrowserReportFixture.publish(publisher, root);
        var flow = scenario(Jsoup.parse(Files.readString(root.resolve("report.html"))), "Observed four-player flow");
        for (var step : test.get("steps")) assertTrue(flow.text().contains(step.get("id").asText()), "Every role retains an ordered assertion owner");
        assertEquals(4, flow.select("a[data-atf-screenshot-ref]").size(), "Only the four planned checkpoints carry images");
    }

    @Test void externalCaptureAndInvalidPngCannotBePublished(ComponentInputs inputs) throws Exception {
        Path evidence = root.resolve("evidence"); BrowserReportFixture.write(evidence);
        Files.copy(evidence.resolve("first.png"), root.resolve("outside.png"));
        BrowserReportFixture.change(evidence, "image", "../outside.png");
        inputs.add("Negative controls", "An existing external PNG, then invalid PNG bytes inside the evidence directory.");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
        assertFalse(Files.exists(evidence.resolve("report.html")), "External image fails preflight");
        BrowserReportFixture.change(evidence, "image", "first.png"); Files.writeString(evidence.resolve("first.png"), "not a PNG");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
        assertFalse(Files.exists(evidence.resolve("report.html")), "Invalid image fails preflight");
    }

    @Test void invalidLayerAndImageShapeDoNotCreatePartialReports(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.write(root); BrowserReportFixture.change(root, "layer", "unknown");
        inputs.add("Negative controls", "Unknown evidence ownership, then a malformed image inventory.");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, root));
        BrowserReportFixture.change(root, "layer", "component"); BrowserReportFixture.change(root, "images", "invalid");
        assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, root));
        assertFalse(Files.exists(root.resolve("report.html")), "Malformed inventories cannot be partially published");
    }

    @Test void orderedStepsKeepEachActionBeforeAfterAndAssertionsInTheirOriginalOwner(ComponentInputs inputs) throws Exception {
        BrowserReportFixture.steps(root);
        inputs.add("Ordered flow fixture", "Two recorded deposits, a real driver-action label and an observation label, distinct before/after values, assertions and images.");
        BrowserReportFixture.publish(publisher, root);
        Document doc = Jsoup.parse(Files.readString(root.resolve("report.html")));
        Element flow = scenario(doc, "Observed four-player flow");
        String text = flow.text();
        assertTrue(text.indexOf("Human deposits A♠") < text.indexOf("AI deposits K♥"), "Canonical steps retain their execution order");
        for (int i = 0; i < 2; i++) {
            assertTrue(text.contains("BEFORE_DEPOSIT_" + i) && text.contains("AFTER_DEPOSIT_" + i), "Each step retains both original states");
            assertTrue(text.contains("FRAME_ASSERTION_" + i), "Evaluated assertion rows are present, not only an aggregate PASS");
            assertTrue(text.contains("DEPOSIT_CAPTURE_" + i), "Each capture belongs to its producing step");
        }
        assertTrue(text.contains("Click the legal A♠") && text.contains("Observe the AI deposit"), "Observing an AI action cannot be relabelled as a driver click");
        String flowHtml = flow.outerHtml();
        int firstStep = flowHtml.indexOf("Human deposits A♠"), secondStep = flowHtml.indexOf("AI deposits K♥");
        int firstImage = flowHtml.indexOf("DEPOSIT_CAPTURE_0"), secondImage = flowHtml.indexOf("DEPOSIT_CAPTURE_1");
        assertTrue(firstImage > firstStep && firstImage < secondStep && secondImage > secondStep, "Step images remain between their original action and the next action");
        // Reporting may intern identical PNG bytes; the owner keeps its ordered reference.
        var links = flow.select("a[data-atf-screenshot-ref]");
        assertEquals(2, links.size(), "Both steps retain their own image reference");
        for (int i = 0; i < 2; i++) {
            String ref = links.get(i).attr("data-atf-screenshot-ref");
            Element image = doc.getElementById("atf-screenshot-" + ref);
            assertNotNull(image, "The step reference resolves inside the same HTML");
            assertTrue(image.attr("href").endsWith(BrowserReportFixture.payload(root.resolve(i == 0 ? "first.png" : "second.png"))), "The referenced bytes belong to the original step");
        }
        assertFalse(text.contains("Recorded comparisons"), "Stepped flows do not add a synthetic aggregate assertion");
        assertFalse(doc.selectFirst("#dashboard-view").text().contains("FRAME_ASSERTION"), "Overview remains standard");
    }

    @Test void incompleteOrContradictoryStepEvidenceIsRejectedBeforePublication(ComponentInputs inputs) throws Exception {
        inputs.add("Negative controls", "Missing assertions or before state, duplicated IDs, a non-Boolean or contradictory verdict, a passing case with a failed step, and an absent/negative observed interval.");
        for (String mutation : java.util.List.of("assertions", "before", "null-before", "duplicate", "boolean", "step-status", "case-status", "duration")) {
            Path evidence = root.resolve(mutation);
            var test = BrowserReportFixture.steps(evidence);
            var step = (com.fasterxml.jackson.databind.node.ObjectNode) test.get("steps").get(0);
            var assertion = (com.fasterxml.jackson.databind.node.ObjectNode) step.get("assertions").get(0);
            switch (mutation) {
                case "assertions" -> step.putArray("assertions");
                case "before" -> step.remove("before");
                case "null-before" -> step.putNull("before");
                case "duplicate" -> ((com.fasterxml.jackson.databind.node.ObjectNode) test.get("steps").get(1)).put("id", "DEPOSIT-1");
                case "boolean" -> assertion.put("passed", "true");
                case "step-status" -> step.put("status", "FAIL");
                case "case-status" -> { assertion.put("passed", false); step.put("status", "FAIL"); }
                case "duration" -> step.put("durationMillis", -1);
                default -> throw new AssertionError(mutation);
            }
            BrowserReportFixture.writeSteps(evidence, test);
            assertThrows(IllegalArgumentException.class, () -> BrowserReportFixture.publish(publisher, evidence));
            assertFalse(Files.exists(evidence.resolve("report.html")), "Rejected step data creates no partial HTML: " + mutation);
        }
    }

    private static Element scenario(Document doc, String title) {
        Element node = doc.select("a.node").stream().filter(element -> element.text().equals(title)).findFirst().orElseThrow();
        while (!node.hasClass("card")) node = node.parent();
        return node;
    }
}
