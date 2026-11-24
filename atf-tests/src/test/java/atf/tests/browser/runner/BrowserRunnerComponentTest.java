package atf.tests.browser.runner;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import ro.cardtable.testing.browser.BrowserAcceptanceSession;
import ro.cardtable.testing.browser.BrowserCheckResults;
import ro.cardtable.testing.browser.WhistBrowserChecks;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class BrowserRunnerComponentTest {
    @TempDir Path output;
    @Test void ownedLifecyclePreservesOrderingMutedHeadlessOptionsAndResultSchema(ComponentInputs inputs) throws Exception {
        var fixture = new BrowserSessionFixture();
        BrowserCheckResults.Result result;
        try(var session=BrowserAcceptanceSession.open(()->fixture.playwright,Path.of("controlled-browser.exe"),fixture.capture)) {
            result=session.run(check("ONE",(page,images)->{fixture.event("scenario");return Map.of("state","ready");}));
        }
        inputs.add("Preconditions", "Playwright interfaces and capture port are controlled neighbours; no browser or PNG is created.");
        inputs.add("Original protocol events",fixture.events.toString());
        assertEquals(List.of("playwright.chromium","browser.launch","browser.context","context.page","page.timeout","page.errors","scenario","browser.version","capture","context.close","browser.close","playwright.close"),fixture.events,"Ownership and scenario order remain explicit");
        assertEquals(true,fixture.launchOptions.headless,"The native browser launch is headless");
        assertEquals(List.of("--mute-audio"),fixture.launchOptions.args,"The browser cannot enable audio");
        assertEquals("PASS",result.status(),"The original successful observation remains passing");
        try(var records=new BrowserCheckResults(output)){records.add(result);assertFalse(records.failed());}
        var json=new ObjectMapper().readTree(output.resolve("browser-ui-checks.json").toFile());
        assertEquals("ONE",json.get(0).required("id").asText(),"The publisher input retains the case identity");
        assertEquals("ready",json.get(0).required("observed").required("observations").required("state").asText());
        assertEquals(6,json.get(0).size(),"Existing id/name/status/expected/observed/images schema is preserved");
    }

    @Test void failedCaptureBodyAndCleanupCannotMaskTheOriginalOrHideTheNextCase(ComponentInputs inputs) throws Exception {
        var fixture=new BrowserSessionFixture(); var original=new IllegalStateException("Original scenario failure");
        fixture.failures.put("capture",new IllegalStateException("Capture failed"));
        fixture.failures.put("body.text",new IllegalStateException("Body unavailable"));
        fixture.failures.put("context.close",new IllegalStateException("Context cleanup failed"));
        try(var records=new BrowserCheckResults(output);
            var session=BrowserAcceptanceSession.open(()->fixture.playwright,null,fixture.capture)) {
            var failed=session.run(check("FAILED",(page,images)->{throw original;}));records.add(failed);
            inputs.add("Original failure diagnostic",failed.observed().toString());
            assertEquals("FAIL",failed.status());
            assertEquals(3,original.getSuppressed().length,"Capture, body and cleanup remain secondary to the actual scenario failure");
            for(var secondary:original.getSuppressed())assertTrue(failed.observed().toString().contains(secondary.getMessage()),"The report retains each secondary cause");
            fixture.failures.clear();
            records.add(session.run(check("NEXT",(page,images)->Map.of("state","ready"))));
            assertTrue(records.failed(),"A later passing case cannot erase the original failure");
        }
        var json=new ObjectMapper().readTree(output.resolve("browser-ui-checks.json").toFile());
        assertEquals(List.of("FAIL","PASS"),List.of(json.get(0).get("status").asText(),json.get(1).get("status").asText()),"Both original outcomes survive publication");
        try(var files=Files.list(output)){assertEquals(List.of("browser-ui-checks.json"),files.map(p->p.getFileName().toString()).toList(),"Mocked captures never create component images");}
        String before=Files.readString(output.resolve("browser-ui-checks.json"));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,()->new BrowserCheckResults(output));
        assertEquals(before,Files.readString(output.resolve("browser-ui-checks.json")),"A second writer cannot replace the original result");
    }

    @Test void launchAndShutdownFailuresPreserveThePrimaryCauseAndCloseOnlyOwnedResources(ComponentInputs inputs) {
        var launch=new BrowserSessionFixture();var original=new IllegalStateException("Original launch failure");var cleanup=new IllegalStateException("Playwright cleanup");
        launch.failures.put("browser.launch",original);launch.failures.put("playwright.close",cleanup);
        assertSame(original,assertThrows(IllegalStateException.class,()->BrowserAcceptanceSession.open(()->launch.playwright,null,launch.capture)));
        assertEquals(List.of("playwright.chromium","browser.launch","playwright.close"),launch.events,"A failed launch cannot close another browser");
        assertSame(cleanup,original.getSuppressed()[0]);
        var owned=new BrowserSessionFixture();var closeFailure=new IllegalStateException("Owned browser close");
        var session=BrowserAcceptanceSession.open(()->owned.playwright,null,owned.capture);
        owned.failures.put("browser.close",closeFailure);owned.failures.put("playwright.close",cleanup);
        inputs.add("Original lifecycle errors",original.toString()+"; "+closeFailure);
        assertSame(closeFailure,assertThrows(IllegalStateException.class,session::close));
        assertSame(cleanup,closeFailure.getSuppressed()[0],"Both owned resources are attempted while preserving the first cause");
    }

    @Test void earlyContextFailuresAndJavascriptErrorsCannotBecomePassingCases(ComponentInputs inputs) {
        for(String stage:List.of("browser.context","context.page","page.timeout","page.errors","browser.version")) {
            var fixture=new BrowserSessionFixture();fixture.failures.put(stage,new IllegalStateException("Original "+stage));
            try(var session=BrowserAcceptanceSession.open(()->fixture.playwright,null,fixture.capture)) {
                var result=session.run(check(stage,(page,images)->Map.of("state","ready")));
                inputs.add("Original boundary outcome",stage+" -> "+result.observed());
                assertEquals("FAIL",result.status(),"Infrastructure failures retain a failed case identity");
                assertTrue(result.observed().toString().contains("Original "+stage));
                if(!stage.equals("browser.context"))assertTrue(fixture.events.contains("context.close"),"Every allocated context is closed");
            }
        }
        var fixture=new BrowserSessionFixture();
        try(var session=BrowserAcceptanceSession.open(()->fixture.playwright,null,fixture.capture)) {
            var result=session.run(check("PAGE-ERROR",(page,images)->{fixture.pageError.accept("Original JavaScript error");return Map.of("state","ready");}));
            assertEquals("FAIL",result.status());assertTrue(result.observed().toString().contains("Original JavaScript error"));
        }
    }
    private WhistBrowserChecks.Check check(String id,WhistBrowserChecks.Scenario scenario) {
        return new WhistBrowserChecks.Check(new Browser.NewContextOptions().setViewportSize(414,896),output,id,"Controlled "+id,"The declared observation is ready",scenario);
    }
}
