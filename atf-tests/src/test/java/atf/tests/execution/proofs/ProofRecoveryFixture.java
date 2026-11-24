package atf.tests.execution.proofs;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** A disposable child observes actual body executions independently of checkpoint metadata. */
final class ProofRecoveryFixture {
    static final String CONTROL = "FIXTURE-STATIC-PROOF";
    static List<String> selectors() {
        return List.of("preserved", "resumable", "pending").stream().map(name -> Cases.class.getName() + "#" + name).toList();
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        System.setProperty("atf.proof.fixture", root.toString());
        Path output = Files.createDirectory(root.resolve(args[1]));
        StaticProofRun.execute(CONTROL, selectors(), output, Files.readAllBytes(root.resolve("provenance.json")),
                args.length == 3 ? root.resolve(args[2]) : null);
    }

    @AtfComponentTest @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class Cases {
        private static Path observed(String name) throws Exception {
            Path root = Path.of(System.getProperty("atf.proof.fixture"));
            Files.writeString(root.resolve(name + ".count"), "executed\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return root;
        }
        @Test @Order(1) void preserved() throws Exception {
            observed("preserved");
            AuditedAssertions.assertEquals("original comparison", "original comparison", "Original evaluated values are preserved");
        }
        @Test @Order(2) void resumable() throws Exception {
            Path root = observed("resumable");
            if (Files.exists(root.resolve("halt")) && !Files.exists(root.resolve("allow-pass"))) Runtime.getRuntime().halt(23);
            AuditedAssertions.assertTrue(Files.exists(root.resolve("allow-pass")), "The controlled root cause must be resolved before PASS");
        }
        @Test @Order(3) void pending() throws Exception {
            observed("pending");
            AuditedAssertions.assertTrue(true, "The known pending static invocation runs exactly once");
        }
    }

    private ProofRecoveryFixture() { }
}
