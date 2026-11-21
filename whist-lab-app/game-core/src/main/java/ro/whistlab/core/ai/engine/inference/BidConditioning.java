package ro.whistlab.core.ai.engine.inference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.ai.policy.HandTrickPotential;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

/** Soft public-bid evidence for hypothetical four-seat Romanian 1–8–1 deals with two to eight cards. */
public final class BidConditioning {
    private static final double UNINFORMED_MIX = .65;
    private static final double STRENGTH_MIX = 1 - UNINFORMED_MIX;
    private static final double BID_DISPERSION = .90;
    private static final double DEFAULT_LIKELIHOOD_TEMPERATURE = .50;
    private static final double TWO_CARD_LIKELIHOOD_TEMPERATURE = .25;

    private BidConditioning() {}

    public static int evidenceCount(View view) {
        if (!supported(view)) return 0;
        int count = 0;
        int prefix = 0;
        boolean prefixKnown = true;
        int seat = (view.dealer() + 1) % view.seats().size();
        for (int position = 0; position < view.seats().size(); position++) {
            Seat publicSeat = view.seats().get(seat);
            Integer bid = publicSeat.bid();
            if (bid != null && seat != view.seat() && prefixKnown) {
                List<Integer> legal = GameRules.allowedBids(view.handSize(), prefix,
                        position == view.seats().size() - 1);
                if (legal.size() > 1 && legal.contains(bid)) count++;
            }
            if (bid == null) prefixKnown = false;
            else prefix += bid;
            seat = (seat + 1) % view.seats().size();
        }
        return count;
    }

    /** Likelihood is bounded away from zero so a bid remains evidence, never a card reveal. */
    public static double likelihood(View view, List<List<Card>> possibleHands) {
        if (!supported(view) || possibleHands.size() != view.seats().size()) return 1.0;
        double logWeight = 0;
        int evidence = 0;
        int prefix = 0;
        boolean prefixKnown = true;
        int seat = (view.dealer() + 1) % view.seats().size();
        for (int position = 0; position < view.seats().size(); position++) {
            Seat publicSeat = view.seats().get(seat);
            Integer bid = publicSeat.bid();
            if (bid != null && seat != view.seat() && prefixKnown) {
                List<Integer> legal = GameRules.allowedBids(view.handSize(), prefix,
                        position == view.seats().size() - 1);
                if (legal.size() > 1 && legal.contains(bid)) {
                    List<Card> originalHand = originalHand(view, possibleHands, seat);
                    if (originalHand.size() == view.handSize()) {
                        double likelihood = bidLikelihood(bid, legal,
                                HandTrickPotential.score(originalHand, view.trump()));
                        logWeight += Math.log(likelihood);
                        evidence++;
                    }
                }
            }
            if (bid == null) prefixKnown = false;
            else prefix += bid;
            seat = (seat + 1) % view.seats().size();
        }
        double temperature = view.handSize() == 2
                ? TWO_CARD_LIKELIHOOD_TEMPERATURE
                : DEFAULT_LIKELIHOOD_TEMPERATURE;
        return evidence == 0 ? 1.0 : Math.exp(logWeight * temperature);
    }

    private static double bidLikelihood(int observed, List<Integer> legal, double strength) {
        double total = 0;
        double observedModel = 0;
        for (int bid : legal) {
            double distance = bid - strength;
            double probability = Math.exp(-(distance * distance) / (2 * BID_DISPERSION * BID_DISPERSION));
            total += probability;
            if (bid == observed) observedModel = probability;
        }
        double model = observedModel / total;
        return UNINFORMED_MIX / legal.size() + STRENGTH_MIX * model;
    }

    private static List<Card> originalHand(View view, List<List<Card>> possibleHands, int seat) {
        Set<Card> cards = new LinkedHashSet<>(possibleHands.get(seat));
        for (PlayedCard play : view.history()) if (play.seat() == seat) cards.add(play.card());
        for (PlayedCard play : view.trick()) if (play.seat() == seat) cards.add(play.card());
        return new ArrayList<>(cards);
    }

    private static boolean supported(View view) {
        return view.mode() == GameMode.ROMANIAN_WHIST
                && view.seats().size() == 4
                && view.handSize() >= 2
                && view.handSize() <= 8;
    }
}
