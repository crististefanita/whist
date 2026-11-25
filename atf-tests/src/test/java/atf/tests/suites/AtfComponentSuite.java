package atf.tests.suites;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary foundation owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.governance\\.source\\.language\\.SourceLanguageComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.language\\.SourceLanguageGovernanceTest$",
    "^atf\\.libraries\\.PlatformLibraryIntegrityTest$",
    "^atf\\.tests\\.contracts\\.HeuristicCatalogContractTest$",
    "^atf\\.tests\\.governance\\.source\\.AtfSourceQualityGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.ModuleBoundaryGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.PackageCohesionGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.TestLayerSyntaxTest$",
    "^atf\\.tests\\.governance\\.source\\.android\\.AndroidJavaApiGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.android\\.DebugFixturePackagingGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.TestChangeProofComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.TestChangeEditorialProofComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.TestChangeReviewComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.TestChangeSelectorComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.changes\\.domains\\.TestChangeDomainComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.game\\.GameSessionBoundaryComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.gradle\\.GradleAuditGovernanceTest$",
    "^atf\\.tests\\.governance\\.source\\.reporting\\.ReportProducerInventoryComponentTest$",
    "^atf\\.tests\\.governance\\.source\\.workspace\\.WorkspaceHygieneGovernanceTest$",
    "^atf\\.tests\\.governance\\.suites\\.AtfCollectionOwnershipComponentTest$",
    "^atf\\.tests\\.governance\\.suites\\.SuiteOwnershipGovernanceTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfComponentSuite { }
