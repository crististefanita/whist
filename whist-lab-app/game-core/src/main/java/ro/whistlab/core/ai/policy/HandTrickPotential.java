package ro.whistlab.core.ai.policy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

/** Shared, deliberately soft estimate of how many tricks an original hand can support. */
public final class HandTrickPotential {
    private static final double VOID_RUFF_POTENTIAL = .22;
    private static final double SINGLETON_RUFF_POTENTIAL = .03;

    private HandTrickPotential() {}

    public static double score(List<Card> hand, Trump trump) {
        Map<Suit, Integer> lengths = new EnumMap<>(Suit.class);
        boolean[] ace = new boolean[Suit.values().length];
        boolean[] king = new boolean[Suit.values().length];
        boolean[] queen = new boolean[Suit.values().length];
        double value = 0;
        int trumpCount = 0;
        for (Card card : hand) {
            int suit = card.suit().ordinal();
            lengths.merge(card.suit(), 1, Integer::sum);
            ace[suit] |= card.rank() == 14;
            king[suit] |= card.rank() == 13;
            queen[suit] |= card.rank() == 12;
            value += cardValue(card.rank());
            if (trump.suit().filter(s -> s == card.suit()).isPresent()) {
                value += .10;
                trumpCount++;
            }
        }
        double ruffFlexibility = Math.min(1.0, trumpCount / 2.0);
        for (Suit suit : Suit.values()) {
            int length = lengths.getOrDefault(suit, 0);
            if (king[suit.ordinal()] && !ace[suit.ordinal()] && length >= 2) {
                // A small same-suit card can draw or spend the higher ace before the king is used.
                // It is useful cover, not a guarantee that the king will take a trick.
                value += Math.min(.33, .27 + .02 * (length - 2));
            } else if (queen[suit.ordinal()] && !king[suit.ordinal()] && !ace[suit.ordinal()] && length >= 3) {
                // A longer suit gives a queen more chances after the higher honors are exposed.
                value += .10;
            }
            if (trumpCount > 0 && trump.suit().filter(candidate -> candidate != suit).isPresent()) {
                if (length == 0) value += VOID_RUFF_POTENTIAL * ruffFlexibility;
                else if (length == 1) value += SINGLETON_RUFF_POTENTIAL * ruffFlexibility;
            }
        }
        return value;
    }

    public static int estimate(List<Card> hand, Trump trump) {
        return Math.max(0, Math.min(hand.size(), (int) Math.round(score(hand, trump))));
    }

    private static double cardValue(int rank) {
        return switch (rank) {
            case 14 -> .90;
            case 13 -> .45;
            case 12 -> .20;
            case 11 -> .08;
            default -> .04;
        };
    }
}
