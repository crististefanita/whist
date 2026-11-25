package atf.tests.contracts.validation;

import atf.android.evidence.component.ComponentInputs;
import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationEvaluator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest @ExtendWith(GameReportAudit.class)
class SchemaBudgetComponentTest {
    @TempDir Path scratch;

    @Test void boundedContractsRetainPathsOrderAndEvaluationIsolation(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Public ResponseContract and ValidationEvaluator; controlled UTF-8 schema file and complete response. No transport, renderer or device is mocked.");
        String schema = "{\"items\":{\"pattern\":\"(?U)^\\\\p{L}+$\"}}";
        assertEquals("schema matched", evaluate(schema, "[\"Ștefan\",\"Ana\"]", false, inputs));
        assertEquals("$[0]: does not match pattern | $[2]: does not match pattern",
                evaluate(schema, "[\"1\",\"Ana\",\"!\"]", false, inputs));
        assertEquals("schema matched", evaluate(schema, "[\"Ștefan\"]", false, inputs));
        String nested = "[".repeat(64) + "0" + "]".repeat(64);
        assertEquals("schema matched", evaluate("true", nested, false, inputs), "Depth 64 remains admitted even for an unconstrained schema");
    }

    @Test void excessiveInputsAndRepeatedRulesFailClosedUnderBothPolicies(ComponentInputs inputs) throws Exception {
        inputs.add("Precondition", "Independent literal boundaries: depth 64, 16384 nodes, 1048576 UTF-16 text units and 200000 work steps per evaluation. Limit refusals are comparisons, not truncated successful evaluations.");
        String deep = "[".repeat(65) + "0" + "]".repeat(65);
        String wide = "[" + "0,".repeat(16383) + "0]";
        String text = "\"" + "a".repeat(1048577) + "\"";
        String enumeration = "{\"enum\":[" + java.util.stream.IntStream.range(0, 1000)
                .mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining(",")) + "]}";
        String properties = java.util.stream.IntStream.range(0, 300)
                .mapToObj(i -> "\"p" + i + "\":true").collect(java.util.stream.Collectors.joining(","));
        String repeated = "{\"items\":{\"properties\":{" + properties + "}}}";
        String objects = "[" + "{},".repeat(799) + "{}]";
        for (boolean warning : new boolean[]{false, true}) {
            assertTrue(evaluate("true", deep, warning, inputs).contains("instance: JSON Schema budget exceeded (depth"));
            assertTrue(evaluate("true", wide, warning, inputs).contains("instance: JSON Schema budget exceeded (nodes"));
            assertTrue(evaluate("true", text, warning, inputs).contains("instance: JSON Schema budget exceeded (text units"));
            assertTrue(evaluate("{\"default\":" + deep + "}", "null", warning, inputs).contains("schema: JSON Schema budget exceeded (depth"), "Annotation and unvisited data cannot bypass structural admission");
            assertTrue(evaluate(enumeration, "0", warning, inputs).contains("evaluation: JSON Schema budget exceeded (steps"), "Schema prevalidation shares the work budget");
            assertTrue(evaluate(repeated, objects, warning, inputs).contains("evaluation: JSON Schema budget exceeded (steps"), "Repeated property scans consume work even when no instance field matches");
        }
        assertEquals("schema matched", evaluate("true", "null", false, inputs), "A rejected evaluation cannot poison the next one");
    }

    private String evaluate(String schema, String body, boolean warning, ComponentInputs inputs) throws Exception {
        Path path = Files.writeString(scratch.resolve("schema.json"), schema);
        var decision = new ValidationEvaluator().evaluate(new ResponseContract().jsonSchema(path.toString()),
                new ApiResponse(200, body, Map.of(), Duration.ZERO, ""), warning);
        var row = decision.rows().getFirst();
        inputs.add("Compared evaluation", "schema characters=" + schema.length() + "; instance characters=" + body.length()
                + "; warning=" + warning + "; status=" + decision.status() + "; actual=" + row.actual());
        assertEquals(row.passed() ? AtfReportStatus.PASS : warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL,
                decision.status(), "Strict and warning policies retain the evaluated comparison");
        return row.actual();
    }
}
