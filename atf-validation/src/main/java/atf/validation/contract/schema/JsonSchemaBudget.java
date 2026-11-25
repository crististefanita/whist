package atf.validation.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayDeque;
import java.util.Iterator;

/** Per-evaluation structural admission and deterministic work accounting. */
public final class JsonSchemaBudget {
    public static final int MAX_DEPTH = 64;
    public static final int MAX_NODES = 16_384;
    public static final int MAX_TEXT_UNITS = 1_048_576;
    public static final int MAX_STEPS = 200_000;
    private int remaining = MAX_STEPS;

    public static void admit(JsonNode root, String owner) {
        var stack = new ArrayDeque<Frame>();
        stack.push(new Frame(java.util.List.of(root).iterator(), 0));
        int nodes = 0;
        long text = 0;
        while (!stack.isEmpty()) {
            var frame = stack.peek();
            if (!frame.nodes().hasNext()) { stack.pop(); continue; }
            JsonNode node = frame.nodes().next();
            if (frame.depth() > MAX_DEPTH) throw limit(owner, "depth", MAX_DEPTH);
            if (++nodes > MAX_NODES) throw limit(owner, "nodes", MAX_NODES);
            if (node.isTextual()) text += node.textValue().length();
            if (node.isObject()) {
                var names = node.fieldNames();
                while (names.hasNext()) text += names.next().length();
            }
            if (text > MAX_TEXT_UNITS) throw limit(owner, "text units", MAX_TEXT_UNITS);
            if (node.isContainerNode()) stack.push(new Frame(node.elements(), frame.depth() + 1));
        }
    }

    public void step() {
        if (--remaining < 0) throw limit("evaluation", "steps", MAX_STEPS);
    }

    private static Limit limit(String owner, String resource, int maximum) {
        return new Limit(owner + ": JSON Schema budget exceeded (" + resource + ", maximum " + maximum + ")");
    }

    public static final class Limit extends IllegalArgumentException {
        private Limit(String message) { super(message); }
    }

    private record Frame(Iterator<JsonNode> nodes, int depth) { }
}
