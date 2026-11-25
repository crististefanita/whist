package atf.tests.documentation.policy;

import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.links.MarkdownAnchorIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import static atf.tests.documentation.DocumentationGovernanceSupport.violation;

/** All active feedback tables are governed; completion never requires inventing more open work. */
public final class FeedbackIndexRules {
    private static final Pattern LOCAL_LINK = Pattern.compile("\\[[^]]+]\\(([^)]+)\\)");
    private static final String EMPTY = "<!-- feedback:empty -->";
    private FeedbackIndexRules() { }

    public static void inspectRows(List<String> lines, Path file, Path root, List<Violation> violations) {
        validate(MarkdownAnchorIndex.withoutFences(lines), 0, file, root, violations);
    }

    public static void inspectPriorities(List<String> lines, Path file, Path root, List<Violation> violations) {
        var visible = MarkdownAnchorIndex.withoutFences(lines);
        int heading = visible.indexOf("## Prioritized ToDos");
        if (heading < 0) heading = visible.indexOf("## ToDo-uri prioritizate");
        if (heading < 0) {
            row(violations, root, file, 1, "the prioritized feedback heading is missing");
            return;
        }
        int end = heading + 1;
        while (end < visible.size() && !visible.get(end).startsWith("## ")) end++;
        validate(visible.subList(heading + 1, end), heading + 1, file, root, violations);
    }

    private static void validate(List<String> lines, int offset, Path file, Path root, List<Violation> violations) {
        boolean header = false, active = false, empty = false;
        int rows = 0;
        Integer previous = null;
        var ids = new HashSet<String>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            int number = offset + index + 1;
            if (line.equals(EMPTY)) empty = true;
            if (!line.startsWith("|")) { active = false; continue; }
            String[] cells = line.split("\\|", -1);
            if (cells.length >= 6 && cells[1].trim().equals("ID") && List.of("Score", "Scor").contains(cells[3].trim())) {
                header = true; active = true; previous = null; continue;
            }
            if (!active) continue;
            if (cells.length != 6) { row(violations, root, file, number, "feedback row must have exactly four columns"); continue; }
            String id = cells[1].trim();
            if (id.matches(":?-{3,}:?")) continue;
            rows++;
            if (!id.matches("[A-Z][A-Z0-9]*-[0-9]{2}")) row(violations, root, file, number, "feedback row has invalid ID: " + id);
            else if (!ids.add(id)) row(violations, root, file, number, "feedback row has duplicate ID " + id);
            if (!List.of("P1", "P2").contains(cells[2].trim())) row(violations, root, file, number, "feedback row has invalid priority: " + cells[2].trim());
            try {
                int score = Integer.parseInt(cells[3].trim());
                if (score < 1 || score > 100) row(violations, root, file, number, "feedback row " + id + " score must be between 1 and 100");
                if (previous != null && score > previous) row(violations, root, file, number, "feedback scores must be in descending order at " + id);
                previous = score;
            } catch (NumberFormatException invalid) { row(violations, root, file, number, "feedback row " + id + " has a non-integer score"); }
            owner(cells[4], id, number, file, root, violations);
        }
        if (empty && rows > 0) row(violations, root, file, offset + 1, "empty feedback marker contradicts active rows");
        if ((!header || rows == 0) && !empty) row(violations, root, file, offset + 1, "feedback ToDo table is missing or empty; declare an empty backlog explicitly");
    }

    private static void owner(String criterion, String id, int line, Path file, Path root, List<Violation> violations) {
        var link = LOCAL_LINK.matcher(criterion);
        if (!link.find()) { row(violations, root, file, line, "feedback row " + id + " has no owner link"); return; }
        String target = link.group(1).trim();
        if (target.matches("(?i)^[a-z][a-z0-9+.-]*:.*") || target.startsWith("//")) {
            row(violations, root, file, line, "feedback row " + id + " owner link must be a local project document"); return;
        }
        String[] parts = target.split("#", 2);
        Path resolved = file.getParent().resolve(parts[0]).normalize();
        try {
            if (!resolved.startsWith(root.normalize()) || !Files.isRegularFile(resolved) || !resolved.toRealPath().startsWith(root.toRealPath()))
                throw new java.io.IOException("missing or external owner");
            if (parts.length == 2 && !MarkdownAnchorIndex.anchors(Files.readAllLines(resolved)).contains(parts[1]))
                row(violations, root, file, line, "feedback row " + id + " owner anchor does not resolve: " + target);
        } catch (java.io.IOException invalid) { row(violations, root, file, line, "feedback row " + id + " owner link does not resolve: " + target); }
    }

    private static void row(List<Violation> violations, Path root, Path file, int line, String finding) {
        violations.add(violation("DOC-FEEDBACK-INDEX", root, file, line, finding,
                "all active feedback tables have unique IDs, valid priorities, ordered scores and local owners; an empty backlog is explicit",
                "correct the row or its owner; remove completed work without retaining an artificial minimum count"));
    }
}
