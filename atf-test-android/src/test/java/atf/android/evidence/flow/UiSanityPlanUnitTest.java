package atf.android.evidence.flow;

import atf.testkit.AtfUnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiSanityPlanUnitTest {
    private static final String PLAN = """
            {"schemaVersion":1,"id":"TABLE-FLOW","steps":["bid","score"],"captureSteps":["bid"],
             "optionalSteps":["notice"],"deals":[1],"maximumNormalCaptures":1,"maximumFailureCaptures":1}
            """;

    @Test void immutablePlanExecutesOnceAndStopsOnOriginalFailure() throws Exception {
        var plan = UiSanityPlan.parse(PLAN); var actions = new ArrayList<String>();
        var original = new IllegalStateException("original driver failure");
        assertSame(original, assertThrows(IllegalStateException.class, () -> plan.execute(step -> {
            actions.add(step); throw original;
        })));
        assertEquals(List.of("bid"), actions);
        assertThrows(UnsupportedOperationException.class, () -> plan.steps().add("extra"));
        assertEquals(plan, UiSanityPlan.parse(plan.json()));
        assertEquals(plan, UiSanityPlan.parse(PLAN.replace("\"schemaVersion\":1", "\"schemaVersion\":1.0")));
    }

    @Test void malformedPlansCannotExpandTheHandOrCaptureBudgets() {
        for (String changed : List.of(PLAN.replace("[1]", "[1,1,1]"), PLAN.replace("[1]", "[1.5]"),
                PLAN.replace("[1]", "[2147483648]"), PLAN.replace("\"maximumNormalCaptures\":1", "\"maximumNormalCaptures\":5"),
                PLAN.replace("[\"bid\"]", "[\"missing\"]"), PLAN.replace("[\"notice\"]", "[\"failure\"]"),
                PLAN.replace("[\"bid\",\"score\"]", "[\"bid\",\"bid\"]"), PLAN.replace("\"schemaVersion\":1", "\"unknown\":1")))
            assertThrows(IllegalArgumentException.class, () -> UiSanityPlan.parse(changed), changed);
    }

    @Test void completeAndFailurePrefixesKeepOrderAndCaptureOwnership() throws Exception {
        var plan = UiSanityPlan.parse(PLAN);
        var complete = List.of(new UiSanityPlan.Frame("bid", "bid.png"), new UiSanityPlan.Frame("notice", null), new UiSanityPlan.Frame("score", null));
        assertDoesNotThrow(() -> plan.verify(complete, true));
        assertDoesNotThrow(() -> plan.verify(List.of(complete.getFirst(), new UiSanityPlan.Frame("failure", "failure.png")), false));
        for (var invalid : List.of(complete.subList(0, 1), List.of(new UiSanityPlan.Frame("bid", null)),
                List.of(complete.getFirst(), new UiSanityPlan.Frame("score", "extra.png")),
                List.of(complete.getFirst(), new UiSanityPlan.Frame("failure", "bid.png"))))
            assertThrows(IllegalArgumentException.class, () -> plan.verify(invalid, true));
        assertThrows(IllegalArgumentException.class, () -> plan.verify(List.of(complete.getFirst(),
                new UiSanityPlan.Frame("failure", "failed.png"), new UiSanityPlan.Frame("failure", "second.png")), false));
    }
}
