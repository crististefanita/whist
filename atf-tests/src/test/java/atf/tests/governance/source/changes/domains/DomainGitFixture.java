package atf.tests.governance.source.changes.domains;

import atf.android.process.CommandProcess;
import atf.tests.testchange.TestChangeReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real Git diffs and production selector admission, with nonexecuted component-symbol stubs. */
final class DomainGitFixture {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    final Path root;
    final String base;
    private final Map<String, String> original = new LinkedHashMap<>();
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final ObjectNode declarations;

    DomainGitFixture(Path root, ReviewedDomains domains) throws Exception {
        this.root = root;
        git("init", "-q", "-b", "main");
        git("config", "core.autocrlf", "false");
        git("config", "core.longpaths", "true");
        put("pom.xml", "<project/>\n");
        for (String id : ReviewedDomains.IDS) for (String path : domains.paths(id))
            put(path, path.endsWith(".json") ? "{}\n" : "// Controlled source boundary\n");
        var symbols = new LinkedHashMap<String, Set<String>>();
        for (String id : ReviewedDomains.IDS) for (String selector : domains.checks(id)) {
            String[] parts = selector.split("#");
            symbols.computeIfAbsent(parts[0], ignored -> new java.util.TreeSet<>()).add(parts[1]);
        }
        for (var symbol : symbols.entrySet()) {
            int dot = symbol.getKey().lastIndexOf('.');
            StringBuilder source = new StringBuilder("package ").append(symbol.getKey(), 0, dot)
                    .append(";\nimport atf.testkit.AtfComponentTest;\nimport org.junit.jupiter.api.Test;\n")
                    .append("@AtfComponentTest class ").append(symbol.getKey().substring(dot + 1)).append(" {\n");
            symbol.getValue().forEach(method -> source.append("@Test void ").append(method).append("() {}\n"));
            put("src/test/java/" + symbol.getKey().replace('.', '/') + ".java", source.append("}\n").toString());
        }
        commit("controlled domain inputs");
        base = git("rev-parse", "HEAD");
        declarations = yaml.createObjectNode();
        ArrayNode controls = declarations.putArray("testChanges");
        controls.add(domains.row(ReviewedDomains.LEGACY).deepCopy());
        for (String id : ReviewedDomains.IDS) {
            ObjectNode row = domains.row(id).deepCopy();
            row.put("base", base).put("expires", TODAY.plusDays(30).toString());
            controls.add(row);
        }
        put(ReviewedDomains.REGISTER, markdown());
        commit("review separate domain controls");
    }

    void changes(String... paths) throws Exception {
        for (var entry : original.entrySet()) put(entry.getKey(), entry.getValue());
        for (String path : paths) {
            String current = Files.readString(root.resolve(path));
            original.putIfAbsent(path, current);
            put(path, current + "\n// Controlled domain change\n");
        }
        commit("controlled diff");
    }

    List<String> selected() throws Exception {
        return TestChangeReview.requiredProofs(root, base, git("rev-parse", "HEAD"), TODAY);
    }
    List<String> checks(String id) throws Exception {
        var checks = TestChangeReview.proofPlan(root, base, id, TODAY).checks();
        return java.util.stream.Stream.concat(checks.positive().stream(), checks.negative().stream()).toList();
    }
    void duplicateUiOwner() throws Exception {
        ObjectNode duplicate = java.util.stream.StreamSupport.stream(declarations.path("testChanges").spliterator(), false)
                .filter(row -> row.path("id").asText().equals(ReviewedDomains.UI)).findFirst().orElseThrow().deepCopy();
        duplicate.put("id", "RAG-DOMAIN-DUPLICATE");
        ((ArrayNode) declarations.path("testChanges")).add(duplicate);
        put(ReviewedDomains.REGISTER, markdown());
        commit("controlled ambiguous owner");
    }
    private String markdown() throws Exception { return "```yaml\n" + yaml.writeValueAsString(declarations) + "```\n"; }
    private void put(String relative, String text) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
    private void commit(String reason) throws Exception {
        git("add", "-A");
        git("-c", "user.name=ATF fixture", "-c", "user.email=fixture@example.invalid",
                "-c", "commit.gpgSign=false", "commit", "-qm", reason);
    }
    private String git(String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(args));
        var result = CommandProcess.execute(command, Map.of("GIT_TERMINAL_PROMPT", "0"), Duration.ofSeconds(30), false);
        if (result.exitCode() != 0) throw new java.io.IOException("Fixture Git failed: " + result.stderr());
        return new String(result.stdout(), StandardCharsets.UTF_8).strip();
    }
}
