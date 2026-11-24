package atf.android.component.storage;

import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.support.flow.WhistLabScenario;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class NativeStoragePlanComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void orderedNativeReceiptPreservesFailuresAndRejectsLostForeignOrContradictoryCases(ComponentInputs inputs) throws Exception {
        var script = WhistLabScenario.checkoutRoot().resolve("atf-test-android/tools/native-storage-contract.test.mjs");
        var process = new ProcessBuilder("node", "--test", script.toString()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "Offline native plan contracts terminate within their budget");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        inputs.add("Preconditions", "Controlled native receipts, process phases and original comparisons; no Android device or UI images.");
        inputs.add("Original receipt-control output", output);
        assertEquals(0, process.exitValue(), "Ordered phases, failure retention and rejection controls pass");
        assertTrue(output.contains("tests 3"), "All receipt controls executed");
    }
}
