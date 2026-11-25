package atf.android.e2e.driver.actions;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Measures command dispatch independently of target selection, retries and UI settling. */
public final class AndroidUiActions {
    @FunctionalInterface public interface Command {
        void run(String... arguments) throws Exception;
    }

    private final Command command;
    private final LongSupplier ticks;
    private Consumer<AtfUiActionRecord> sink = ignored -> { };
    private int sequence;

    public AndroidUiActions(Command command, LongSupplier ticks) {
        this.command = Objects.requireNonNull(command);
        this.ticks = Objects.requireNonNull(ticks);
    }

    public void recordAs(Consumer<AtfUiActionRecord> sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public void dispatch(String operation, String target, String expected, String accepted, String... arguments)
            throws Exception {
        long started = ticks.getAsLong();
        try {
            command.run(arguments);
        } catch (Exception failure) {
            try { record(operation, target, expected, failure.toString(), elapsed(started), AtfReportStatus.FAIL); }
            catch (Exception reportingFailure) {
                if (reportingFailure != failure) failure.addSuppressed(reportingFailure);
            }
            throw failure;
        }
        record(operation, target, expected, accepted, elapsed(started), AtfReportStatus.PASS);
    }

    private long elapsed(long started) { return Duration.ofNanos(ticks.getAsLong() - started).toMillis(); }

    public void record(String operation, String target, String expected, String observed, long durationMillis,
            AtfReportStatus status) {
        sink.accept(new AtfUiActionRecord(++sequence, operation, target, expected, observed,
                durationMillis, status, AtfUiEvidenceSource.ACTION));
    }
}
