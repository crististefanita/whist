package atf.tests.jvm;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Syntax-only JDK parsing; never analyzes types, processes annotations or loads application classes. */
public final class JavaSourceParser implements AutoCloseable {
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final StandardJavaFileManager manager;
    private boolean closed;

    public JavaSourceParser() {
        if (compiler == null) throw new IllegalStateException("GOV-JDK: source governance requires a full JDK");
        manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
    }

    public CompilationUnitTree parse(String source) {
        var input = new SimpleJavaFileObject(URI.create("string:///Governed.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        return parseInputs(List.of(input)).getFirst();
    }

    public List<CompilationUnitTree> parseFiles(List<Path> files) {
        if (files.isEmpty()) return List.of();
        return parseInputs(manager.getJavaFileObjectsFromPaths(files));
    }

    private List<CompilationUnitTree> parseInputs(Iterable<? extends JavaFileObject> inputs) {
        if (closed) throw new IllegalStateException("GOV-JAVA-PARSE: parser is closed");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--class-path", "", "--processor-path", ""), null, inputs);
            var result = new ArrayList<CompilationUnitTree>();
            task.parse().forEach(result::add);
            var error = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).findFirst();
            if (error.isPresent()) {
                var first = error.get();
                throw new IllegalArgumentException("GOV-JAVA-SYNTAX: line " + first.getLineNumber()
                        + ", column " + first.getColumnNumber() + ": " + first.getMessage(Locale.ROOT));
            }
            return List.copyOf(result);
        } catch (IOException failure) {
            throw new IllegalStateException("GOV-JAVA-PARSE: cannot inspect source", failure);
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try { manager.close(); }
        catch (IOException failure) { throw new IllegalStateException("GOV-JAVA-PARSE: cannot close parser", failure); }
    }
}
