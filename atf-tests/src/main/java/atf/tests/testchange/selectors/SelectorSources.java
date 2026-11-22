package atf.tests.testchange.selectors;

import atf.tests.jvm.AtfJavaSourceInventory;
import atf.tests.jvm.JavaSourceParser;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Literal Maven roots and syntax metadata, independent of compiled tests and their classpath. */
final class SelectorSources {
    record Context(String pkg, Map<String, List<String>> imports, List<String> wildcards) { }
    record Annotation(String name, String literal) { }
    record Method(String name, Set<Modifier> modifiers, String returns, boolean body, List<Annotation> annotations) { }
    record Type(String name, boolean test, Tree.Kind kind, Set<Modifier> modifiers, Context context,
                String enclosing, String parent, List<Annotation> annotations, List<Method> methods) { }
    private final Map<String, List<Type>> types = new LinkedHashMap<>();

    SelectorSources(Path root) throws IOException {
        var files = new LinkedHashMap<Path, Boolean>();
        for (var source : AtfJavaSourceInventory.roots(root)) {
            for (Path file : AtfJavaSourceInventory.files(source.directory())) {
                if (files.putIfAbsent(file, source.test()) != null)
                    throw failure("AMBIGUOUS", "overlapping source ownership: " + root.relativize(file));
            }
        }
        try (var parser = new JavaSourceParser()) {
            for (var unit : parser.parseFiles(List.copyOf(files.keySet()))) {
                boolean test = Boolean.TRUE.equals(files.get(Path.of(unit.getSourceFile().toUri())));
                Context context = context(unit);
                for (var tree : unit.getTypeDecls()) if (tree instanceof ClassTree type) collect(type, context, test, "");
            }
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            throw failure("SOURCE", invalid.getMessage());
        }
    }

    Type type(String name) throws IOException {
        var candidates = types.getOrDefault(name, List.of());
        if (candidates.size() != 1) throw failure(candidates.isEmpty() ? "MISSING" : "AMBIGUOUS", "type " + name);
        return candidates.getFirst();
    }

    boolean contains(String name) { return types.containsKey(name); }

    String resolve(Context context, String name, Set<String> known) throws IOException {
        if (name.contains(".")) return name;
        var explicit = context.imports().getOrDefault(name, List.of()).stream().distinct().toList();
        if (explicit.size() > 1) throw failure("AMBIGUOUS", "annotation import " + name);
        if (!explicit.isEmpty()) return explicit.getFirst();
        String local = context.pkg().isEmpty() ? name : context.pkg() + "." + name;
        if (contains(local)) return local;
        var candidates = new ArrayList<String>();
        for (String prefix : context.wildcards()) {
            String candidate = prefix + "." + name;
            if (known.contains(candidate) || contains(candidate)) candidates.add(candidate);
        }
        if (known.contains("java.lang." + name)) candidates.add("java.lang." + name);
        if (candidates.stream().distinct().count() > 1) throw failure("AMBIGUOUS", "wildcard annotation " + name);
        if (candidates.isEmpty()) throw failure("ANNOTATION", "unresolved annotation " + name);
        for (String prefix : context.wildcards()) {
            if (!prefix.startsWith("java.") && known.stream().noneMatch(value -> value.startsWith(prefix + "."))
                    && types.keySet().stream().noneMatch(value -> value.startsWith(prefix + ".")))
                throw failure("AMBIGUOUS", "opaque wildcard " + prefix + " for " + name + "; use an explicit import");
        }
        return candidates.getFirst();
    }

    private static Context context(CompilationUnitTree unit) {
        var imports = new LinkedHashMap<String, List<String>>();
        var wildcards = new ArrayList<String>();
        for (var entry : unit.getImports()) {
            if (entry.isStatic()) continue;
            String name = entry.getQualifiedIdentifier().toString();
            if (name.endsWith(".*")) wildcards.add(name.substring(0, name.length() - 2));
            else imports.computeIfAbsent(name.substring(name.lastIndexOf('.') + 1), ignored -> new ArrayList<>()).add(name);
        }
        return new Context(unit.getPackageName() == null ? "" : unit.getPackageName().toString(), imports, List.copyOf(wildcards));
    }

    private void collect(ClassTree tree, Context context, boolean test, String enclosing) {
        String name = enclosing.isEmpty() ? (context.pkg().isEmpty() ? "" : context.pkg() + ".") + tree.getSimpleName()
                : enclosing + "$" + tree.getSimpleName();
        var methods = new ArrayList<Method>();
        for (var member : tree.getMembers()) if (member instanceof MethodTree method) {
            methods.add(new Method(method.getName().toString(), Set.copyOf(method.getModifiers().getFlags()),
                    method.getReturnType() == null ? "" : method.getReturnType().toString(), method.getBody() != null,
                    annotations(method.getModifiers().getAnnotations())));
        }
        var parent = tree.getExtendsClause();
        String parentName = parent == null ? "" : parent instanceof ParameterizedTypeTree parameterized
                ? parameterized.getType().toString() : parent.toString();
        var type = new Type(name, test, tree.getKind(), Set.copyOf(tree.getModifiers().getFlags()), context, enclosing,
                parentName, annotations(tree.getModifiers().getAnnotations()), List.copyOf(methods));
        types.computeIfAbsent(name, ignored -> new ArrayList<>()).add(type);
        for (var member : tree.getMembers()) if (member instanceof ClassTree nested) collect(nested, context, test, name);
    }

    private static List<Annotation> annotations(List<? extends AnnotationTree> values) {
        return values.stream().map(value -> {
            String literal = "";
            if (value.getArguments().size() == 1) {
                var argument = value.getArguments().getFirst();
                var expression = argument instanceof AssignmentTree assignment ? assignment.getExpression() : argument;
                if (expression instanceof LiteralTree text && text.getValue() instanceof String string) literal = string;
            }
            return new Annotation(value.getAnnotationType().toString(), literal);
        }).toList();
    }

    static IOException failure(String code, String detail) { return new IOException("TEST-CHANGE-SELECTOR-" + code + ": " + detail); }
}
