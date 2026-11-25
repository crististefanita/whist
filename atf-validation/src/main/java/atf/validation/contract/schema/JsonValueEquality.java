package atf.validation.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;

/** JSON Schema equality compares mathematical numbers without narrowing. */
public final class JsonValueEquality {
    private JsonValueEquality() { }
    public static boolean same(JsonNode left, JsonNode right) {
        return same(left, right, () -> { });
    }
    public static boolean same(JsonNode left, JsonNode right, Runnable step) {
        step.run();
        if (left.isNumber() && right.isNumber()) return left.decimalValue().compareTo(right.decimalValue()) == 0;
        if (left.isArray() && right.isArray()) {
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++) if (!same(left.get(i), right.get(i), step)) return false;
            return true;
        }
        if (left.isObject() && right.isObject()) {
            if (left.size() != right.size()) return false;
            var fields = left.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (!right.has(field.getKey()) || !same(field.getValue(), right.get(field.getKey()), step)) return false;
            }
            return true;
        }
        return left.equals(right);
    }
}
