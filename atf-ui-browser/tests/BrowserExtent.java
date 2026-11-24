import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.android.evidence.flow.UiSanityPlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashSet;

/** Publishes recorded comparisons and scenario-owned captures using the pinned Reporting API. */
class BrowserExtent {
    record Check(JsonNode result, String layer, List<ReportScreenshot> captures, List<Step> steps) { }
    record Step(JsonNode input, List<AtfAssertion> assertions, List<ReportScreenshot> captures, AtfReportStatus status) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Evidence directory and new Extent path required");
        Path directory = Path.of(args[0]).toRealPath();
        Path report = Path.of(args[1]).toAbsolutePath().normalize();
        if (!report.getParent().equals(directory) || Files.exists(report)) {
            throw new IllegalArgumentException("Use a new report inside the evidence directory");
        }
        var mapper = new ObjectMapper();
        var checks = new ArrayList<Check>();
        readCases(mapper, directory, "browser-checks.json", "component", checks);
        readCases(mapper, directory, "browser-ui-checks.json", "ui", checks);
        Path parity = directory.resolve("whist-parity.json");
        if (Files.isRegularFile(parity)) checks.add(check(mapper.readTree(parity.toFile()), "component", directory));
        readCases(mapper, directory, "paired-android.json", "android-comparison", checks);
        if (checks.isEmpty()) throw new IllegalArgumentException("No recorded cases");
        var identities = new HashSet<String>();
        for (var check : checks) if (!identities.add(check.layer() + ":" + text(check.result(), "id")))
            throw new IllegalArgumentException("Duplicate recorded case identity");
        var profile = ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT);
        // Keep standard comparison previews; original bodies must remain complete in their disclosures.
        var settings = new ReportingSettings(ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS,
                ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT);
        try (var session = new ExtentReportSession(report, settings)) {
            for (var check : checks) {
                var test = check.result();
                String title = test.required("name").asText();
                var status = AtfReportStatus.valueOf(test.required("status").asText());
                var raw = session.startScenario(suite(check.layer()), "Card Table", title,
                        List.of(check.layer(), "recorded-evidence"));
                try (var scenario = UiExtentReportSession.adaptScenario(raw, profile, title);
                     var phase = scenario.startPhase("TEST BODY")) {
                    phase.observation(new ReportObservation("Recorded inputs and identity", List.of(
                            new ReportField("Case ID", test.path("id").asText(title)),
                            new ReportField("Evidence layer", check.layer()),
                            new ReportField("Flow detail", check.steps().isEmpty() ? "Aggregate summary; no per-action trace was recorded" : "Ordered before/after checkpoints"),
                            new ReportField("Original outcome", status.name())), List.of(
                            body("Original expected", test.required("expected")),
                            body("Original observed", test.required("observed"))), List.of()));
                    if (check.steps().isEmpty()) phase.uiValidation("Recorded comparisons", "Captured test result", null,
                            List.of(new AtfAssertion(title, display(test.required("expected")),
                                    display(test.required("observed")), status == AtfReportStatus.PASS, status)), status);
                    int sequence = 0;
                    for (var step : check.steps()) publishStep(phase, step, ++sequence);
                    for (var capture : check.captures()) {
                        phase.uiValidationAsMedia(capture.title(), "Captured checkpoint", capture.base64Png(),
                                List.of(), List.of(), status);
                    }
                    phase.complete(status);
                    scenario.complete(status);
                }
            }
        }
        System.out.println(report);
    }

    private static void readCases(ObjectMapper mapper, Path directory, String file, String layer,
                                  List<Check> checks) throws Exception {
        Path source = directory.resolve(file);
        if (!Files.isRegularFile(source)) return;
        var cases = mapper.readTree(source.toFile());
        if (!cases.isArray() || cases.isEmpty()) throw new IllegalArgumentException("No cases in " + file);
        for (var result : cases) checks.add(check(result, layer, directory));
    }

    private static Check check(JsonNode test, String defaultLayer, Path directory) throws Exception {
        if (!test.isObject() || test.required("name").asText().isBlank())
            throw new IllegalArgumentException("Recorded case name is required");
        AtfReportStatus.valueOf(test.required("status").asText());
        text(test, "id");
        test.required("expected"); test.required("observed");
        if (test.path("id").asText().startsWith("MATCH-ANCHORS-"))
            verifyFlowBudget(test);
        String layer = test.path("layer").asText(defaultLayer);
        suite(layer);
        var steps = new ArrayList<Step>();
        var ids = new HashSet<String>();
        if (test.has("steps")) {
            if (!test.get("steps").isArray() || test.get("steps").isEmpty())
                throw new IllegalArgumentException("Recorded steps must be a nonempty array");
            for (var step : test.get("steps")) {
                if (!ids.add(text(step, "id"))) throw new IllegalArgumentException("Duplicate step ID");
                text(step, "name"); text(step, "action"); text(step, "url");
                AtfGherkinKeyword.valueOf(text(step, "keyword"));
                AtfUiEvidenceSource.valueOf(text(step, "source"));
                if (!step.required("durationMillis").isIntegralNumber() || !step.get("durationMillis").canConvertToLong()
                        || step.get("durationMillis").longValue() < 0) throw new IllegalArgumentException("Observed step duration is required");
                step.required("before").requireNonNull(); step.required("after").requireNonNull();
                if (!step.required("assertions").isArray() || step.get("assertions").isEmpty())
                    throw new IllegalArgumentException("Each step requires captured assertions");
                var assertions = new ArrayList<AtfAssertion>();
                for (var row : step.get("assertions")) {
                    if (!row.required("passed").isBoolean()) throw new IllegalArgumentException("Assertion verdict must be Boolean");
                    assertions.add(new AtfAssertion(text(row, "name"), display(row.required("expected").requireNonNull()),
                            display(row.required("observed").requireNonNull()), row.get("passed").booleanValue()));
                }
                var outcome = assertions.stream().allMatch(AtfAssertion::passed) ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                if (!outcome.name().equals(text(step, "status"))) throw new IllegalArgumentException("Step outcome contradicts assertions");
                if (test.get("status").asText().equals("PASS") && outcome != AtfReportStatus.PASS)
                    throw new IllegalArgumentException("Passing case contains a failed step");
                steps.add(new Step(step, List.copyOf(assertions), captures(step, directory), outcome));
            }
        }
        return new Check(test, layer, captures(test, directory), List.copyOf(steps));
    }

    private static void publishStep(UiExtentReportSession.UiPhase phase, Step step, int sequence) {
        var input = step.input();
        String name = input.get("id").asText() + " · " + input.get("name").asText();
        phase.observation(new ReportObservation(name + " — original before/after", List.of(),
                List.of(body("Before", input.get("before")), body("After", input.get("after"))), List.of()));
        var action = new AtfUiActionRecord(sequence, input.get("action").asText(), input.get("url").asText(),
                "Observe the recorded before/after comparison", "Captured state transition", input.get("durationMillis").longValue(), step.status(),
                AtfUiEvidenceSource.valueOf(input.get("source").asText()));
        phase.step(AtfGherkinKeyword.valueOf(input.get("keyword").asText()), name, input.get("url").asText(), "",
                step.assertions(), List.of(action), step.status());
        for (var capture : step.captures()) phase.uiValidationAsMedia(capture.title(), name, capture.base64Png(),
                List.of(), List.of(), step.status());
    }

    private static void verifyFlowBudget(JsonNode test) throws Exception {
        var observed = test.required("observed");
        var rounds = observed.required("rounds");
        var deals = observed.required("profile").required("deals");
        var budget = observed.required("flowBudget");
        var plan = UiSanityPlan.parse(budget.required("plan").toString());
        boolean completed = test.required("status").asText().equals("PASS");
        if (!rounds.isArray() || rounds.size() > plan.deals().size() || !deals.equals(plan.specification().get("deals"))
                || (completed && rounds.size() != plan.deals().size())
                || !budget.path("contract").asText().equals("MINIMAL_TABLE_FLOW_V1")
                || !budget.path("scenarioId").asText().equals(plan.id())
                || budget.path("hands").asInt(-1) != rounds.size() || budget.path("maximumHands").asInt(-1) != 2
                || budget.path("maximumNormalCaptures").asInt(-1) != plan.maximumNormalCaptures())
            throw new IllegalArgumentException("E2E_HAND_BUDGET");
        for (int i = 0; i < rounds.size(); i++) {
            var round = rounds.get(i);
            if (!round.path("hand").isIntegralNumber() || round.path("hand").asInt() != i + 1
                    || !deals.get(i).isIntegralNumber() || deals.get(i).asInt() <= 0
                    || !round.path("cards").equals(deals.get(i)))
                throw new IllegalArgumentException("E2E_EXECUTION_SEQUENCE");
        }
        var images = new ArrayList<JsonNode>();
        test.path("images").forEach(images::add);
        test.path("steps").forEach(step -> step.path("images").forEach(images::add));
        var files = new HashSet<String>();
        for (var image : images) if (!files.add(image.path("file").asText()))
            throw new IllegalArgumentException("E2E_CAPTURE_BUDGET");
        var frames = new ArrayList<UiSanityPlan.Frame>();
        if (!budget.required("frames").isArray()) throw new IllegalArgumentException("E2E_EXECUTION_SEQUENCE");
        for (var frame : budget.get("frames")) frames.add(new UiSanityPlan.Frame(text(frame, "id"),
                frame.path("capture").isNull() ? null : text(frame, "capture")));
        plan.verify(frames, completed);
        var declaredRoles = frames.stream().map(UiSanityPlan.Frame::id).filter(plan.steps()::contains).toList();
        var publishedRoles = new ArrayList<String>();
        for (var step : test.path("steps")) if (step.path("id").asText().startsWith("FLOW-"))
            publishedRoles.add(step.get("id").asText().substring(5));
        if (!declaredRoles.equals(publishedRoles)) throw new IllegalArgumentException("E2E_EXECUTION_SEQUENCE");
        var frameFiles = new HashSet<String>();
        for (var frame : frames) if (frame.capture() != null) frameFiles.add(frame.capture());
        if (!files.equals(frameFiles)) throw new IllegalArgumentException("E2E_CAPTURE_OWNERSHIP");
        for (var image : images) if (frames.stream().noneMatch(f -> image.path("file").asText().equals(f.capture())
                && image.path("role").asText().equals(f.id()))) throw new IllegalArgumentException("E2E_CAPTURE_OWNERSHIP");
        long failures = frames.stream().filter(f -> f.id().equals("failure") && f.capture() != null).count();
        if (budget.path("normalCaptures").asInt(-1) != images.size() - failures
                || budget.path("failureCaptures").asInt(-1) != failures)
            throw new IllegalArgumentException("E2E_CAPTURE_BUDGET");
    }

    private static String text(JsonNode input, String field) {
        var value = input.required(field);
        if (!value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("Nonempty text required: " + field);
        return value.asText();
    }

    private static List<ReportScreenshot> captures(JsonNode test, Path directory) throws Exception {
        var captures = new ArrayList<ReportScreenshot>();
        if (test.has("images") && !test.get("images").isArray())
            throw new IllegalArgumentException("Captured images must be an array");
        for (var image : test.path("images")) {
            Path png = directory.resolve(image.required("file").asText()).toRealPath();
            if (!png.getParent().equals(directory)) throw new IllegalArgumentException("Image escaped evidence root");
            String payload = Base64.getEncoder().encodeToString(Files.readAllBytes(png));
            if (!ReportScreenshot.hasPngSignature(payload)) throw new IllegalArgumentException("Invalid PNG: " + png.getFileName());
            captures.add(new ReportScreenshot(image.required("title").asText(), payload));
        }
        return List.copyOf(captures);
    }

    private static String suite(String layer) {
        return switch (layer) {
            case "component" -> "Browser components";
            case "ui" -> "Browser UI";
            case "android-component" -> "Android components";
            case "android-comparison" -> "Android and Web comparison";
            default -> throw new IllegalArgumentException("Unknown evidence layer: " + layer);
        };
    }

    private static String display(JsonNode value) {
        return value.isTextual() ? value.asText() : value.toPrettyString();
    }

    private static ReportObservation.Body body(String label, JsonNode value) {
        return new ReportObservation.Body(label, display(value), value.isTextual()
                ? ReportObservation.Format.TEXT : ReportObservation.Format.JSON);
    }
}
