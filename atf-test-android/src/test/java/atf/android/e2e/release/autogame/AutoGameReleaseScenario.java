package atf.android.e2e.release.autogame;

import atf.android.e2e.release.ReleaseBidContext;
import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.hierarchy.HierarchyDocument;
import java.util.Map;
import java.util.TreeMap;
import java.time.Duration;
import java.util.List;

/** Verifies the bid-screen AutoGame control and its one-hand boundary on the signed APK. */
public final class AutoGameReleaseScenario {
    public static atf.android.evidence.flow.UiSanityPlan plan() throws java.io.IOException {
        return atf.android.evidence.flow.UiSanityPlan.load(atf.android.e2e.support.flow.WhistLabScenario.checkoutRoot()
                .resolve("config/table-sanity-plan.json"));
    }

    public static void run(ReleaseBidContext c) throws Exception { var plan = plan(); c.useSanityPlan(plan); run(c, plan); }

    public static void run(ReleaseBidContext c, atf.android.evidence.flow.UiSanityPlan plan) throws Exception {
        String installed = c.installedHash();
        if (!c.setup.required("requestedApkSha256").asText().equals(installed))
            throw new IllegalStateException("Installed APK differs from the signed candidate");
        c.ui.restartToActivity(ReleaseBidContext.PACKAGE, ".MainActivity");
        boolean ro = c.ui.hasText(c.ui.dump(), "Joc nou");
        Labels labels = new Labels(ro);
        c.ui.tapText(labels.newGame());
        c.ui.selectNamedSpinnerOption(labels.players(), "4");
        c.ui.selectNamedSpinnerOption(labels.mode(), ro ? "Românesc (1–8–1)" : "Romanian (1–8–1)");
        c.ui.selectNamedSpinnerOption(labels.difficulty(), ro ? "Dificil" : "Hard");
        c.ui.tapText(labels.start());
        var biddingFrames = new java.util.concurrent.atomic.AtomicReference<Map<String, Bounds>>();
        plan.execute(step -> {
            switch (step) {
                case "bidding" -> {
                    c.awaitRecommendation(labels.submit());
                    biddingFrames.set(seatFrames(c.ui.dump(), labels));
                    c.sanityCheckpoint(step, "Manual hand awaits its bid", xml -> List.of(
                            ReleaseBidContext.equal("Installed APK matches the candidate", c.setup.required("requestedApkSha256").asText(), installed),
                            ReleaseBidContext.equal("Four real seats are present", 4, seatFrames(xml, labels).size()),
                            ReleaseBidContext.equal("AutoGame starts unchecked", true, c.ui.hasDescription(xml, labels.startAutoGame())),
                            ReleaseBidContext.equal("Header placement", true, headerPlacement(c, xml, labels))));
                }
                case "before-human" -> {
                    c.ui.tapText(labels.submit());
                    c.ui.awaitDescriptionContaining(ro ? "Carte legală" : "Legal card", Duration.ofSeconds(30));
                    c.sanityCheckpoint(step, "Human turn before the legal card", xml -> List.of(
                            ReleaseBidContext.equal("Exactly one legal private card", 1, c.ui.legalCardDescriptions(xml).size()),
                            ReleaseBidContext.equal("Bid and play preserve seat rectangles", biddingFrames.get(), seatFrames(xml, labels))));
                }
                case "after-human" -> {
                    String xml = c.ui.dump();
                    String legal = c.ui.legalCardDescriptions(xml).stream().findFirst().orElseThrow();
                    c.ui.tapDescription(legal);
                    c.awaitTextAndDismissAchievementNotice(labels.handComplete(), Duration.ofSeconds(45));
                    c.sanityCheckpoint(step, "The human card reaches the score boundary", current -> List.of(
                            ReleaseBidContext.equal("Real hand reached score", true, c.ui.hasText(current, labels.handComplete()))));
                }
                case "manual-score" -> c.sanityCheckpoint(step, "Manual hand has no stale actor", xml -> List.of(
                        ReleaseBidContext.equal("Score has no play badge", 0L, badgeCount(xml, labels.playBadge()))));
                case "auto-bidding" -> {
                    c.revealPostHandAction(labels.next()); c.ui.tapText(labels.next());
                    c.awaitRecommendation(labels.submit());
                    c.sanityCheckpoint(step, "Next hand is ready for AutoGame", xml -> List.of(
                            ReleaseBidContext.equal("One-card automatic hand", true, c.ui.hasDescriptionContaining(xml, labels.handPrefix()+"1")),
                            ReleaseBidContext.equal("AutoGame remains unchecked before explicit selection", true, c.ui.hasDescription(xml, labels.startAutoGame()))));
                }
                case "auto-score" -> {
                    c.ui.tapText(labels.autoGameToggle());
                    c.awaitTextAndDismissAchievementNotice(labels.handComplete(), Duration.ofSeconds(45));
                    c.sanityCheckpoint(step, "AutoGame completes its one real hand", xml -> List.of(
                            ReleaseBidContext.equal("Automatic hand reached score", true, c.ui.hasText(xml, labels.handComplete())),
                            ReleaseBidContext.equal("Score has no stale actor", 0L, badgeCount(xml, labels.playBadge())+badgeCount(xml, labels.bidBadge()))));
                }
                case "return-control" -> {
                    c.revealPostHandAction(labels.next()); c.ui.tapText(labels.next()); c.awaitRecommendation(labels.submit());
                    c.sanityCheckpoint(step, "Control returns without playing a third hand", xml -> List.of(
                            ReleaseBidContext.equal("Manual bid is available", true, c.ui.hasText(xml, labels.submit())),
                            ReleaseBidContext.equal("AutoGame is unchecked", true, c.ui.hasDescription(xml, labels.startAutoGame())&&!c.ui.hasDescription(xml, labels.stopAutoGame()))));
                }
                default -> throw new IllegalArgumentException("Unsupported paired step: " + step);
            }
        });
    }

    private static Map<String, Bounds> seatFrames(String xml, Labels labels) throws Exception {
        Map<String, Bounds> frames = new TreeMap<>();
        for (var node : HierarchyDocument.nodes(xml)) {
            int marker = node.description().indexOf(labels.seatMarker());
            if (marker > 0 && node.className().equals("android.widget.LinearLayout")) {
                frames.put(node.description().substring(0, marker), HierarchyDocument.toBounds(node.bounds()));
            }
        }
        return frames;
    }

    private static long badgeCount(String xml, String label) throws Exception {
        return HierarchyDocument.nodes(xml).stream().filter(node -> node.text().equals(label)).count();
    }

    private static boolean privateHandIsVisible(ReleaseBidContext c, String xml, Labels labels,
            Map<String, Bounds> seats) throws Exception {
        int expected = privateHandHeight(c);
        int seatBottom = seats.values().stream().mapToInt(Bounds::bottom).max().orElseThrow();
        Bounds hand = c.ui.boundsForDescriptionContaining(xml, labels.handPrefix());
        return HierarchyDocument.nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.FrameLayout"))
                .filter(node -> node.description().contains(labels.cardStateMarker()))
                .map(node -> HierarchyDocument.toBounds(node.bounds()))
                .anyMatch(card -> card.bottom() - card.top() == expected
                        && card.top() >= seatBottom && card.top() >= hand.top() && card.bottom() <= hand.bottom()
                        && card.left() >= hand.left() && card.right() <= hand.right());
    }

    private static int privateHandHeight(ReleaseBidContext context) {
        String displayProfile = context.setup.path("displayProfile").asText();
        int densityDpi = displayProfile.matches("[1-9]\\d*x[1-9]\\d*@[1-9]\\d*")
                ? Integer.parseInt(displayProfile.substring(displayProfile.indexOf('@') + 1))
                : context.setup.required("display").required("density").asInt();
        if (densityDpi <= 0) throw new IllegalStateException("Missing native display density");
        float density = densityDpi / 160f;
        return Math.round(88 * density) + Math.round(28 * density);
    }

    private static boolean headerPlacement(ReleaseBidContext c, String xml, Labels labels) throws Exception {
        if (!c.ui.hasText(xml, labels.autoGameToggle()) || !c.ui.hasText(xml, labels.title())) return false;
        Bounds title = c.ui.boundsForText(xml, labels.title());
        Bounds toggle = c.ui.boundsForText(xml, labels.autoGameToggle());
        Bounds trump = c.ui.hasDescriptionContaining(xml, labels.trumpPrefix())
                ? c.ui.boundsForDescriptionContaining(xml, labels.trumpPrefix())
                : c.ui.boundsForDescription(xml, labels.noTrump());
        boolean sameLine = toggle.top() < title.bottom() && title.top() < toggle.bottom();
        boolean separateLine = toggle.top() >= title.bottom() && toggle.right() >= title.right();
        return (sameLine && toggle.left() >= title.right() || separateLine)
                && toggle.bottom() <= trump.top() && toggle.bottom() <= c.ui.boundsForText(xml, labels.submit()).top();
    }

    private record Labels(boolean ro) {
        String newGame() { return ro ? "Joc nou" : "New game"; }
        String players() { return ro ? "Jucători" : "Players"; }
        String mode() { return ro ? "Varianta de joc" : "Game type"; }
        String difficulty() { return ro ? "Nivelul adversarilor" : "AI level"; }
        String start() { return ro ? "Începe partida" : "Start game"; }
        String submit() { return ro ? "Licitează" : "Place bid"; }
        String handComplete() { return ro ? "Mână încheiată" : "Hand complete"; }
        String bidBadge() { return ro ? "LICITEAZĂ" : "BID NOW"; }
        String playBadge() { return ro ? "JOACĂ ACUM" : "PLAY NOW"; }
        String seatMarker() { return ro ? ". Licitat " : ". Bid "; }
        String handPrefix() { return ro ? "Cărțile tale, " : "Your hand, "; }
        String cardStateMarker() { return ". "; }
        String next() { return ro ? "Mâna următoare" : "Next hand"; }
        String title() { return ro ? "Whist · Românesc" : "Whist · Romanian"; }
        String trumpPrefix() { return ro ? "Atu " : "Trump "; }
        String noTrump() { return ro ? "Fără atu" : "No trump"; }
        String autoGameToggle() { return ro ? "Automat" : "Auto play"; }
        String startAutoGame() { return ro ? "Joacă automat mâna curentă" : "Auto-play this hand"; }
        String stopAutoGame() { return ro ? "Oprește jocul automat" : "Stop auto-play"; }
    }

    private AutoGameReleaseScenario() { }
}
