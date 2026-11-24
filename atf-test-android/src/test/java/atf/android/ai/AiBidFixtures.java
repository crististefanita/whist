package atf.android.ai;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;

/** Public decision inputs only; no fixture supplies an opponent's private cards. */
final class AiBidFixtures {
    static MonteCarloAi.View view(GameMode mode, int handSize, int seats, boolean exposed) {
        var hand = List.of(new Card(Suit.HEARTS, 14), new Card(Suit.HEARTS, 13),
                new Card(Suit.CLUBS, 7), new Card(Suit.DIAMONDS, 10), new Card(Suit.CLUBS, 14),
                new Card(Suit.HEARTS, 7), new Card(Suit.SPADES, 14), new Card(Suit.CLUBS, 8));
        var players = new ArrayList<MonteCarloAi.Seat>();
        for (int seat = 0; seat < seats; seat++) players.add(new MonteCarloAi.Seat(handSize, seat == 0 ? 0 : null, 0));
        return new MonteCarloAi.View(mode, Trump.SPADES, 1, seats - 1, handSize,
                hand.subList(0, handSize), players, List.of(), List.of(),
                exposed ? new Card(Suit.SPADES, 8) : null);
    }

    static MonteCarloAi.View shape(MonteCarloAi.View base, boolean table, boolean history) {
        var card = new PlayedCard(0, new Card(Suit.DIAMONDS, 7));
        return new MonteCarloAi.View(base.mode(), base.trump(), base.seat(), base.dealer(), base.handSize(),
                base.hand(), base.seats(), table ? List.of(card) : List.of(), history ? List.of(card) : List.of(), base.turnedCard());
    }
    private AiBidFixtures() { }
}
