package atf.android.e2e.support.flow;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import java.util.List;
import java.util.Objects;

/** One public Reporting budget shared by live checkpoint admission and publication. */
public final class CheckpointAssertionPolicy {
    private CheckpointAssertionPolicy() { }

    public static void validate(List<AtfAssertion> assertions, ReportingSettings settings) {
        int limit = Objects.requireNonNull(settings, "settings").assertionMaxDisplayCharacters();
        for (var assertion : Objects.requireNonNull(assertions, "assertions")) {
            check(assertion.expected(), "expected", assertion.name(), limit);
            check(assertion.actual(), "actual", assertion.name(), limit);
        }
    }

    private static void check(String value, String field, String name, int limit) {
        if (value.length() > limit) throw new IllegalStateException(
                "Assertion " + field + " value exceeds Extent's configured " + limit
                        + " character preview: " + name + "; observed " + value.length()
                        + " UTF-16 units; put complete diagnostic values in the expandable observation table");
    }
}
