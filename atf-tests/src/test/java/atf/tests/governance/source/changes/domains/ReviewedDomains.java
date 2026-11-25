package atf.tests.governance.source.changes.domains;

import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads the reviewed declarations rather than maintaining another selector catalog. */
final class ReviewedDomains {
    static final String VALIDATOR = "RAG-DOMAIN-VALIDATOR";
    static final String UI = "RAG-DOMAIN-UI";
    static final String REPORTING = "RAG-DOMAIN-REPORTING";
    static final String CATALOG = "RAG-DOMAIN-CATALOG";
    static final String SELECTION = "RAG-DOMAIN-SELECTION";
    static final String LEGACY = "UI-CONTENT-REPORT-PUBLICATION";
    static final String REGISTER = "docs/governance/ATF_TestChangesBaseline.md";
    static final List<String> IDS = List.of(VALIDATOR, UI, REPORTING, CATALOG, SELECTION);
    private static final Pattern FENCE = Pattern.compile("(?ms)^```yaml[^\\r\\n]*\\R(.*?)^```");
    private final Map<String, JsonNode> rows;

    private ReviewedDomains(Map<String, JsonNode> rows) { this.rows = Map.copyOf(rows); }

    static ReviewedDomains load() throws Exception {
        String markdown = Files.readString(GovernanceProject.locateProjectRoot().resolve(REGISTER));
        var yaml = new ObjectMapper(new YAMLFactory());
        var found = new LinkedHashMap<String, JsonNode>();
        var blocks = FENCE.matcher(markdown);
        while (blocks.find()) for (var row : yaml.readTree(blocks.group(1)).path("testChanges")) {
            String id = row.path("id").asText();
            if ((id.startsWith("RAG-DOMAIN-") || id.equals(LEGACY)) && found.put(id, row) != null)
                throw new IllegalStateException("Duplicate reviewed domain: " + id);
        }
        for (String id : Stream.concat(IDS.stream(), Stream.of(LEGACY)).toList())
            if (!found.containsKey(id)) throw new IllegalStateException("Missing reviewed domain: " + id);
        return new ReviewedDomains(found);
    }

    JsonNode row(String id) { return rows.get(id); }
    List<String> domainIds() { return rows.keySet().stream().filter(id -> !id.equals(LEGACY)).sorted().toList(); }
    List<String> paths(String id) { return values(row(id).path("paths")); }
    List<String> checks(String id) {
        return Stream.concat(values(row(id).path("checks").path("positive")).stream(),
                values(row(id).path("checks").path("negative")).stream()).toList();
    }
    static List<String> values(JsonNode node) {
        var values = new java.util.ArrayList<String>();
        node.forEach(value -> values.add(value.asText()));
        return List.copyOf(values);
    }
}
