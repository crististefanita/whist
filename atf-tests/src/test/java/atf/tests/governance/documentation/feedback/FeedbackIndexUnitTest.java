package atf.tests.governance.documentation.feedback;

import atf.testkit.AtfUnitTest;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.policy.FeedbackIndexRules;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class FeedbackIndexUnitTest {
    @TempDir Path root;
    private static final String HEADER = "| ID | Priority | Score | Criterion |\n|---|---|---|---|\n";
    private List<Violation> inspect(String body, boolean priorities) throws Exception {
        Files.writeString(root.resolve("Owner.md"), "# Owner\n");var out = new ArrayList<Violation>();
        if(priorities) FeedbackIndexRules.inspectPriorities(("## Prioritized ToDos\n"+body).lines().toList(),root.resolve("README.md"),root,out);
        else FeedbackIndexRules.inspectRows(body.lines().toList(),root.resolve("README.md"),root,out);
        return out;
    }
    private String row(int id,int score,String target) { return "| GOV-"+String.format(Locale.ROOT,"%02d",id)+" | P2 | "+score+" | [Owner]("+target+") |\n"; }
    @Test void zeroThroughNineOpenItemsCanCompleteWithoutFillingAQuota() throws Exception {
        for(int size=0;size<10;size++) {
            var body=new StringBuilder(size==0?"<!-- feedback:empty -->":HEADER);
            for(int i=0;i<size;i++)body.append(row(i,100-i,"Owner.md#owner"));
            assertTrue(inspect(body.toString(),false).isEmpty());assertTrue(inspect(body.toString(),true).isEmpty());
        }
    }
    @Test void everyTrackSharesIdsButRetainsItsOwnDescendingOrder() throws Exception {
        String first=HEADER+row(1,80,"Owner.md");
        assertTrue(inspect(first+"\n### Other\n"+HEADER+row(2,99,"Owner.md"),false).isEmpty());
        assertFalse(inspect(first+"\n### Other\n"+HEADER+row(1,99,"Owner.md"),false).isEmpty());
        assertFalse(inspect(first+row(2,90,"Owner.md"),false).isEmpty());
    }
    @Test void badPrioritiesMalformedColumnsAndInvalidNumericScoresAreRejected() throws Exception {
        for(String bad:List.of(row(1,90,"Owner.md").replace("P2","P3"),row(1,90,"Owner.md").replace("90","high"),
                row(1,101,"Owner.md"),row(1,0,"Owner.md"),row(1,90,"Owner.md").replace(" | P2", " | extra | P2")))
            assertFalse(inspect(HEADER+bad,false).isEmpty());
    }
    @Test void ownerMustResolveInsideTheProjectIncludingItsFragment() throws Exception {
        for(String bad:List.of("Owner.md#missing","../missing.md","https://example.invalid","file:/outside","//outside/owner.md"))
            assertFalse(inspect(HEADER+row(1,90,bad),false).isEmpty());
        assertTrue(inspect(HEADER+row(1,90,"Owner.md#owner"),false).isEmpty());
    }
    @Test void examplesCannotDeclareCompletionAndCompletionCannotCoexistWithOpenRows() throws Exception {
        assertFalse(inspect("\u0060\u0060\u0060markdown\n<!-- feedback:empty -->\n\u0060\u0060\u0060",false).isEmpty());
        assertFalse(inspect(HEADER+row(1,90,"Owner.md")+"<!-- feedback:empty -->",false).isEmpty());
        assertFalse(inspect("# Feedback",false).isEmpty());
    }
}
