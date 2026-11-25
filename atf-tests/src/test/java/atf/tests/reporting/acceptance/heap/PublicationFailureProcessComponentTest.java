package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class PublicationFailureProcessComponentTest {
    @TempDir Path root;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void freshProcessCanPublishAndSealTheCandidate(ComponentInputs inputs) throws Exception {
        Path report = Files.writeString(root.resolve("report.html"), "previous qualified bytes");
        inputs.add("Boundary", "Real isolated JVM and consumer publication protocol with controlled render/verify operations; no native UI or library allocation claim.");
        assertEquals(0, run(report, "nominal", inputs));
        assertEquals("complete candidate", Files.readString(report), "Only a completed candidate replaces canonical bytes");
        var receipt = receipt(report); inputs.add("Original acceptance receipt", receipt.toPrettyString());
        assertEquals("PASS", receipt.path("status").asText());
        assertTrue(receipt.path("sha256").asText().matches("[a-f0-9]{64}"), "Accepted bytes have a physical seal");
    }

    @Test void realHeapExhaustionAndAbruptTerminationCannotPromotePartialReports(ComponentInputs inputs) throws Exception {
        inputs.add("Preconditions", "One 24 MiB JVM per fault, existing canonical HTML and original failed JUnit. Heap faults allocate until the VM throws OutOfMemoryError; halt faults terminate without Java cleanup.");
        Path xml = Files.writeString(root.resolve("TEST-original.xml"), "<testsuite tests='1' failures='1'/>");
        for (String mode : List.of("heap-render", "heap-verify", "halt-render", "halt-verify")) {
            Path report = Files.writeString(root.resolve(mode + ".html"), "previous qualified bytes");
            int exit = run(report, mode, inputs);
            assertTrue(exit != 0, "The injected real process failure cannot appear successful: " + mode);
            if (mode.startsWith("halt")) assertEquals(37, exit, "Abrupt termination reaches the intended stage");
            assertEquals("previous qualified bytes", Files.readString(report), "Canonical bytes survive " + mode);
            assertEquals("<testsuite tests='1' failures='1'/>", Files.readString(xml), "Original JUnit outcomes survive " + mode);
            var receipt = receipt(report); inputs.add("Original interrupted receipt / " + mode, receipt.toPrettyString());
            String stage = mode.endsWith("render") ? "RENDER_AND_FINALIZE" : "VERIFY";
            var last = receipt.path("stages").get(receipt.path("stages").size() - 1);
            assertEquals(stage, last.path("name").asText(), "The durable journal identifies the last started stage");
            assertFalse(receipt.path("status").asText().equals("PASS"), "Incomplete output is never qualified");
            assertTrue(Files.isRegularFile(Path.of(receipt.path("staged").asText())), "Available partial bytes remain inspectable");
            if (mode.startsWith("heap")) {
                assertEquals("FAIL", receipt.path("status").asText());
                assertEquals("java.lang.OutOfMemoryError", receipt.path("causeType").asText(), "Failure is actual heap exhaustion");
            } else assertEquals("STARTED", last.path("status").asText(), "A halted VM cannot fabricate completion");
            assertEquals(0, run(report, "nominal", inputs), "A fresh attempt succeeds without rewriting JUnit");
            assertEquals("PASS", receipt(report).path("status").asText());
            assertEquals("<testsuite tests='1' failures='1'/>", Files.readString(xml));
        }
    }

    private static int run(Path report, String mode, ComponentInputs inputs) throws Exception {
        Path log = report.resolveSibling(report.getFileName() + "." + mode + ".log");
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var process = new ProcessBuilder(java, "-Xmx24m", "-XX:ActiveProcessorCount=2",
                "-Djava.io.tmpdir=" + report.getParent(), "-cp", System.getProperty("java.class.path"),
                PublicationFailureProbe.class.getName(), report.toString(), mode)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean ended;
        try { ended = process.waitFor(30, TimeUnit.SECONDS); }
        finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); } }
        String original = Files.readString(log); inputs.add("Original child output / " + report.getFileName() + " / " + mode, original);
        assertTrue(ended, "The failure probe terminates within its declared budget");
        return process.exitValue();
    }
    private static com.fasterxml.jackson.databind.JsonNode receipt(Path report) throws Exception {
        return JSON.readTree(report.resolveSibling(report.getFileName() + ".publication.json").toFile());
    }
}
