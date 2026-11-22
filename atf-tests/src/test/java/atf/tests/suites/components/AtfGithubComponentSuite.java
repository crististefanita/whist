package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary github owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.governance\\.documentation\\.release\\.download\\.ApkDownloadComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.download\\.DownloadSelectionComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.ReleaseRagGithubComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.ReleasePublicationSealComponentTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfGithubComponentSuite { }
