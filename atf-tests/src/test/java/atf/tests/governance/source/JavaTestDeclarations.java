package atf.tests.governance.source;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import atf.tests.jvm.JavaSourceParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Syntax-only JDK parsing; no compilation, annotation processing or application-class loading. */
final class JavaTestDeclarations {
    record Type(String name, Tree.Kind kind, Set<Modifier> modifiers, List<String> annotations,
                List<List<String>> methods, List<Type> nested, String annotationSource,
                Map<String, List<String>> selectors) {
        boolean concrete() {
            return (kind == Tree.Kind.CLASS || kind == Tree.Kind.RECORD || kind == Tree.Kind.ENUM)
                    && !modifiers.contains(Modifier.ABSTRACT);
        }
    }

    record Unit(String packageName, Map<String, String> imports, List<String> wildcards,
                String header, List<Type> types) {
        String qualified(String name, Map<String, String> known) {
            if (name.contains(".")) return name;
            if (imports.containsKey(name)) return imports.get(name);
            String local = packageName.isEmpty() ? name : packageName + "." + name;
            if (known.containsKey(local)) return local;
            var matches = wildcards.stream().map(prefix -> prefix + "." + name).filter(known::containsKey).distinct().toList();
            if (matches.size() > 1) throw new IllegalArgumentException("GOV-ANNOTATION-AMBIGUOUS: " + name);
            return matches.isEmpty() ? local : matches.getFirst();
        }
    }

    private JavaTestDeclarations() { }

    static Unit parse(String source) {
        try (var parser = open()) { return parser.parse(source); }
    }

    static Parser open() { return new Parser(); }

    /** One inspection owns the file manager; each source still gets a fresh task and diagnostics. */
    static final class Parser implements AutoCloseable {
        private final JavaSourceParser parser = new JavaSourceParser();

        Unit parse(String source) {
            return describe(parser.parse(source));
        }

        @Override public void close() {
            parser.close();
        }
    }

    private static Unit describe(CompilationUnitTree tree) {
        String owner = tree.getPackageName() == null ? "" : tree.getPackageName().toString();
        var imports = new LinkedHashMap<String, String>();
        var wildcards = new ArrayList<String>();
        StringBuilder header = new StringBuilder(owner.isEmpty() ? "" : "package " + owner + ";\n");
        for (var declaration : tree.getImports()) {
            String name = declaration.getQualifiedIdentifier().toString();
            header.append(declaration).append('\n');
            if (name.endsWith(".*")) wildcards.add(name.substring(0, name.length() - 2));
            else imports.put(name.substring(name.lastIndexOf('.') + 1), name);
        }
        var types = tree.getTypeDecls().stream().filter(ClassTree.class::isInstance)
                .map(ClassTree.class::cast).map(JavaTestDeclarations::describe).toList();
        return new Unit(owner, Map.copyOf(imports), List.copyOf(wildcards), header.toString(), types);
    }

    private static Type describe(ClassTree tree) {
        var annotations = tree.getModifiers().getAnnotations().stream().map(value -> value.getAnnotationType().toString()).toList();
        var methods = new ArrayList<List<String>>();
        var nested = new ArrayList<Type>();
        for (var member : tree.getMembers()) {
            if (member instanceof MethodTree method)
                methods.add(method.getModifiers().getAnnotations().stream().map(value -> value.getAnnotationType().toString()).toList());
            else if (member instanceof ClassTree type) nested.add(describe(type));
        }
        return new Type(tree.getSimpleName().toString(), tree.getKind(), Set.copyOf(tree.getModifiers().getFlags()),
                annotations, List.copyOf(methods), List.copyOf(nested), tree.getKind() == Tree.Kind.ANNOTATION_TYPE ? tree.toString() : "",
                selectors(tree));
    }

    private static Map<String, List<String>> selectors(ClassTree tree) {
        var result = new LinkedHashMap<String, List<String>>();
        for (var annotation : tree.getModifiers().getAnnotations()) {
            String name = annotation.getAnnotationType().toString().replaceAll(".*\\.", "");
            if (!Set.of("SelectPackages", "SelectClasses", "IncludeTags", "IncludeEngines",
                    "IncludeClassNamePatterns", "ExcludeTags", "ExcludePackages", "ExcludeClassNamePatterns").contains(name)) continue;
            var values = new ArrayList<String>();
            for (var argument : annotation.getArguments()) selectorValues(argument, values);
            if (result.put(name, List.copyOf(values)) != null) throw new IllegalArgumentException("Duplicate suite selector: " + name);
        }
        return Map.copyOf(result);
    }
    private static void selectorValues(com.sun.source.tree.ExpressionTree expression, List<String> values) {
        if (expression instanceof com.sun.source.tree.AssignmentTree named) selectorValues(named.getExpression(), values);
        else if (expression instanceof com.sun.source.tree.NewArrayTree array) {
            if (array.getInitializers() != null) array.getInitializers().forEach(value -> selectorValues(value, values));
        } else if (expression instanceof com.sun.source.tree.LiteralTree literal && literal.getValue() instanceof String value) values.add(value);
        else if (expression instanceof com.sun.source.tree.MemberSelectTree member && member.getIdentifier().contentEquals("class")) values.add(member.getExpression().toString());
        else throw new IllegalArgumentException("Suite selector requires explicit string or class literals: " + expression);
    }
}
