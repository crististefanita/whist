package atf.tests.execution.recovery;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.suite.api.*;

/** Controlled child-JVM fixture; counters independently observe which bodies actually executed. */
final class RecoveryFixture {
    @Suite @SelectClasses({Cases.class}) @IncludeEngines("junit-jupiter") @IncludeTags("component")
    @ConfigurationParameter(key="atf.component.aggregate",value="true")
    @ConfigurationParameter(key="junit.jupiter.extensions.autodetection.enabled",value="true")
    public static class ControlledSuite { }
    @Suite @SelectClasses({InterruptedCases.class}) @IncludeEngines("junit-jupiter") @IncludeTags("component")
    @ConfigurationParameter(key="atf.component.aggregate",value="true")
    @ConfigurationParameter(key="junit.jupiter.extensions.autodetection.enabled",value="true")
    public static class InterruptedSuite { }
    @AtfComponentTest @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class InterruptedCases {
        @Test @Order(1) void completed() throws Exception { Cases.observed("completed");AuditedAssertions.assertTrue(true,"The completed body survives process interruption"); }
        @Test @Order(2) void interrupted() throws Exception {
            if(!Cases.observed("interrupted")) Runtime.getRuntime().halt(23);
            AuditedAssertions.assertTrue(true,"Only the interrupted body is resumed");
        }
        @Test @Order(3) void pending() throws Exception { Cases.observed("pending");AuditedAssertions.assertTrue(true,"The previously unseen static body executes once"); }
    }
    @AtfComponentTest public static class Cases {
        private static boolean observed(String id) throws Exception {
            Path root=Path.of(System.getProperty("atf.recovery.fixture"));
            Files.writeString(root.resolve(id+".count"),"executed\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            return Files.exists(root.resolve("allow-pass"));
        }
        @Test void alwaysPass() throws Exception { observed("pass");AuditedAssertions.assertEquals("retained value","retained value","The original successful comparison remains unchanged"); }
        @Test void controlledFailure() throws Exception { AuditedAssertions.assertTrue(observed("failure"),"Only the failing fixture changes on retry"); }
        @ParameterizedTest @ValueSource(ints={0,1}) void parameters(int value) throws Exception {
            boolean allowed=observed("parameter-"+value);AuditedAssertions.assertTrue(value==0 || allowed,"Parameter IDs remain distinct even when only one fails");
        }
        @Test void controlledAbort() throws Exception {
            boolean allowed=observed("abort");Assumptions.assumeTrue(allowed,"Controlled aborted invocation");
            AuditedAssertions.assertTrue(allowed,"The resumed invocation has an evaluated body");
        }
        @Nested class Child {
            @Test void nestedPass() throws Exception {observed("nested");AuditedAssertions.assertTrue(true,"Nested identity keeps its original source and comparison");}
        }
    }
    private RecoveryFixture() { }
}
