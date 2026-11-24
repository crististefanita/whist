package atf.android.process;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertArrayEquals;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertInstanceOf;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

import atf.testkit.AtfComponentTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
@Timeout(60)
public class CommandProcessComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    private static final int WATCHDOG_SECONDS = 30;
    @Test void aLateProcessStartIsBoundedAndItsEventualProcessIsCleaned(
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch destroyed = new CountDownLatch(1);
        Process process = new StubProcess(new ByteArrayInputStream(new byte[0])) {
            @Override public Process destroyForcibly() { destroyed.countDown(); return this; }
        };
        inputs.add("Late-start precondition", "Controlled starter blocks on a latch; start=0 ns, budget=1 ns, clock=1 ns; cleanup acknowledgement has an independent 30 s watchdog");
        try {
            assertThrows(IOException.class, () -> CommandProcess.start(() -> {
                try { release.await(); }
                catch (InterruptedException failure) { throw new IOException(failure); }
                return process;
            }, Duration.ofNanos(1), 0, 1, () -> 1));
            release.countDown();
            assertTrue(destroyed.await(WATCHDOG_SECONDS, TimeUnit.SECONDS), "A late process must not escape after timeout");
        } finally { release.countDown(); }
    }
    @Test void separatesBinaryOutputAndKeepsNonzeroExit() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("output"), Map.of(), Duration.ofSeconds(30), false);
        assertEquals(7, result.exitCode());
        assertArrayEquals(new byte[] {(byte) 0xff, 0, 1, 13, 10}, result.stdout());
        assertEquals("separate-error", result.stderr());
        result.stdout()[0] = 0;
        assertEquals((byte) 0xff, result.stdout()[0]);
    }

    @Test void drainsBothFullPipesWithoutDeadlock() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("flood"), Map.of(), Duration.ofSeconds(30), false);
        assertEquals(0, result.exitCode());
        assertEquals(256 * 1024, result.stdout().length);
        assertEquals(256 * 1024, result.stderr().length());
    }

    @Test void mergedOutputUsesTheSameDeadline() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("output"), Map.of(), Duration.ofSeconds(30), true);
        assertEquals(7, result.exitCode());
        assertTrue(new String(result.stdout(), java.nio.charset.StandardCharsets.UTF_8).contains("separate-error"));
        assertEquals("", result.stderr());
    }

    @Test void blockedDrainCannotTurnProcessExitIntoAnUnboundedJoin(
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch reading = new CountDownLatch(1);
        AtomicLong ticks = new AtomicLong();
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException {
                reading.countDown();
                try { release.await(); return -1; }
                catch (InterruptedException failure) { throw new IOException(failure); }
            }
            @Override public void close() { release.countDown(); }
        }) {
            @Override public boolean waitFor(long time, TimeUnit unit) throws InterruptedException {
                if (!reading.await(WATCHDOG_SECONDS, TimeUnit.SECONDS)) throw new IllegalStateException("Drain fixture not ready");
                ticks.set(150);
                return true;
            }
        };
        inputs.add("Blocked-drain precondition", "Readiness latch precedes process exit; start=0 ns, budget=150 ns; exit advances clock to exactly 150 ns while stdout remains blocked");
        try {
            IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(
                    process, Duration.ofNanos(150), 0, 150, ticks::get));
            assertTrue(failure.getMessage().contains("timed out"));
            assertTrue(release.await(WATCHDOG_SECONDS, TimeUnit.SECONDS), "Cleanup closes the blocked drain after the exact clock boundary");
            assertTrue(process.destroyed);
        } finally { release.countDown(); }
    }

    @Test void completionBeforeTheDeadlineKeepsBothOutputsAndTheExitCode(
            atf.android.evidence.component.ComponentInputs inputs) throws Exception {
        CountDownLatch drained = new CountDownLatch(2);
        AtomicLong ticks = new AtomicLong();
        var output = new ByteArrayInputStream(new byte[] {42}) {
            @Override public byte[] readAllBytes() { byte[] value = super.readAllBytes(); drained.countDown(); return value; }
        };
        StubProcess process = new StubProcess(output) {
            @Override public InputStream getErrorStream() {
                return new ByteArrayInputStream(new byte[] {69}) {
                    @Override public byte[] readAllBytes() { byte[] value = super.readAllBytes(); drained.countDown(); return value; }
                };
            }
            @Override public boolean waitFor(long time, TimeUnit unit) throws InterruptedException {
                if (!drained.await(WATCHDOG_SECONDS, TimeUnit.SECONDS)) throw new IllegalStateException("Output fixture not ready");
                ticks.set(149);
                return true;
            }
            @Override public int exitValue() { return 7; }
        };
        inputs.add("Controlled transport clock", "start=0; deadline=150 ns; completion=149 ns; watchdog is not a latency oracle");
        var result = CommandProcess.collect(process, Duration.ofNanos(150), 0, 150, ticks::get);
        assertEquals(7, result.exitCode());
        assertArrayEquals(new byte[] {42}, result.stdout());
        assertEquals("E", result.stderr());
        assertFalse(process.destroyed);
    }

    @Test void startFailureKeepsTheOriginalCauseWithoutAClockRace() {
        IOException original = new IOException("controlled launch failure");
        IOException actual = assertThrows(IOException.class, () -> CommandProcess.start(
                () -> { throw original; }, Duration.ofSeconds(30), 0, TimeUnit.SECONDS.toNanos(30), () -> 0));
        assertSame(original, actual);
    }

    @Test void readFailureIsNotSilentlyReportedAsSuccessAndCleanupCannotMaskIt() {
        IOException original = new IOException("controlled read failure");
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException { throw original; }
        });
        process.failDestroy = true;
        IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(process,
                Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)));
        assertSame(original, failure.getCause());
        assertEquals("controlled cleanup failure", failure.getSuppressed()[0].getMessage());
    }

    @Test void aBlockedStdoutDoesNotHideTheOriginalStderrFailure() {
        CountDownLatch release = new CountDownLatch(1);
        IOException original = new IOException("controlled stderr failure");
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException {
                try { release.await(); return -1; }
                catch (InterruptedException failure) { throw new IOException(failure); }
            }
            @Override public void close() { release.countDown(); }
        }) {
            @Override public InputStream getErrorStream() {
                return new InputStream() { @Override public int read() throws IOException { throw original; } };
            }
        };
        try {
            IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(process,
                    Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)));
            assertSame(original, failure.getCause());
            assertTrue(process.destroyed);
        } finally { release.countDown(); }
    }

    @Test void interruptionKillsTheOwnedProcessAndKeepsTheInterruptFlag(@TempDir Path directory) throws Exception {
        Path ready = directory.resolve("ready");
        Process process = new ProcessBuilder(ProcessFixture.command("sleep", ready.toString())).start();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try { CommandProcess.collect(process, Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)); }
            catch (IOException | InterruptedException error) { failure.set(error); interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            ProcessFixture.awaitReady(process, ready);
            caller.start();
            caller.interrupt();
            caller.join(TimeUnit.SECONDS.toMillis(WATCHDOG_SECONDS));
            assertFalse(caller.isAlive(), "Interrupted caller must return without waiting for output EOF");
            assertInstanceOf(InterruptedException.class, failure.get());
            assertTrue(interrupted.get());
            assertTrue(process.waitFor(WATCHDOG_SECONDS, TimeUnit.SECONDS), "Owned process must terminate");
        } finally { process.destroyForcibly(); caller.interrupt(); }
    }

    @Test void timedOutTreeKillsDescendantHoldingThePipeAfterParentExit(@TempDir Path directory) throws Exception {
        Path pidFile = directory.resolve("child.pid");
        Path release = directory.resolve("parent-can-exit");
        Process process = new ProcessBuilder(ProcessFixture.command("tree", pidFile.toString(), release.toString())).start();
        ProcessHandle child = null;
        try {
            ProcessFixture.awaitReady(process, pidFile);
            child = ProcessHandle.of(Long.parseLong(Files.readString(pidFile))).orElseThrow();
            Process observed = ProcessFixture.exitAfterDescendantObservation(process, release);
            long start = System.nanoTime();
            assertThrows(IOException.class, () -> CommandProcess.collect(observed, Duration.ofSeconds(3),
                    start, TimeUnit.SECONDS.toNanos(3)));
            assertTrue(Files.exists(pidFile), "Fixture must actually create a descendant");
            if (child != null) {
                child.onExit().get(WATCHDOG_SECONDS, TimeUnit.SECONDS);
                assertFalse(child.isAlive());
            }
            assertFalse(process.isAlive());
        } finally { process.destroyForcibly(); if (child != null) child.destroyForcibly(); }
    }

    private static class StubProcess extends Process {
        private final InputStream input;
        private boolean destroyed;
        private boolean failDestroy;
        StubProcess(InputStream input) { this.input = input; }
        @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        @Override public InputStream getInputStream() { return input; }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int waitFor() { return 0; }
        @Override public boolean waitFor(long time, TimeUnit unit) throws InterruptedException { return true; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() { destroyed = true; }
        @Override public Process destroyForcibly() {
            destroyed = true;
            if (failDestroy) throw new IllegalStateException("controlled cleanup failure");
            return this;
        }
        @Override public Stream<ProcessHandle> descendants() { return Stream.empty(); }
    }
}
