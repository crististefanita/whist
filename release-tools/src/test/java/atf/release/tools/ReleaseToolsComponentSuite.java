package atf.release.tools;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.ConfigurationParameter;
import atf.testkit.scope.TestRunScope;

@Suite
@SelectPackages("atf.release.tools")
@IncludeClassNamePatterns("^atf\\.release\\.tools\\..*ComponentTest$")
@IncludeTags("component")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfComponentTest
public class ReleaseToolsComponentSuite { }
