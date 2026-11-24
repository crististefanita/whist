package atf.tests.governance.source.language;

import atf.testkit.AtfUnitTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runContracts;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class SourceLanguageUnitTest {
    @Test void lexicalMatricesDistinguishImplementationFromLocalizedLiterals() throws Exception {
        assertTrue(runContracts(Duration.ofSeconds(15), "source-language.test.mjs")
                .contains("English identifiers and comments coexist with localized data"));
    }
}
