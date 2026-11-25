package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.publication.ComponentReportPublication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Real bounded JVM failure at the consumer publication boundary, not a library allocation benchmark. */
public final class PublicationFailureProbe {
    private PublicationFailureProbe() { }
    public static void main(String[] arguments) throws Exception {
        Path report = Path.of(arguments[0]); String mode = arguments[1];
        ComponentReportPublication.publish(report, 1024,
                pending -> { Files.writeString(pending, "complete candidate"); fail(mode, "render"); },
                pending -> { fail(mode, "verify"); });
    }
    private static void fail(String mode, String stage) {
        if (mode.equals("halt-" + stage)) Runtime.getRuntime().halt(37);
        if (mode.equals("heap-" + stage)) {
            var retained = new ArrayList<byte[]>();
            while (true) retained.add(new byte[1024 * 1024]);
        }
    }
}
