package atf.tests.browser.runner;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import ro.cardtable.testing.browser.BrowserCheckResults;
import ro.cardtable.testing.browser.WhistBrowserChecks;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class WhistBrowserChecksComponentTest {
    @TempDir Path output;
    @Test void aClosedWriterCannotCreditLaterRowsOrRewritePublishedEvidence(ComponentInputs inputs) throws Exception {
        var records=new BrowserCheckResults(output);
        var result=new BrowserCheckResults.Result("ONE","Completed case","PASS","ready",Map.of("state","ready"),List.of());
        records.add(result);records.close();
        String original=Files.readString(output.resolve("browser-ui-checks.json"));
        inputs.add("Published prefix",original);
        assertThrows(IllegalStateException.class,()->records.add(result));
        records.close();
        assertEquals(original,Files.readString(output.resolve("browser-ui-checks.json")),"Repeated cleanup cannot rewrite or extend a published selection");
    }
    @Test void everyDiagnosticSelectionRetainsItsOrderedCaseIdentitiesAndViewports(ComponentInputs inputs) {
        var expected=new LinkedHashMap<String,List<String>>();
        expected.put("playback",List.of("WEB-COMPONENT-PLAYBACK-PAINT"));
        expected.put("seats",List.of("WEB-COMPONENT-SEATS-5","WEB-COMPONENT-SEATS-6"));
        expected.put("card-face",List.of("WEB-COMPONENT-TEXT","WEB-COMPONENT-CARD-INK","WEB-UI-AUTO"));
        expected.put("header",List.of("WEB-12-HEADER-414","WEB-12-HEADER-360"));
        expected.put("dedication",List.of("WEB-COMPONENT-HOME-DEDICATION"));
        expected.put("viewport",List.of("WEB-VIEWPORT-PHONE","WEB-VIEWPORT-DESKTOP"));
        expected.put("trump",List.of("WEB-COMPONENT-TRUMP"));
        expected.put("numbers",List.of("WEB-UI-NUMBERS"));
        var all=new ArrayList<String>();expected.values().forEach(all::addAll);
        all.addAll(List.of("WEB-UI-17","WEB-UI-HEADER-DESKTOP","WEB-UI-DESKTOP"));
        expected.put("all",all);
        expected.put("typography",List.of("CARD-TYPOGRAPHY","WEB-UI-NUMBERS"));
        inputs.add("Preconditions", "Diagnostic plan construction only, without a browser, server or product images. Existing selections, ordered IDs and viewports form the independent compatibility oracle.");
        var observed=new LinkedHashMap<String,List<String>>();
        for(var entry:expected.entrySet()) {
            var cases=WhistBrowserChecks.plan(entry.getKey(),"http://127.0.0.1/","original-checkpoint","wide-checkpoint",output);
            observed.put(entry.getKey(),cases.stream().map(WhistBrowserChecks.Check::id).toList());
            assertEquals(entry.getValue(),observed.get(entry.getKey()),"The complete selected case population and order remain unchanged");
            for(var check:cases) {
                assertFalse(check.name().isBlank());assertFalse(check.expected().isBlank());
                assertEquals(output,check.output(),"Output stays inside the caller-owned evidence directory");
                assertNotNull(check.scenario(),"Every admitted case retains its executable scenario");
                assertTrue(check.options().viewportSize.orElseThrow().width>0&&check.options().viewportSize.orElseThrow().height>0);
            }
        }
        inputs.add("Original plan inventory",observed.toString());
        var seats=WhistBrowserChecks.plan("seats","local","one","wide",output);
        assertEquals(List.of(414,360),seats.stream().map(c->c.options().viewportSize.orElseThrow().width).toList(),"Five- and six-seat diagnostic profiles are retained");
        var viewport=WhistBrowserChecks.plan("viewport","local","one","wide",output);
        assertEquals(List.of(860,720),viewport.stream().map(c->c.options().viewportSize.orElseThrow().height).toList(),"Phone and desktop diagnostic heights are retained");
    }

    @Test void invalidSelectionsDuplicateOwnersAndInterruptedFlowsCannotInventSuccessfulCases(ComponentInputs inputs) throws Exception {
        inputs.add("Negative controls", "Unknown selectors are refused before browser startup; duplicate result IDs are refused; an interrupted diagnostic writes only its completed prefix.");
        for(String selection:List.of("","ALL","foreign","seats,header"))
            assertThrows(AssertionError.class,()->WhistBrowserChecks.plan(selection,"local","one","wide",output));
        var images=new ArrayList<BrowserCheckResults.Image>();
        var first=new BrowserCheckResults.Result("FIRST","First completed case","PASS","ready",Map.of("state","ready"),images);
        images.add(new BrowserCheckResults.Image("foreign.png","Foreign later mutation"));
        assertEquals(0,first.images().size(),"A stored result cannot acquire another invocation's later images");
        var original=new IllegalStateException("Original interruption");
        var observed=assertThrows(IllegalStateException.class,()-> {
            try(var records=new BrowserCheckResults(output)) {
                records.add(first);
                assertThrows(IllegalArgumentException.class,()->records.add(first));
                throw original;
            }
        });
        assertSame(original,observed,"Writing the completed prefix retains the original flow interruption");
        var report=new com.fasterxml.jackson.databind.ObjectMapper().readTree(output.resolve("browser-ui-checks.json").toFile());
        assertEquals(1,report.size(),"Unexecuted planned cases never become passing results");
        assertEquals("FIRST",report.get(0).get("id").asText());
    }

    @Test void offlineComponentsAndStandaloneDiagnosticsUseTheSamePinnedClientWithoutBundledBrowsers(ComponentInputs inputs) throws Exception {
        var root=GovernanceProject.locateProjectRoot();
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        var standalone=factory.newDocumentBuilder().parse(root.resolve("atf-ui-browser/tests/playwright-dependencies.xml").toFile());
        var component=factory.newDocumentBuilder().parse(root.resolve("atf-tests/pom.xml").toFile());
        var xpath=javax.xml.xpath.XPathFactory.newInstance().newXPath();
        String path="//*[local-name()='dependency'][*[local-name()='artifactId']='playwright']/*[local-name()='version']/text()";
        String original=xpath.evaluate(path,standalone),actual=xpath.evaluate(path,component);
        inputs.add("Pinned browser client",original+"; component neighbours never launch the native driver");
        assertFalse(original.isBlank());assertEquals(original,actual,"Mocked components exercise the same client API as the standalone adapter");
        assertTrue(Files.readString(root.resolve("atf-tests/pom.xml")).contains("<artifactId>driver-bundle</artifactId>"),"Mocked components exclude bundled browser drivers");
    }
}
