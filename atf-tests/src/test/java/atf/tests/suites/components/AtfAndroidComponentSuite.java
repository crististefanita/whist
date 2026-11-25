package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary android owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.ReleaseRagAndroidComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.nativeproof\\.SharedPresentationComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.nativeproof\\.WhistSurfaceAdapterComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.nativeproof\\.WhistBindingComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.AndroidReleasePreflightComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.AndroidUserOwnershipComponentTest$",
    "^atf\\.tests\\.governance\\.uiadapter\\.SharedMenuHistoryComponentTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfAndroidComponentSuite { }
