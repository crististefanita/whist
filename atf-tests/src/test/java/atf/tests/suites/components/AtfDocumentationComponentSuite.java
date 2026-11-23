package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary documentation owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationAnchorGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationArchiveGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationDiagramGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationPolicyGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationProseGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationReadmeStructureGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DocumentationStyleGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.DynamicPathDocumentationArchitectureTest$",
    "^atf\\.tests\\.governance\\.documentation\\.factual\\.DocumentedModuleMapGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.feedback\\.FeedbackIndexGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.language\\.DocumentationLanguageComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.language\\.DocumentationTerminologyComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.language\\.DocumentationTerminologyGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.links\\.DocumentationLinkGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.ReleaseArtifactLayoutGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.ReleaseEvidenceRetentionGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.ReleaseIntegrityGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.ReleasePresentationGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.ReleaseRagPlatformGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.WebReleaseDossierComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.reporting\\.ReleaseSuiteReportingGovernanceTest$",
    "^atf\\.tests\\.governance\\.documentation\\.strategy\\.TestingStrategyGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.TestLayerGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.TestChangeGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.workspace\\.PrimaryBranchGovernanceTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfDocumentationComponentSuite { }
