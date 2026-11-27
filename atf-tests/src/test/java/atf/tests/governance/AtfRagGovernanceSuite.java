package atf.tests.governance;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Bounded offline entry point for RAG, launch, report and publication governance. */
@AtfComponentTest
@Suite
@SelectPackages("atf.tests.governance")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "atf.component.aggregate", value = "true")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@IncludeClassNamePatterns("^.*(?:AtfRagGovernanceSuiteContractTest|ReleaseRagPlatformGovernanceTest|"
        + "ReleaseRagProvenanceComponentTest|ReleaseRagAndroidComponentTest|ReleaseRagGithubComponentTest|"
        + "QualificationLauncherComponentTest|ComponentEvidenceScopeComponentTest|"
        + "ReleasePublicationSealComponentTest|ReleaseSuiteReportingGovernanceTest)$")
@ExcludeTags("release-download-live")
public class AtfRagGovernanceSuite { }
