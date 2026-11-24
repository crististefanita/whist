package atf.android.evidence.component.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Verifies private output before replacing the canonical report; failures retain their stage. */
public final class ComponentReportPublication {
    @FunctionalInterface public interface Operation { void run(Path report) throws IOException; }
    private static final ObjectMapper JSON = new ObjectMapper();
    public static final String MAX_BYTES = "atf.component.report-max-bytes";
    public static final long DEFAULT_MAX_BYTES = 64L * 1024 * 1024;
    private ComponentReportPublication() { }

    public static void publish(Path target, long maximumBytes, Operation render, Operation verify) throws IOException {
        if (maximumBytes < 1) throw new IllegalArgumentException("COMPONENT_REPORT_LIMIT_REQUIRED");
        Path canonical = target.toAbsolutePath().normalize();
        Files.createDirectories(canonical.getParent());
        Path staging = Files.createTempDirectory(canonical.getParent(), ".component-publication-");
        Path pending = staging.resolve(canonical.getFileName());
        Path receipt = canonical.resolveSibling(canonical.getFileName() + ".publication.json");
        var journal = new LinkedHashMap<String,Object>();
        journal.put("kind", "COMPONENT_REPORT_PUBLICATION"); journal.put("canonical", canonical.toString());
        journal.put("staged", pending.toString()); journal.put("maximumBytes", maximumBytes);
        journal.put("maximumHeapBytes", Runtime.getRuntime().maxMemory());
        var stages = new ArrayList<Map<String,Object>>(); journal.put("stages", stages);
        String[] active = {"RENDER_AND_FINALIZE"};
        try {
            phase(active[0], receipt, journal, stages, () -> render.run(pending));
            active[0] = "VERIFY";
            phase(active[0], receipt, journal, stages, () -> {
                if (!Files.isRegularFile(pending) || Files.isSymbolicLink(pending))
                    throw new IOException("COMPONENT_REPORT_STAGED_FILE_REQUIRED");
                long bytes = Files.size(pending); journal.put("bytes", bytes);
                if (bytes > maximumBytes) throw new IOException("COMPONENT_REPORT_BYTE_LIMIT: " + bytes + " > " + maximumBytes);
                String original = digest(pending);
                verify.run(pending);
                if (!original.equals(digest(pending))) throw new IOException("COMPONENT_REPORT_CHANGED_DURING_VERIFICATION");
                journal.put("sha256", original);
            });
            active[0] = "PROMOTE";
            phase(active[0], receipt, journal, stages, () -> {
                atf.reporting.publication.ReportPublicationCoordinator.publishAtomic(pending, canonical);
            });
            journal.put("status", "PASS"); write(receipt, journal);
            try { Files.delete(staging); } catch (IOException cleanup) {
                journal.put("cleanupFailure", cleanup.getClass().getName()); write(receipt,journal);
            }
        } catch (IOException | RuntimeException | Error failure) {
            try {
                journal.put("status", "FAIL"); journal.put("failedStage", active[0]);
                journal.put("causeType", failure.getClass().getName()); write(receipt, journal);
            } catch (IOException | RuntimeException | Error diagnostic) { failure.addSuppressed(diagnostic); }
            throw failure;
        }
    }
    private interface Work { void run() throws IOException; }
    private static void phase(String name, Path receipt, Map<String,Object> journal,
            List<Map<String,Object>> stages, Work work) throws IOException {
        var phase = new LinkedHashMap<String,Object>(); phase.put("name", name); phase.put("status", "STARTED");
        phase.put("heapUsedBefore", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        stages.add(phase); write(receipt, journal); long started = System.nanoTime();
        Throwable original = null;
        try { work.run(); phase.put("status", "PASS"); }
        catch (IOException | RuntimeException | Error failure) { original=failure; phase.put("status","FAIL"); throw failure; }
        finally {
            try {
                phase.put("elapsedNanos", System.nanoTime()-started);
                phase.put("heapUsedAfter", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                phase.put("cumulativePoolPeakUpperBound", ManagementFactory.getMemoryPoolMXBeans().stream()
                        .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
                        .mapToLong(pool -> pool.getPeakUsage().getUsed()).sum());
            } catch (RuntimeException | Error diagnostic) {
                if (original != null) original.addSuppressed(diagnostic); else throw diagnostic;
            }
        }
    }
    private static void write(Path receipt, Map<String,Object> value) throws IOException {
        Path pending = Files.createTempFile(receipt.getParent(), "publication-receipt-", ".writing");
        try {
            JSON.writerWithDefaultPrettyPrinter().writeValue(pending.toFile(), value);
            Files.move(pending, receipt, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(pending); }
    }
    private static String digest(Path report) throws IOException {
        try (var input = Files.newInputStream(report)) {
            var digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[16384]; int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer,0,count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
