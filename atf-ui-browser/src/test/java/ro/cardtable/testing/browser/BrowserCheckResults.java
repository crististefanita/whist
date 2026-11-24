package ro.cardtable.testing.browser;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Writes the original result schema, including a failed or interrupted selection's completed prefix. */
public final class BrowserCheckResults implements AutoCloseable {
    public record Image(String file, String title) { }
    public record Result(String id, String name, String status, String expected, Object observed, List<Image> images) {
        public Result { images = List.copyOf(images); }
    }
    private final Path file;
    private final List<Result> results = new ArrayList<>();
    private boolean closed;
    public BrowserCheckResults(Path output) throws IOException {
        file = output.resolve("browser-ui-checks.json");
        Files.createFile(file);
    }
    public void add(Result result) {
        if (closed) throw new IllegalStateException("Browser results are already published");
        if (results.stream().anyMatch(prior -> prior.id().equals(result.id()))) throw new IllegalArgumentException("Duplicate browser case: " + result.id());
        results.add(result);
    }
    public boolean failed() { return results.isEmpty() || results.stream().anyMatch(result -> !result.status().equals("PASS")); }
    @Override public void close() throws IOException {
        if (closed) return;
        Files.writeString(file, new Gson().toJson(results), StandardOpenOption.TRUNCATE_EXISTING);
        closed = true;
    }
}
