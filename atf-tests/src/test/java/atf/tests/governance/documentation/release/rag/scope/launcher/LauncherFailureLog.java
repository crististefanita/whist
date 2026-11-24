package atf.tests.governance.documentation.release.rag.scope.launcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/** Copies bounded combined output into the failure before fixture cleanup. */
public final class LauncherFailureLog {
    private static final int LIMIT = 64 * 1024;
    private LauncherFailureLog() { }

    public static String timeoutMessage(Path file, Duration elapsed) {
        String identity = "Launcher timed out: " + file + "; elapsed=" + elapsed;
        try (var input = FileChannel.open(file, StandardOpenOption.READ)) {
            long observed = input.size();
            input.position(Math.max(0, observed - LIMIT));
            var bytes = ByteBuffer.allocate((int) Math.min(observed, LIMIT));
            while (bytes.hasRemaining() && input.read(bytes) > 0) { }
            bytes.flip();
            byte[] captured = new byte[bytes.remaining()];
            bytes.get(captured);
            return identity + "; observedBytes=" + observed + "; truncated=" + (observed > LIMIT)
                    + "\nCombined stdout/stderr:\n" + tail(captured, LIMIT);
        } catch (IOException failure) {
            return identity + "\nDiagnostic capture failed: " + failure.getClass().getSimpleName() + ": " + failure.getMessage();
        }
    }

    static String tail(byte[] bytes, int limit) {
        if (limit < 1) throw new IllegalArgumentException("positive diagnostic limit required");
        String text = new String(bytes, Math.max(0, bytes.length - limit), Math.min(bytes.length, limit), StandardCharsets.UTF_8);
        while (text.getBytes(StandardCharsets.UTF_8).length > limit)
            text = text.substring(text.offsetByCodePoints(0, 1));
        return text;
    }
}
