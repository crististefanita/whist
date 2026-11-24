package atf.android.ai;

import atf.android.component.AndroidComponentAudit;
import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.OneCardBidBelief;
import ro.whistlab.core.rules.GameMode;
import static atf.testkit.assertions.AuditedAssertions.*;

/** The YAML is descriptive; executable domain predicates must remain its independent counterpart. */
@AtfComponentTest @ExtendWith(AndroidComponentAudit.class)
class OneCardDomainComponentTest {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test void theManifestMatchesTheRuntimeDomainAndKeepsItsSetsImmutable(ComponentInputs inputs) throws Exception {
        var manifest = manifest();
        inputs.add("Declared one-card model", manifest);
        inputs.add("Runtime one-card domain", OneCardBidBelief.domain());
        assertEquals(List.of(), differences(manifest), "The manifest names the current owner, entry, conditioned support and fallback");
        assertThrows(UnsupportedOperationException.class, () -> OneCardBidBelief.domain().conditionedModes().clear(), "Callers cannot mutate the shared domain");
    }

    @Test void everyModeTableSizeAndPublicShapeKeepsEntrySeparateFromConditioning(ComponentInputs inputs) throws Exception {
        var declared = manifest();
        var mismatches = new ArrayList<String>();
        int cases = 0;
        for (var mode : GameMode.values()) for (int seats = 3; seats <= 6; seats++)
            for (int cards = 1; cards <= 8; cards++) for (boolean turned : List.of(false, true))
                for (boolean table : List.of(false, true)) for (boolean history : List.of(false, true)) {
                    var view = AiBidFixtures.shape(AiBidFixtures.view(mode, cards, seats, turned), table, history);
                    boolean entry = cards == declared.at("/auction/hand_size").asInt()
                            && view.hand().size() == declared.at("/auction/private_cards").asInt()
                            && view.trick().size() == declared.at("/auction/trick_cards").asInt()
                            && view.history().size() == declared.at("/auction/history_cards").asInt();
                    boolean conditioned = strings(declared.at("/conditioning/modes")).contains(mode.name())
                            && seats == declared.at("/conditioning/seats").asInt()
                            && (!declared.at("/conditioning/turned_card_required").asBoolean() || turned);
                    if (entry != OneCardBidBelief.isOneCardAuction(view)
                            || conditioned != OneCardBidBelief.usesConditionedBids(view))
                        mismatches.add(mode + "/seats=" + seats + "/cards=" + cards + "/turned=" + turned + "/table=" + table + "/history=" + history);
                    cases++;
                }
        inputs.add("Matrix preconditions", "Every declared mode; 3–6 seats; 1–8 own cards; exposed card present/absent; table and history empty/nonempty. No game simulation, device or pictures.");
        inputs.add("Matrix population", cases);
        inputs.add("All domain disagreements", mismatches);
        assertEquals(1536, cases, "The complete declared boundary matrix is visited");
        assertEquals(List.of(), mismatches, "Runtime predicates and descriptive domain agree on every observation shape");
    }

    @Test void missingOrReversedDomainFieldsFailInsteadOfSilentlyChangingTheContract(ComponentInputs inputs) throws Exception {
        var original = manifest();
        for (String mutation : List.of("missing", "owner", "entry", "modes", "seats", "turned", "fallback", "forced")) {
            ObjectNode altered = original.deepCopy();
            switch (mutation) {
                case "missing" -> altered.remove("auction");
                case "owner" -> altered.put("owner", "DifferentModel");
                case "entry" -> ((ObjectNode) altered.get("auction")).put("hand_size", 2);
                case "modes" -> ((ObjectNode) altered.get("conditioning")).putArray("modes").add("OH_HELL");
                case "seats" -> ((ObjectNode) altered.get("conditioning")).put("seats", 6);
                case "turned" -> ((ObjectNode) altered.get("conditioning")).put("turned_card_required", false);
                case "fallback" -> altered.put("fallback", "CONDITION_EVERY_TABLE");
                case "forced" -> altered.put("forced_dealer_bid", "HAND_STRENGTH_SIGNAL");
                default -> throw new IllegalArgumentException(mutation);
            }
            var errors = differences(altered);
            inputs.add("Controlled domain mutation: " + mutation, errors);
            assertFalse(errors.isEmpty(), "The independent contract comparison rejects " + mutation);
        }
        assertEquals(List.of(), differences(original), "Negative controls do not mutate the live manifest");
    }

    @Test void unsupportedTablesModesAndMissingTrumpKeepTheOriginalFallback(ComponentInputs inputs) {
        int compared = 0;
        for (var mode : GameMode.values()) for (int seats = 3; seats <= 6; seats++) for (boolean turned : List.of(false, true)) {
            var view = AiBidFixtures.view(mode, 1, seats, turned);
            if (OneCardBidBelief.usesConditionedBids(view)) continue;
            assertEquals(MonteCarloAi.oneCardWinProbability(view), OneCardBidBelief.probability(view), 0.0,
                    "Outside the conditioning domain the original unconditioned model is retained: " + mode + "/" + seats + "/" + turned);
            compared++;
        }
        inputs.add("Fallback boundary", "Every mode and 3–6 seats, with/without exposed trump. Comparison checks routing to the unconditioned API, not independent calibration of its probability formula.");
        assertEquals(46, compared, "Every unsupported conditioning combination is exercised");
    }

    private static ObjectNode manifest() throws Exception {
        return (ObjectNode) YAML.readTree(Files.readString(Path.of("..", "config", "whist-play-objectives.yaml"))).required("one_card_bid_model");
    }
    private static Set<String> strings(JsonNode values) {
        var result = new TreeSet<String>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }
    private static List<String> differences(JsonNode declared) {
        var domain = OneCardBidBelief.domain();
        var errors = new ArrayList<String>();
        require(declared.path("owner").asText().equals(OneCardBidBelief.class.getSimpleName()), "owner", errors);
        JsonNode entry = declared.path("auction");
        var fields = List.of("hand_size", "private_cards", "trick_cards", "history_cards");
        var expected = List.of(domain.handSize(), domain.privateCards(), domain.trickCards(), domain.historyCards());
        for (int index = 0; index < fields.size(); index++) {
            JsonNode value = entry.path(fields.get(index));
            require(value.isIntegralNumber() && value.asInt() == expected.get(index), "auction/" + fields.get(index), errors);
        }
        JsonNode conditioning = declared.path("conditioning");
        var modes = domain.conditionedModes().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        JsonNode list = conditioning.path("modes");
        require(list.isArray() && list.size() == modes.size() && strings(list).equals(modes), "conditioning/modes", errors);
        require(conditioning.path("seats").isIntegralNumber() && conditioning.path("seats").asInt() == domain.conditionedSeats(), "conditioning/seats", errors);
        require(conditioning.path("turned_card_required").isBoolean() && conditioning.path("turned_card_required").asBoolean() == domain.turnedCardRequired(), "conditioning/turned_card_required", errors);
        require(declared.path("fallback").asText().equals("UNCONDITIONED_ONE_CARD_PROBABILITY"), "fallback", errors);
        require(declared.path("forced_dealer_bid").asText().equals("NO_STRENGTH_EVIDENCE"), "forced_dealer_bid", errors);
        return errors;
    }
    private static void require(boolean valid, String field, List<String> errors) { if (!valid) errors.add(field); }
}
