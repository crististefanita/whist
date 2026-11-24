package atf.tests.execution.proofs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

/** Compiled discovery must admit exactly one native JUnit identity for every static selector. */
final class StaticProofSelectors {
    static LauncherDiscoveryRequestBuilder request(List<String> expected) throws ClassNotFoundException {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String selector : expected) {
            var parts = selector.split("#", 2);
            Class<?> owner = Class.forName(parts[0]);
            var methods = Stream.of(owner.getDeclaredMethods()).filter(method -> method.getName().equals(parts[1])).toList();
            if (methods.size() != 1) throw invalid(selector);
            request.selectors(selectMethod(owner, methods.getFirst()));
        }
        return configure(request);
    }

    static LauncherDiscoveryRequestBuilder configure(LauncherDiscoveryRequestBuilder request) {
        return request.filters(EngineFilter.includeEngines("junit-jupiter"), TagFilter.includeTags("component"))
                .configurationParameter("atf.component.aggregate", "true")
                .configurationParameter("atf.test.run.scope", "suite")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
    }

    static Map<String, String> identities(TestPlan discovered, List<String> expected) {
        var ids = new LinkedHashMap<String, String>();
        for (var root : discovered.getRoots()) for (var test : discovered.getDescendants(root)) {
            if (!test.isTest()) continue;
            if (!(test.getSource().orElse(null) instanceof MethodSource method)) throw invalid(test.getUniqueId());
            String selector = method.getClassName() + "#" + method.getMethodName();
            if (ids.putIfAbsent(selector, test.getUniqueId()) != null) throw invalid(selector);
        }
        if (!ids.keySet().equals(Set.copyOf(expected))) throw invalid(ids.keySet().toString());
        return Map.copyOf(ids);
    }

    private static IllegalStateException invalid(String detail) {
        return new IllegalStateException("TEST-CHANGE-PROOF-SELECTOR: " + detail);
    }

    private StaticProofSelectors() { }
}
