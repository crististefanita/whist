package atf.tests.execution.components;

import atf.tests.execution.proofs.StaticProofRun;
import atf.tests.testchange.TestChangeProof;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Pins the Git review before execution; neutral recovery never authorizes a different review. */
public final class TestChangeProofExecution {
    public static void main(String[] args) throws Exception {
        if (args.length == 4 && args[0].equals("--list")) {
            TestChangeReview.requiredProofs(Path.of(args[1]), args[2], args[3], LocalDate.now(ZoneOffset.UTC))
                    .forEach(System.out::println);
            return;
        }
        if (args.length != 7 && args.length != 8)
            throw new IllegalArgumentException("expected checkout, stage, output, provenance, base, control, commit and optional previous control");
        Path checkout = Path.of(args[0]).toRealPath();
        Path stage = Path.of(args[1]).toRealPath();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        Path parent = output.getParent().toRealPath();
        if (parent.startsWith(checkout) || parent.startsWith(stage) || !output.getFileName().toString().equals(args[5]))
            throw new IllegalArgumentException("proof output must be external and named after the control");
        Path previous = args.length == 8 ? Path.of(args[7]).toRealPath() : null;
        if (previous != null && (previous.startsWith(checkout) || previous.startsWith(stage)
                || previous.equals(output) || !previous.getFileName().toString().equals(args[5])))
            throw new IllegalArgumentException("previous proof must be a distinct external directory for this control");
        var plan = TestChangeReview.proofPlan(checkout, args[4], args[5], LocalDate.now(ZoneOffset.UTC));
        if (!plan.identity().commit().equals(args[6])) throw new IllegalStateException("TEST-CHANGE-PROOF-COMMIT: staged checkout moved");
        byte[] provenance = context(stage, Path.of(args[3]), plan);
        Files.createDirectory(output);
        System.setProperty("atf.governance.root", stage.toString());
        var expected = Stream.concat(plan.checks().positive().stream(), plan.checks().negative().stream()).toList();
        Map<String, String> observed = StaticProofRun.execute(args[5], expected, output, provenance, previous);
        if (!plan.equals(TestChangeReview.proofPlan(checkout, args[4], args[5], LocalDate.now(ZoneOffset.UTC))))
            throw new IllegalStateException("TEST-CHANGE-PROOF-DIFF: review changed during execution");
        TestChangeProof.stage(plan, outcomes(plan.checks().positive(), observed), outcomes(plan.checks().negative(), observed), output);
        System.out.println("PASS " + plan.identity() + " cases=" + expected.size());
    }

    private static byte[] context(Path stage, Path provenance, TestChangeProof.Plan plan) throws Exception {
        var mapper = new ObjectMapper();
        ObjectNode context = (ObjectNode) mapper.readTree(provenance.toFile());
        var compiler = mapper.readTree(stage.getParent().resolve("proof-compiler-context.json").toFile());
        if (compiler.path("schemaVersion").asInt() != 2) throw new IllegalStateException("TEST-CHANGE-PROOF-COMPILER: verified preparation required");
        ObjectNode identity = mapper.createObjectNode();
        for (String field : List.of("identity", "compilerFingerprint", "toolFingerprint", "buildFingerprint", "classpathSha256")) {
            if (!compiler.path(field).asText().matches("[a-fA-F0-9]{64}"))
                throw new IllegalStateException("TEST-CHANGE-PROOF-COMPILER: missing " + field);
            identity.set(field, compiler.get(field));
        }
        identity.put("javaRuntime", System.getProperty("java.runtime.version"));
        identity.put("javaVendor", System.getProperty("java.vendor"));
        identity.set("jvmArguments", mapper.valueToTree(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));
        context.set("testChangeReview", mapper.valueToTree(plan.identity()));
        context.set("testChangeChecks", mapper.valueToTree(plan.checks()));
        context.set("testChangeCompiler", identity);
        return mapper.writeValueAsBytes(context);
    }

    private static List<TestChangeProof.Outcome> outcomes(List<String> selectors, Map<String, String> observed) {
        return selectors.stream().map(selector -> new TestChangeProof.Outcome(selector, observed.get(selector))).toList();
    }

    private TestChangeProofExecution() { }
}
