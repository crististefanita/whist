package atf.android.evidence.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable platform-neutral sanity intent; drivers supply the observed frames. */
public record UiSanityPlan(String id, List<String> steps, List<String> captureSteps, List<String> optionalSteps, List<Integer> deals,
                           int maximumNormalCaptures, int maximumFailureCaptures) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public UiSanityPlan {
        steps = List.copyOf(steps); captureSteps = List.copyOf(captureSteps); optionalSteps = List.copyOf(optionalSteps); deals = List.copyOf(deals);
        require(id != null && id.matches("[A-Z][A-Z0-9-]{3,79}"), "E2E_SCENARIO_ID");
        require(!steps.isEmpty() && steps.size() <= 20 && !steps.contains("failure") && new HashSet<>(steps).size() == steps.size()
                && steps.stream().allMatch(s -> s.matches("[a-z][a-z0-9-]*")), "E2E_STEP_PLAN");
        require(!deals.isEmpty() && deals.size() <= 2 && deals.stream().allMatch(n -> n > 0 && n <= 17), "E2E_HAND_BUDGET");
        require(steps.stream().filter(s -> s.equals("score") || s.endsWith("-score")).count() == deals.size(), "E2E_EXECUTION_SEQUENCE");
        require(maximumNormalCaptures > 0 && maximumNormalCaptures <= 4 && maximumFailureCaptures == 1
                && !captureSteps.isEmpty() && captureSteps.size() <= maximumNormalCaptures
                && new HashSet<>(captureSteps).size() == captureSteps.size()
                && steps.containsAll(captureSteps), "E2E_CAPTURE_BUDGET");
        require(new HashSet<>(optionalSteps).size() == optionalSteps.size(), "E2E_STEP_PLAN");
        for (String optional : optionalSteps) require(optional.matches("[a-z][a-z0-9-]*")
                && !steps.contains(optional) && !optional.equals("failure"), "E2E_STEP_PLAN");
    }

    public static UiSanityPlan load(Path source) throws IOException {
        for (Path node = source.toAbsolutePath(); node != null; node = node.getParent())
            require(!Files.isSymbolicLink(node), "E2E_INDIRECT_PLAN");
        return parse(Files.readString(source));
    }

    public static UiSanityPlan parse(String text) throws IOException {
        JsonNode input = JSON.readTree(text);
        require(input != null && input.isObject() && input.size() == 8 && integer(input.path("schemaVersion"))
                && input.path("schemaVersion").intValue() == 1 && input.path("id").isTextual(), "E2E_PLAN_SCHEMA");
        var steps = strings(input.path("steps")); var captures = strings(input.path("captureSteps"));
        require(input.path("deals").isArray(), "E2E_HAND_BUDGET");
        var deals = new java.util.ArrayList<Integer>();
        for (JsonNode value : input.path("deals")) { require(integer(value), "E2E_ILLEGAL_DEAL"); deals.add(value.intValue()); }
        require(integer(input.path("maximumNormalCaptures")) && integer(input.path("maximumFailureCaptures")), "E2E_CAPTURE_BUDGET");
        return new UiSanityPlan(input.path("id").textValue(), steps, captures, strings(input.path("optionalSteps")), deals,
                input.path("maximumNormalCaptures").intValue(), input.path("maximumFailureCaptures").intValue());
    }

    public String json() { return specification().toString(); }

    public JsonNode specification() {
        var result = JSON.createObjectNode().put("schemaVersion", 1).put("id", id);
        result.set("steps", JSON.valueToTree(steps)); result.set("captureSteps", JSON.valueToTree(captureSteps));
        result.set("optionalSteps", JSON.valueToTree(optionalSteps));
        result.set("deals", JSON.valueToTree(deals));
        return result.put("maximumNormalCaptures", maximumNormalCaptures).put("maximumFailureCaptures", maximumFailureCaptures);
    }

    public void execute(StepDriver driver) throws Exception {
        for (String step : steps) driver.execute(step);
    }

    public void verify(List<Frame> frames, boolean completed) {
        int ordinal = 0, normal = 0, failures = 0;
        var captures = new HashSet<String>();
        boolean failed = false;
        for (var frame : frames) {
            Objects.requireNonNull(frame);
            boolean captured = frame.capture() != null && !frame.capture().isBlank();
            if (captured) require(captures.add(frame.capture()), "E2E_DUPLICATE_CAPTURE");
            if (frame.id().equals("failure")) {
                require(!completed && !failed, "E2E_FAILURE_CAPTURE_BUDGET");
                failed = true; if (captured) failures++;
            } else if (optionalSteps.contains(frame.id())) {
                require(!failed && !captured, "E2E_CAPTURE_OWNERSHIP");
            } else {
                require(!failed && ordinal < steps.size() && steps.get(ordinal++).equals(frame.id()), "E2E_EXECUTION_SEQUENCE");
                require(captured == captureSteps.contains(frame.id()), "E2E_CAPTURE_OWNERSHIP");
                if (captured) normal++;
            }
        }
        require(normal <= maximumNormalCaptures && failures <= maximumFailureCaptures, "E2E_CAPTURE_BUDGET");
        require(!completed || ordinal == steps.size(), "E2E_INCOMPLETE_FLOW");
    }

    private static List<String> strings(JsonNode values) {
        require(values.isArray(), "E2E_STEP_PLAN");
        var result = new java.util.ArrayList<String>();
        for (JsonNode value : values) { require(value.isTextual(), "E2E_STEP_PLAN"); result.add(value.textValue()); }
        return result;
    }
    private static boolean integer(JsonNode value) {
        return value.isNumber() && value.canConvertToInt() && value.decimalValue().stripTrailingZeros().scale() <= 0;
    }
    private static void require(boolean condition, String reason) { if (!condition) throw new IllegalArgumentException(reason); }
    @FunctionalInterface public interface StepDriver { void execute(String step) throws Exception; }
    public record Frame(String id, String capture) { }
}
