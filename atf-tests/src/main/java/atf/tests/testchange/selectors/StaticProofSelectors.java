package atf.tests.testchange.selectors;

import com.sun.source.tree.Tree;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Source eligibility only; compiled JUnit discovery and original proof verdicts remain mandatory. */
public final class StaticProofSelectors {
    private static final String TEST = "org.junit.jupiter.api.Test";
    private static final String COMPONENT = "atf.testkit.AtfComponentTest";
    private static final String NESTED = "org.junit.jupiter.api.Nested";
    private static final Set<String> DYNAMIC = Set.of("org.junit.jupiter.api.TestFactory", "org.junit.jupiter.api.TestTemplate",
            "org.junit.jupiter.api.RepeatedTest", "org.junit.jupiter.params.ParameterizedTest");
    private static final Set<String> SAFE = Set.of("java.lang.Override", "java.lang.SuppressWarnings", "java.lang.Deprecated",
            "java.lang.SafeVarargs", "java.lang.annotation.Target", "java.lang.annotation.Retention",
            "java.lang.annotation.Inherited", "java.lang.annotation.Documented", "java.lang.annotation.Repeatable",
            "org.junit.jupiter.api.DisplayName", "org.junit.jupiter.api.Timeout", "org.junit.jupiter.api.Disabled",
            "org.junit.jupiter.api.Order", "org.junit.jupiter.api.Tag", "org.junit.jupiter.api.Tags",
            "org.junit.jupiter.api.TestInstance", "org.junit.jupiter.api.TestMethodOrder", "org.junit.jupiter.api.TestClassOrder",
            "org.junit.jupiter.api.extension.ExtendWith", "org.junit.jupiter.api.parallel.Execution",
            "atf.testkit.AtfUnitTest", "atf.testkit.AtfIntegrationTest", "atf.testkit.AtfE2eTest");
    private static final Set<String> KNOWN;
    static {
        var names = new HashSet<>(SAFE); names.addAll(DYNAMIC); names.addAll(Set.of(TEST, COMPONENT, NESTED));
        KNOWN = Set.copyOf(names);
    }
    private StaticProofSelectors() { }

    public static void validate(Path root, List<String> selectors) throws IOException {
        if (selectors.isEmpty()) return;
        SelectorSources sources;
        try { sources = new SelectorSources(root); }
        catch (IOException invalid) { throw new IOException(invalid.getMessage() + " " + selectors, invalid); }
        for (String selector : selectors) {
            var parts = selector.split("#", -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) throw SelectorSources.failure("SHAPE", selector);
            try { validate(sources, sources.type(parts[0]), parts[1]); }
            catch (IOException invalid) { throw new IOException(invalid.getMessage() + " [" + selector + "]", invalid); }
        }
    }

    private static void validate(SelectorSources sources, SelectorSources.Type type, String method) throws IOException {
        if (!type.test() || !Set.of(Tree.Kind.CLASS, Tree.Kind.RECORD, Tree.Kind.ENUM).contains(type.kind())
                || type.modifiers().contains(Modifier.ABSTRACT) || type.modifiers().contains(Modifier.PRIVATE))
            throw SelectorSources.failure("SHAPE", "expected a concrete Maven test class " + type.name());
        var candidates = type.methods().stream().filter(value -> value.name().equals(method)).toList();
        if (candidates.size() != 1) throw SelectorSources.failure(candidates.isEmpty() ? "MISSING" : "AMBIGUOUS", "declared method " + method);
        var target = candidates.getFirst();
        var flags = flags(sources, type, target.annotations(), new HashSet<>());
        if (flags.contains("DYNAMIC")) throw SelectorSources.failure("DYNAMIC", "proof requires one static JUnit invocation");
        if (!flags.contains("TEST") || !target.returns().equals("void") || !target.body()
                || target.modifiers().stream().anyMatch(Set.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.ABSTRACT, Modifier.NATIVE)::contains))
            throw SelectorSources.failure("SHAPE", "expected an instance void @Test method " + method);
        if (!component(sources, type, new HashSet<>())) throw SelectorSources.failure("SHAPE", "missing component scope " + type.name());
    }

    private static boolean component(SelectorSources sources, SelectorSources.Type type, Set<String> visited) throws IOException {
        if (!visited.add(type.name())) throw SelectorSources.failure("AMBIGUOUS", "cyclic test scope " + type.name());
        var flags = flags(sources, type, type.annotations(), new HashSet<>());
        if (flags.contains("COMPONENT")) return true;
        if (!type.enclosing().isEmpty() && !type.modifiers().contains(Modifier.STATIC) && flags.contains("NESTED"))
            return component(sources, sources.type(type.enclosing()), visited);
        if (!type.parent().isEmpty()) {
            String parent = sources.resolve(type.context(), type.parent(), KNOWN);
            if (sources.contains(parent)) return component(sources, sources.type(parent), visited);
        }
        return false;
    }

    private static Set<String> flags(SelectorSources sources, SelectorSources.Type owner,
                                      List<SelectorSources.Annotation> annotations, Set<String> visiting) throws IOException {
        var result = new HashSet<String>();
        for (var annotation : annotations) {
            String name = sources.resolve(owner.context(), annotation.name(), KNOWN);
            if (KNOWN.contains(name) && sources.contains(name)) throw SelectorSources.failure("AMBIGUOUS", "shadowed canonical annotation " + name);
            if (name.equals(TEST)) result.add("TEST");
            else if (name.equals(COMPONENT) || name.equals("org.junit.jupiter.api.Tag") && annotation.literal().equals("component")) result.add("COMPONENT");
            else if (name.equals(NESTED)) result.add("NESTED");
            else if (DYNAMIC.contains(name)) result.add("DYNAMIC");
            else if (!SAFE.contains(name) && !name.startsWith("org.junit.jupiter.api.condition.")
                    && !name.startsWith("org.junit.jupiter.params.provider.")) {
                if (!sources.contains(name)) throw SelectorSources.failure("ANNOTATION", "opaque annotation " + name);
                if (!visiting.add(name)) throw SelectorSources.failure("AMBIGUOUS", "annotation cycle " + name);
                var composed = sources.type(name);
                if (composed.kind() != Tree.Kind.ANNOTATION_TYPE) throw SelectorSources.failure("ANNOTATION", name);
                result.addAll(flags(sources, composed, composed.annotations(), visiting));
                visiting.remove(name);
            }
        }
        return result;
    }
}
