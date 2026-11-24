package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.publication.ComponentReportPublication;
import atf.testkit.AtfUnitTest;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportHeapUnitTest {
    @TempDir Path root;
    @Test void completeTextSurvivesDisclosurePartBoundaries() throws Exception {
        var file=Files.writeString(root.resolve("fixture.txt"),"<html><pre>owner=0; val</pre><p>Unrelated label</p><pre>ue=xxxx; final=0;</pre></html>");
        CapturedTextProbe.verify(file,1,4);
    }
    @Test void wrongOwnersTruncatedValuesAndPrefixOnlySuffixesAreRejected() throws Exception {
        for(String text:new String[]{"owner=1; value=xxxx; final=0;","owner=0; value=xxx; final=0;",
                "owner=0; value=xxxx; final=01;","owner=0; value=xxxx; final=0"}){
            var file=Files.writeString(root.resolve("fixture.txt"),"<html><pre>"+text+"</pre></html>");
            assertThrows(IOException.class,()->CapturedTextProbe.verify(file,1,4));
        }
    }
    @Test void invalidByteLimitsCannotBeginPublication() {
        for(long limit:new long[]{0,-1,Long.MIN_VALUE})
            assertThrows(IllegalArgumentException.class,()->ComponentReportPublication.publish(root.resolve("output.html"),limit,
                    file->{throw new AssertionError("Renderer must not be called");},file->{throw new AssertionError("Verifier must not be called");}));
    }
}
