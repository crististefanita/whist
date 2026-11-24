package ro.cardtable.testing.browser;

import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import static ro.cardtable.testing.browser.BrowserCheckSupport.*;

import static ro.cardtable.testing.browser.WhistBrowserLayoutScenarios.*;
import static ro.cardtable.testing.browser.WhistBrowserCardScenarios.*;
import static ro.cardtable.testing.browser.WhistBrowserTableScenarios.*;

/** Preserves the existing diagnostic selection, ordering, inputs and scenario identities. */
public final class WhistBrowserChecks {
    private WhistBrowserChecks() { }
    @FunctionalInterface public interface Scenario { Map<String,Object> run(Page page,List<Image> images); }
    public record Check(Browser.NewContextOptions options,Path output,String id,String name,String expected,Scenario scenario) { }
    public static List<Check> plan(String check,String url,String checkpoint,String wideCheckpoint,Path output) {
        List<Check> checks = new ArrayList<>();
        if (check.equals("all") || check.equals("playback")) {
            checks.add(new Check(mobile(), output, "WEB-COMPONENT-PLAYBACK-PAINT",
                    "The table stays stable when an opponent returns control to the human",
                    "After collection and an opponent's deposit, the board stays settled until a legal human move is available.",
                    (page, images) -> stablePlaybackPaint(page, url, checkpoint, output, images)));
        }
        if (check.equals("all") || check.equals("seats")) {
            for (int players : List.of(5, 6)) {
                var viewport = players == 5 ? mobile() : mobile().setViewportSize(360, 800);
                checks.add(new Check(viewport, output, "WEB-COMPONENT-SEATS-" + players,
                        "Stable frames with " + players + " players",
                        "Coordinates remain equal across bidding, play and collection in two one-card hands.",
                        (page, images) -> stableSeats(page, url, players, output, images)));
            }
        }
        if (check.equals("all") || check.equals("card-face")) {
            checks.add(new Check(mobile().setViewportSize(360, 800), output, "WEB-COMPONENT-TEXT",
                    "Whist fonts on a controlled probe panel",
                    "All thirteen ranks are enlarged by 50 percent; suits and trump retain their size.",
                    (page, images) -> rankTypography(page, url)));
            checks.add(new Check(mobile(), output, "WEB-COMPONENT-CARD-INK",
                    "Suit colors remain equal in the hand, on the table and during collection",
                    "Each of the four suits retains its ink and text rendering.",
                    (page, images) -> cardInk(page, url)));
            checks.add(new Check(mobile(), output, "WEB-UI-AUTO", "Card faces in the hand, at deposit and during collection",
                    "Ranks, suits and their positions remain proportional across surfaces.",
                    (page, images) -> autoGame(page, url, checkpoint, output, images)));
        }
        if (check.equals("typography")) {
            checks.add(new Check(mobile(), output, "CARD-TYPOGRAPHY",
                    "Shared rank typography for 2–A", "Ranks scale independently of menus; suits and trump remain unchanged.",
                    (page, images) -> rankTypography(page, url)));
        }
        if (check.equals("all") || check.equals("header")) {
            for (int width : List.of(414, 360)) {
                checks.add(new Check(mobile().setViewportSize(width, 896), output, "WEB-12-HEADER-" + width,
                        "Header at 200 percent text scale and width " + width,
                        "Bid facts occupy two unclipped lines immediately below progress.",
                        (page, images) -> enlargedHeader(page, url, wideCheckpoint)));
            }
        }
        if (check.equals("all") || check.equals("dedication")) {
            checks.add(new Check(mobile(), output, "WEB-COMPONENT-HOME-DEDICATION",
                    "Home dedication in Romanian and English",
                    "The photo follows game rules and the caption changes immediately with the language.",
                    (page, images) -> homeDedication(page, url, output, images)));
        }
        if (check.equals("all") || check.equals("viewport")) {
            checks.add(new Check(mobile().setViewportSize(400, 860), output, "WEB-VIEWPORT-PHONE",
                    "Mobile viewport fits the adaptive scene in portrait and landscape",
                    "The logical width follows available CSS space and content demand determines the uniform fit.",
                    (page, images) -> responsiveViewport(page, url, output, true, images)));
            checks.add(new Check(new Browser.NewContextOptions().setViewportSize(1280, 720), output,
                    "WEB-VIEWPORT-DESKTOP",
                    "Desktop viewport scales uniformly and stays centered after resize",
                    "Height determines scale when width permits; narrow windows remain within available width.",
                    (page, images) -> responsiveViewport(page, url, output, false, images)));
        }
        if (check.equals("all") || check.equals("trump")) {
            checks.add(new Check(mobile(), output, "WEB-COMPONENT-TRUMP",
                    "Trump frame in Descending Whist",
                    "Color, border, dimensions and text remain identical when AutoPlay is enabled or disabled.",
                    (page, images) -> descendingTrump(page, url)));
        }
        if (check.equals("all") || check.equals("numbers") || check.equals("typography")) {
            checks.add(new Check(mobile().setViewportSize(360, 800), output, "WEB-UI-NUMBERS",
                    "Rank 10 in a seventeen-card hand at 200 percent text scale",
                    "Numeric and face ranks retain one anchor; the header respects text scale.",
                    (page, images) -> enlargedNumbers(page, url, wideCheckpoint, output, images)));
        }
        if (check.equals("all")) {
            checks.add(new Check(mobile(), output, "WEB-UI-17", "Hand fan and bidding at 414×896",
                    "Seventeen accessible cards and unchanged seats after keyboard bidding",
                    (page, images) -> responsiveHand(page, url)));
            checks.add(new Check(new Browser.NewContextOptions().setViewportSize(535, 760), output,
                    "WEB-UI-HEADER-DESKTOP", "Oh Hell header in the scaled desktop frame",
                    "Title and progress retain one anchor while trump stays at the useful edge",
                    (page, images) -> desktopHeader(page, url)));
            checks.add(new Check(new Browser.NewContextOptions().setViewportSize(1280, 720), output,
                    "WEB-UI-DESKTOP", "Local profile and desktop centering",
                    "Cancel and save update the menu while the portrait frame stays centered after resize",
                    (page, images) -> desktop(page, url)));
        }
        require(!checks.isEmpty(), "Unknown browser check: " + check);
        return List.copyOf(checks);
    }
}
