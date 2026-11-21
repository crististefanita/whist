package atf.tests.governance;

import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Bounded offline entry point for RAG, launch, report and publication governance. */
@AtfIntegrationTest
@Suite
@SelectPackages("atf.tests.governance")
@IncludeClassNamePatterns("^.*(?:AtfRagGovernanceSuiteContractTest|ReleaseRagPlatformGovernanceTest|"
        + "ReleaseRagProvenanceComponentTest|ReleaseRagAndroidComponentTest|ReleaseRagGithubComponentTest|"
        + "QualificationLauncherComponentTest|ComponentEvidenceScopeComponentTest|"
        + "ReleasePublicationSealComponentTest|ReleaseSuiteReportingGovernanceTest)$")
@ExcludeTags("release-download-live")
public class AtfRagGovernanceSuite { }
