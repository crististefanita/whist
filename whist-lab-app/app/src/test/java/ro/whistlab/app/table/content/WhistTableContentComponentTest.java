package ro.whistlab.app.table.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;
import org.junit.Test;
import ro.cardtable.ui.cards.CardFace;
import ro.cardtable.ui.hand.HandCard;
import ro.cardtable.ui.seat.SeatContent;
import ro.cardtable.ui.table.PlayedCardContent;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Whist rules and copy remain consumer contracts, never shared renderer fixtures. */
public final class WhistTableContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void seatFactsAndBidderProjectionMatchEveryWhistModeAndLanguage() {
        var checks = new ExhaustiveMatrixChecks();
        int scenarios = 0;
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++)
            for (var language : HowToPlay.Language.values()) {
                var state = game(mode, count).snapshot();
                var copy = new WhistUiText(language);
                List<SeatContent> expectedSeats = state.players().stream().map(player -> {
                    boolean active = player.seat() == state.biddingSeat();
                    return new SeatContent(player.seat(), copy.seatName(player), List.of(
                            new SeatContent.Fact(copy.bidLabel() + ":", "—"),
                            new SeatContent.Fact(copy.seatWonLabel() + ":", "0")), active,
                            copy.seatAccessibility(player, player.seat() == state.dealerSeat(), active, true));
                }).toList();
                List<SeatContent> projectedSeats = state.players().stream()
                        .map(player -> WhistTableContent.seat(player, state.biddingSeat(), state.dealerSeat(), true, copy))
                        .toList();
                checks.at(mode + " / " + count + " seats / " + language);
                checks.equal("Seat names, facts, active state and accessible action", expectedSeats, projectedSeats);
                scenarios++;
            }

        atf.gradle.audit.GradleInputs.fixture("Bidder seat matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "languages", List.of(HowToPlay.Language.values()),
                "scenarios", scenarios);
        assertEquals("Every mode, table size and language was evaluated",
                (long) GameMode.values().length * 4 * HowToPlay.Language.values().length, checks.checkedPairs());
        checks.publish();
    }

    @Test public void actorMarkerStaysWithTheCurrentPlayerDuringManualAndAutomaticPlay() {
        var checks = new ExhaustiveMatrixChecks();
        long expectedPairs = 0;
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++)
            for (var language : HowToPlay.Language.values()) {
                expectedPairs += 2L * (count + 2L * count * GameRules.handSizes(mode, count).get(0) + 1);
                var game = game(mode, count);
                var copy = new WhistUiText(language);
                while (game.snapshot().phase() == GamePhase.BIDDING) {
                    var state = game.snapshot();
                    addActorCases(checks, state, state.biddingSeat(), true, mode, count, language, copy);
                    var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                            state.bidsSubmitted() == state.players().size() - 1);
                    game.submitBid(state.biddingSeat(), allowed.get(0));
                }
                while (game.snapshot().phase() == GamePhase.PLAYING) {
                    var state = game.snapshot();
                    int actor = state.currentSeat();
                    addActorCases(checks, state, actor, false, mode, count, language, copy);
                    addActorCases(checks, state, -1, false, mode, count, language, copy, true);
                    var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                    var legal = GameRules.legalCards(state.players().get(actor).hand(), led,
                            state.trump(), state.mode());
                    game.playCard(actor, legal.get(0));
                }
                addActorCases(checks, game.snapshot(), -1, false, mode, count, language, copy);
            }

        atf.gradle.audit.GradleInputs.fixture("Public actor-state matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "languages", List.of(HowToPlay.Language.values()),
                "observations", checks.checkedPairs());
        assertEquals("Both AutoGame states cover bidding, every play, collection and settlement",
                expectedPairs, checks.checkedPairs());
        checks.publish();
    }

    private static void addActorCases(ExhaustiveMatrixChecks checks,
            GameSession.Snapshot state, int expectedSeat, boolean bidding, GameMode mode, int playerCount,
            HowToPlay.Language language, WhistUiText copy) {
        addActorCases(checks, state, expectedSeat, bidding, mode, playerCount, language, copy, false);
    }

    private static void addActorCases(ExhaustiveMatrixChecks checks,
            GameSession.Snapshot state, int expectedSeat, boolean bidding, GameMode mode, int playerCount,
            HowToPlay.Language language, WhistUiText copy, boolean collecting) {
        for (boolean autoGame : new boolean[]{false, true}) {
            var turn = WhistTableContent.turn(state, collecting, autoGame);
            List<Integer> expectedActor = expectedSeat < 0 ? List.of() : List.of(expectedSeat);
            List<Integer> expectedBadge = bidding && expectedSeat == 0 ? List.of() : expectedActor;
            boolean expectedManual = expectedSeat == 0 && !collecting && !autoGame;
            List<Integer> activeSeats = new ArrayList<>(), announcedSeats = new ArrayList<>(), visibleBadges = new ArrayList<>();
            for (var player : state.players()) {
                var seat = WhistTableContent.seat(player, turn.seat(), state.dealerSeat(), bidding, copy);
                if (seat.active()) activeSeats.add(player.seat());
                if (seat.description().contains(copy.seatTurnAction(bidding))) announcedSeats.add(player.seat());
                if (WhistTableContent.showsTurnBadge(seat, bidding)) visibleBadges.add(player.seat());
            }
            checks.at(mode + " / " + playerCount + " seats / " + language + " / " + state.phase()
                    + " / actor " + expectedSeat + " / AutoGame " + autoGame + " / collection " + collecting);
            checks.equal("Actor, manual input, active frame, announcement and visible badge",
                    new ActorObservation(mode, playerCount, language, state.phase(), autoGame, collecting,
                            expectedSeat, expectedManual, expectedActor, expectedActor, expectedBadge),
                    new ActorObservation(mode, playerCount, language, state.phase(), autoGame, collecting,
                            turn.seat(), turn.manual(), activeSeats, announcedSeats, visibleBadges));
        }
    }

    @Test public void handProjectionKeepsCardOrderFacesLegalityAndSnapshotIdentity() {
        var checks = new ExhaustiveMatrixChecks();
        int cardCases = 0;
        long expectedPairs = 0;
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            expectedPairs += GameRules.handSizes(mode, count).get(0) + 3;
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.EN);
            var bidding = game.snapshot();
            var initialHand = bidding.players().get(0).hand();
            var bidProjection = WhistTableContent.hand(bidding, Set.copyOf(initialHand),
                    WhistTableContent.turn(bidding, false, false), true, copy);
            boolean biddingHasPlayableCard = bidProjection.stream().anyMatch(HandCard::playable);
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                var state = game.snapshot();
                var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1);
                game.submitBid(state.biddingSeat(), allowed.get(0));
            }

            var state = game.snapshot();
            List<Card> cards = state.players().get(0).hand();
            Set<Card> legalCards = Set.of(cards.get(0));
            List<HandCard> expectedCards = cards.stream().map(card -> new HandCard(
                    card.suit().name() + ":" + card.rank(),
                    new CardFace(card.rankLabel(), card.suit().symbol(), card.suit().red()),
                    copy.cardLabel(card) + ". " + copy.cardState(false, true, legalCards.contains(card)),
                    legalCards.contains(card))).toList();
            List<HandCard> manualCards = WhistTableContent.hand(state, legalCards,
                    new WhistTableContent.Turn(0, true), false, copy);
            checks.at(mode + " / " + count + " seats / manual hand");
            checks.equal("Bidding exposes no manual card", false, biddingHasPlayableCard);
            checks.equal("Manual cards retain faces, order and legality", expectedCards, manualCards);
            for (int index = 0; index < cards.size(); index++) {
                checks.at(mode + " / " + count + " seats / hand index " + index);
                checks.equal("Selected IDs retain source object identity", true,
                        WhistTableContent.selected(cards, manualCards.get(index).id()) == cards.get(index));
            }

            List<HandCard> expectedAutoplay = cards.stream().map(card -> new HandCard(
                    card.suit().name() + ":" + card.rank(),
                    new CardFace(card.rankLabel(), card.suit().symbol(), card.suit().red()),
                    copy.cardLabel(card) + ". " + copy.cardState(false, false, false), false)).toList();
            List<HandCard> autoplay = WhistTableContent.hand(state, legalCards,
                    new WhistTableContent.Turn(0, false), false, copy);
            checks.at(mode + " / " + count + " seats / AutoGame hand");
            checks.equal("AutoGame cards retain faces without manual input", expectedAutoplay, autoplay);
            cardCases += 2;
        }

        atf.gradle.audit.GradleInputs.fixture("Snapshot-bound hand matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "cardCases", cardCases);
        assertEquals("Every initial hand retains manual, bidding, AutoGame and per-card identity checks",
                expectedPairs, checks.checkedPairs());
        checks.publish();
        assertThrows("A projected hand is immutable", UnsupportedOperationException.class,
                () -> WhistTableContent.hand(game(GameMode.values()[0], 3).snapshot(), Set.of(),
                        new WhistTableContent.Turn(0, false), true,
                        new WhistUiText(HowToPlay.Language.EN)).clear());
        assertThrows("An unknown card ID is rejected", IllegalArgumentException.class,
                () -> WhistTableContent.selected(List.of(), "unknown"));
    }

    @Test public void depositedCardsPreservePublicOrderFacesAndTrickResults() {
        var checks = new ExhaustiveMatrixChecks();
        int partialTricks = 0;
        for (var mode : GameMode.values()) for (int count = 3; count <= 6; count++) {
            var game = game(mode, count);
            var copy = new WhistUiText(HowToPlay.Language.RO);
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                var state = game.snapshot();
                var allowed = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == state.players().size() - 1);
                game.submitBid(state.biddingSeat(), allowed.get(0));
            }
            for (int played = 0; played < count; played++) {
                var state = game.snapshot();
                var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
                int actor = state.currentSeat();
                var legal = GameRules.legalCards(state.players().get(actor).hand(), led,
                        state.trump(), state.mode());
                game.playCard(actor, legal.get(0));
                var after = game.snapshot();
                List<PlayedCard> publicPlays = played == count - 1 ? after.lastTrick() : after.currentTrick();
                List<PlayedCardContent> expectedPlays = publicPlays.stream().map(play -> {
                    var player = after.players().get(play.seat());
                    return new PlayedCardContent(play.seat(), play.card().shortLabel(), play.card().suit().red(),
                            copy.trickPlayer(player), copy.trickCardAccessibility(player, play.card()));
                }).toList();
                List<PlayedCardContent> projectedPlays = WhistTableContent.plays(after, publicPlays, copy);
                checks.at(mode + " / " + count + " seats / deposited cards " + (played + 1));
                checks.equal("Deposited faces, actor order, color and accessible player names",
                        expectedPlays, projectedPlays);
                partialTricks++;
                if (played == 0) {
                    assertThrows("Deposited-card projection is immutable", UnsupportedOperationException.class,
                            projectedPlays::clear);
                }
                if (played == count - 1) {
                    checks.equal("Completed one-trick hands award exactly one trick", 1,
                            after.players().stream().mapToInt(GameSession.PlayerSnapshot::tricksWon).sum());
                }
            }
        }

        atf.gradle.audit.GradleInputs.fixture("Public trick projection matrix", "modes", List.of(GameMode.values()),
                "playerCounts", List.of(3, 4, 5, 6), "partialTricks", partialTricks);
        assertEquals("Every deposited card and each completed trick was evaluated",
                (long) GameMode.values().length * (3 + 4 + 5 + 6 + 4), checks.checkedPairs());
        checks.publish();
    }

    private GameSession game(GameMode mode, int count) {
        return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation",
                new GameConfig(count, mode, AiDifficulty.NORMAL, 49)));
    }

    private record ActorObservation(GameMode mode, int playerCount, HowToPlay.Language language,
            GamePhase phase, boolean autoGame, boolean collecting, int actorSeat, boolean manual,
            List<Integer> activeSeats, List<Integer> announcedSeats, List<Integer> visibleBadges) { }
}
