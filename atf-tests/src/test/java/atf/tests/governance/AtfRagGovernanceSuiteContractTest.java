package atf.tests.governance;

import atf.testkit.AtfComponentTest;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class AtfRagGovernanceSuiteContractTest {
    @Test void theTargetedEntryPointSelectsOnlyOfflineReleaseGovernance() {
        Class<AtfRagGovernanceSuite> suite = AtfRagGovernanceSuite.class;
        assertTrue(suite.isAnnotationPresent(Suite.class), "The RAG entry point must remain a JUnit suite.");
        assertTrue(suite.isAnnotationPresent(AtfComponentTest.class), "The RAG entry point runs offline component checks.");
        assertEquals(List.of("atf.tests.governance"),
                List.of(suite.getAnnotation(SelectPackages.class).value()));
        var configuration = java.util.Arrays.stream(suite.getAnnotationsByType(ConfigurationParameter.class))
                .collect(java.util.stream.Collectors.toMap(ConfigurationParameter::key, ConfigurationParameter::value));
        assertEquals("suite", configuration.get("atf.test.run.scope"),
                "All audited tests in the bounded RAG entry point must publish under one suite identity.");
        assertEquals("true", configuration.get("atf.component.aggregate"),
                "Unadapted component checks must join the suite report through the shared audit adapter.");
        assertEquals("false", configuration.get("junit.jupiter.execution.parallel.enabled"),
                "Suite aggregation must be deterministic and must not race report publication.");
        var patterns = suite.getAnnotation(IncludeClassNamePatterns.class).value();
        assertEquals(1, patterns.length);
        assertEquals("^.*(?:AtfRagGovernanceSuiteContractTest|ReleaseRagPlatformGovernanceTest|"
                        + "ReleaseRagProvenanceComponentTest|ReleaseRagAndroidComponentTest|ReleaseRagGithubComponentTest|"
                        + "QualificationLauncherComponentTest|ComponentEvidenceScopeComponentTest|"
                        + "ReleasePublicationSealComponentTest|ReleaseSuiteReportingGovernanceTest)$",
                patterns[0], "The suite selector must remain a closed list rather than a broad name scan.");
        Pattern selected = Pattern.compile(patterns[0]);
        for (String required : List.of("AtfRagGovernanceSuiteContractTest", "ReleaseRagPlatformGovernanceTest",
                "ReleaseRagProvenanceComponentTest", "ReleaseRagAndroidComponentTest", "ReleaseRagGithubComponentTest",
                "QualificationLauncherComponentTest", "ComponentEvidenceScopeComponentTest",
                "ReleasePublicationSealComponentTest", "ReleaseSuiteReportingGovernanceTest")) {
            assertTrue(selected.matcher(required).matches(), "Missing RAG governance owner: " + required);
        }
        assertTrue(suite.getAnnotation(ExcludeTags.class).value().length > 0,
                "Live distribution checks must not enter the offline RAG gate.");
    }
}
