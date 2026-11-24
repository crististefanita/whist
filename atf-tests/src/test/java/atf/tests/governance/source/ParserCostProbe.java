package atf.tests.governance.source;

import atf.tests.jvm.AtfJavaSourceInventory;
import atf.tests.jvm.JavaSourceParser;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Test-only paired experiment; timings are observations, never a pass/fail speed threshold. */
final class ParserCostProbe {
    record Input(String path, String text) { }
    record Trial(String mode, int round, long parseNanos, long totalNanos, int units, String astSha256) { }
    record Report(String javaRuntime, String vm, String os, int processors, String inventorySha256,
            List<Trial> trials) { }
    private ParserCostProbe() { }

    static List<Input> freeze(Path root) throws Exception {
        var inputs = new ArrayList<Input>();
        for (var file : AtfJavaSourceInventory.allSources(root))
            inputs.add(new Input(root.relativize(file).toString().replace('\\', '/'), Files.readString(file)));
        return List.copyOf(inputs);
    }

    static Report measure(List<Input> sources, int rounds) throws Exception {
        var inputs = List.copyOf(sources);
        if (inputs.isEmpty() || rounds < 1 || inputs.stream().map(Input::path).distinct().count() != inputs.size())
            throw new IllegalArgumentException("PARSER-COST: require a nonempty unique inventory and positive rounds");
        var inventory = MessageDigest.getInstance("SHA-256");
        for (var input : inputs) update(inventory, input.path(), input.text());
        // Both modes warm the same frozen source population; measured order alternates by pair.
        trial(inputs, "configured", -1);
        trial(inputs, "jdk-default-paths", -1);
        var trials = new ArrayList<Trial>();
        for (int round = 0; round < rounds; round++) {
            for (String mode : round % 2 == 0 ? List.of("configured", "jdk-default-paths")
                    : List.of("jdk-default-paths", "configured")) trials.add(trial(inputs, mode, round));
        }
        return new Report(System.getProperty("java.runtime.version"), System.getProperty("java.vm.name"),
                System.getProperty("os.name") + "/" + System.getProperty("os.arch"), Runtime.getRuntime().availableProcessors(),
                HexFormat.of().formatHex(inventory.digest()), List.copyOf(trials));
    }

    static Trial trial(List<Input> inputs, String mode, int round) throws Exception {
        if (!List.of("configured", "jdk-default-paths").contains(mode)) throw new IllegalArgumentException("PARSER-COST: unknown mode");
        long start = System.nanoTime(), parsing = 0;
        var digest = MessageDigest.getInstance("SHA-256");
        try (var parser = mode.equals("configured") ? configured() : new DefaultPaths()) {
            for (var input : inputs) {
                long parseStart = System.nanoTime();
                var tree = parser.parse(input.text());
                parsing += System.nanoTime() - parseStart;
                update(digest, input.path(), tree.toString());
            }
        }
        return new Trial(mode, round, parsing, System.nanoTime() - start, inputs.size(), HexFormat.of().formatHex(digest.digest()));
    }

    private interface Reader extends AutoCloseable { CompilationUnitTree parse(String source) throws Exception; }

    private static Reader configured() {
        var parser = new JavaSourceParser();
        return new Reader() {
            @Override public CompilationUnitTree parse(String source) { return parser.parse(source); }
            @Override public void close() { parser.close(); }
        };
    }

    private static void update(MessageDigest digest, String path, String value) {
        digest.update(path.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
        digest.update(value.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
    }

    /** Reference differs only in compiler options; it also stops at parse(), before type analysis. */
    private static final class DefaultPaths implements Reader {
        private final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        private final javax.tools.StandardJavaFileManager manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
        @Override public CompilationUnitTree parse(String source) throws Exception {
            var input = new SimpleJavaFileObject(URI.create("string:///Governed.java"), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return source; }
            };
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of(), null, List.of(input));
            var tree = task.parse().iterator().next();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR))
                throw new IllegalArgumentException("GOV-JAVA-SYNTAX: reference parse rejected malformed source");
            return tree;
        }
        @Override public void close() throws Exception { manager.close(); }
    }
}
