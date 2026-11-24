package atf.tests.suites.components;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** Primary provenance owner; its positive class inventory is governed with the catalog. */
@Suite
@SelectPackages({"atf.tests", "atf.libraries"})
@IncludeClassNamePatterns({
    "^atf\\.tests\\.governance\\.documentation\\.release\\.handoff\\.ReleaseCommitEvidenceComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.integrity\\.ApkIntegrityComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.integrity\\.JunitIntegrityComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.integrity\\.ManifestIntegrityComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.integrity\\.html\\.HtmlInvocationIntegrityComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.ReleaseRagProvenanceComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.nativeproof\\.NativeSourceProvenanceComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.VisualContractScopeComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.rag\\.scope\\.context\\.QualifiedCatalogContextComponentTest$",
    "^atf\\.tests\\.governance\\.documentation\\.release\\.versioning\\.ReleaseVersionComponentTest$"
})
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class AtfProvenanceComponentSuite { }
