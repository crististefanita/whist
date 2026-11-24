package atf.tests.reporting.browser;

import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.tools.ToolProvider;

/** Explicit synthetic publisher inputs; no game or browser execution is implied. */
final class BrowserReportFixture {
    static final String FIRST = "First recorded component";
    static final String LAST = "Last recorded component";
    static final String EXPECTED = "expected ".repeat(70) + "EXPECTED_FINAL_♠ · Mâna următoare";
    static final String ACTUAL = "observed ".repeat(70) + "OBSERVED_FINAL_♥";
    static final String LARGE_CASE = "Large structured component";
    static final Map<String, Object> LARGE = Map.of("context", "captured ".repeat(2000),
            "integer", 9007199254740993L, "final", "JSON_FINAL_♠");
    private BrowserReportFixture() { }

    static URLClassLoader compile(Path classes) throws Exception {
        Path source = GovernanceProject.locateProjectRoot().resolve("atf-ui-browser/tests/BrowserExtent.java");
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-encoding", "UTF-8",
                "-cp", System.getProperty("java.class.path"), "-d", classes.toString(), source.toString());
        if (result != 0) throw new IllegalStateException("Browser publisher compilation failed: " + result);
        return new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, BrowserReportFixture.class.getClassLoader());
    }

    static void write(Path root) throws Exception {
        Files.createDirectories(root);
        png(root.resolve("first.png"), 0xffdd1111);
        png(root.resolve("second.png"), 0xff1155dd);
        png(root.resolve("last.png"), 0xff11aa11);
        var first = Map.of("id", "FIXTURE-FIRST", "name", FIRST, "status", "PASS", "expected", EXPECTED,
                "observed", EXPECTED, "images", List.of(image("first.png", "First captured frame"),
                image("second.png", "Second captured frame")));
        var last = Map.of("id", "FIXTURE-LAST", "name", LAST, "status", "FAIL", "expected", EXPECTED,
                "observed", ACTUAL, "layer", "android-component", "images", List.of(image("last.png", "Last captured frame")));
        var large = Map.of("id", "FIXTURE-LARGE", "name", LARGE_CASE, "status", "PASS",
                "expected", LARGE, "observed", LARGE, "images", List.of());
        new ObjectMapper().writeValue(root.resolve("browser-checks.json").toFile(), List.of(first, large, last));
    }

    static void publish(URLClassLoader loader, Path root) throws Exception {
        var method = Class.forName("BrowserExtent", true, loader).getDeclaredMethod("main", String[].class);
        method.setAccessible(true);
        try { method.invoke(null, (Object) new String[]{root.toString(), root.resolve("report.html").toString()}); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception original) throw original;
            if (failure.getCause() instanceof Error original) throw original;
            throw failure;
        }
    }

    static void change(Path root, String field, String value) throws Exception {
        var json = new ObjectMapper();
        var cases = json.readTree(root.resolve("browser-checks.json").toFile());
        if (field.equals("image")) ((com.fasterxml.jackson.databind.node.ObjectNode) cases.get(0).get("images").get(0)).put("file", value);
        else ((com.fasterxml.jackson.databind.node.ObjectNode) cases.get(0)).put(field, value);
        json.writeValue(root.resolve("browser-checks.json").toFile(), cases);
    }

    static com.fasterxml.jackson.databind.node.ObjectNode steps(Path root) throws Exception {
        write(root);
        var json = new ObjectMapper();
        var test = json.createObjectNode().put("id", "ORDERED-FLOW").put("name", "Observed four-player flow").put("status", "PASS");
        test.put("expected", "Stable frames and old table faces").put("observed", "Captured before and after each deposit");
        var steps = test.putArray("steps");
        for (int i = 0; i < 2; i++) {
            var step = steps.addObject().put("id", "DEPOSIT-" + (i + 1)).put("name", i == 0 ? "Human deposits A♠" : "AI deposits K♥")
                    .put("keyword", i == 0 ? "WHEN" : "THEN").put("source", i == 0 ? "ACTION" : "OBSERVE")
                    .put("url", "https://example.invalid/whist/").put("action", i == 0 ? "Click the legal A♠" : "Observe the AI deposit")
                    .put("durationMillis", 37).put("status", "PASS");
            step.putObject("before").put("identity", "BEFORE_DEPOSIT_" + i).putArray("frame").add(10).add(20).add(50).add(70);
            step.putObject("after").put("identity", "AFTER_DEPOSIT_" + i).putArray("frame").add(10).add(20).add(50).add(70);
            step.putArray("assertions").addObject().put("name", "FRAME_ASSERTION_" + i).put("expected", "< 0.02 CSS px").put("observed", 0).put("passed", true);
            step.putArray("images").addObject().put("file", i == 0 ? "first.png" : "second.png").put("title", "DEPOSIT_CAPTURE_" + i);
        }
        json.writeValue(root.resolve("browser-ui-checks.json").toFile(), java.util.List.of(test));
        return test;
    }

    static void writeSteps(Path root, com.fasterxml.jackson.databind.node.ObjectNode test) throws Exception {
        new ObjectMapper().writeValue(root.resolve("browser-ui-checks.json").toFile(), java.util.List.of(test));
    }

    static com.fasterxml.jackson.databind.node.ObjectNode sanity(Path root) throws Exception {
        var test = steps(root); var json = new ObjectMapper();
        var template = test.withArray("steps").get(0).deepCopy();
        var plan = atf.android.evidence.flow.UiSanityPlan.load(GovernanceProject.locateProjectRoot().resolve("config/table-sanity-plan.json"));
        test.put("id", "MATCH-ANCHORS-fixture"); var rows = test.putArray("steps");
        var observed = test.putObject("observed");
        observed.putObject("profile").set("deals", json.valueToTree(plan.deals()));
        var rounds = observed.putArray("rounds");
        for (int i = 0; i < plan.deals().size(); i++) rounds.addObject().put("hand", i + 1).put("cards", plan.deals().get(i));
        var budget = observed.putObject("flowBudget").put("contract", "MINIMAL_TABLE_FLOW_V1").put("scenarioId", plan.id())
                .put("hands", 2).put("normalCaptures", 4).put("failureCaptures", 0).put("maximumHands", 2).put("maximumNormalCaptures", 4);
        budget.set("plan", plan.specification()); var frames = budget.putArray("frames");
        for (String role : plan.steps()) {
            var row = ((com.fasterxml.jackson.databind.node.ObjectNode) template.deepCopy()).put("id", "FLOW-" + role).put("name", role);
            var images = row.putArray("images"); var frame = frames.addObject().put("id", role);
            if (plan.captureSteps().contains(role)) {
                String name = role + ".png"; png(root.resolve(name), 0xff557799 + plan.steps().indexOf(role));
                images.addObject().put("file", name).put("title", "Captured " + role).put("role", role); frame.put("capture", name);
            } else frame.putNull("capture");
            rows.add(row);
        }
        writeSteps(root, test); return test;
    }

    static String payload(Path image) throws Exception { return Base64.getEncoder().encodeToString(Files.readAllBytes(image)); }
    private static Map<String, String> image(String file, String title) { return Map.of("file", file, "title", title); }
    private static void png(Path file, int color) throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, color);
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes); Files.write(file, bytes.toByteArray());
        }
    }
}
