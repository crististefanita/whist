package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary reporting owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.contracts\\.AndroidInlineScreenshotReportContractTest$",
    "^atf\\.tests\\.contracts\\.AndroidPhaseTraceReplayContractTest$",
    "^atf\\.tests\\.contracts\\.LegacyUiMediaReportContractTest$",
    "^atf\\.tests\\.learning\\.EvidenceSelectionContractTest$",
    "^atf\\.tests\\.learning\\.LearningRunReportContractTest$",
    "^atf\\.tests\\.reporting\\.GameEvidenceBodiesContractTest$",
    "^atf\\.tests\\.reporting\\.GameReportPolicyContractTest$",
    "^atf\\.tests\\.reporting\\.GameReportPublicationContractTest$",
    "^atf\\.tests\\.reporting\\.GameReportTextAuditContractTest$",
    "^atf\\.tests\\.reporting\\.acceptance\\.NativeModuleResultsComponentTest$",
    "^atf\\.tests\\.reporting\\.acceptance\\.ComponentPublicationComponentTest$",
    "^atf\\.tests\\.reporting\\.acceptance\\.heap\\.ReportHeapComponentTest$",
    "^atf\\.tests\\.reporting\\.autonomy\\.StandaloneReportContractComponentTest$",
    "^atf\\.tests\\.reporting\\.autonomy\\.DetachedReviewComponentTest$",
    "^atf\\.tests\\.reporting\\.browser\\.BrowserExtentPublicationComponentTest$",
    "^atf\\.tests\\.reporting\\.text\\.ReportTextValueMatcherTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfReportingComponentSuite { }
