package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary orchestration owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.browser\\.runner\\.BrowserRunnerComponentTest$",
    "^atf\\.tests\\.browser\\.runner\\.WhistBrowserChecksComponentTest$",
    "^atf\\.tests\\.execution\\.components\\.ComponentAggregateAcceptanceTest$",
    "^atf\\.tests\\.execution\\.components\\.ComponentSuiteExecutionComponentTest$",
    "^atf\\.tests\\.execution\\.components\\.TestChangeBatchComponentTest$",
    "^atf\\.tests\\.execution\\.jvm\\.ComponentJvmLauncherComponentTest$",
    "^atf\\.tests\\.execution\\.proofs\\.StaticProofRecoveryComponentTest$",
    "^atf\\.tests\\.execution\\.recovery\\.InvocationRecoveryComponentTest$",
    "^atf\\.tests\\.governance\\.AtfRagGovernanceSuiteContractTest$",
    "^atf\\.tests\\.governance\\.documentation\\.cli\\.DocumentationGovernanceCliComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.ComponentEvidencePortabilityComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.ComponentEvidenceScopeComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.QualificationLauncherComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.io\\.SnapshotIoComponentTest$",
    "^atf\\.tests\\.governance\\.support\\.JvmInventoryCliComponentTest$",
    "^atf\\.tests\\.governance\\.support\\.LauncherSourceInventoryComponentTest$",
    "^atf\\.tests\\.governance\\.support\\.SigningSourceIsolationComponentTest$",
    "^atf\\.tests\\.governance\\.support\\.SourceInventoryContractTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfOrchestrationComponentSuite { }
