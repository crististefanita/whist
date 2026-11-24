package ro.cardtable.testing.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static ro.cardtable.testing.browser.BrowserCheckSupport.*;

/** Test-only Whist diagnostic scenario drivers; no browser lifecycle or report publication. */
final class WhistBrowserTableScenarios {

    @SuppressWarnings("unchecked")
    static Map<String, Object> descendingTrump(Page page, String url) {
        page.navigate(url);
        button(page, "Joc nou").click();
        page.locator("select[name=mode]").selectOption("DESCENDING");
        button(page, "Începe partida").click();
        var table = page.locator("#table");
        var trump = table.locator(".whist-trump");
        table.locator(".whist-auto input").waitFor();
        assertThat(trump.locator(".whist-trump-value")).hasText("♠");
        assertThat(trump).hasCSS("border-radius", "11px");
        assertThat(trump).hasCSS("background-color", "rgb(241, 230, 200)");
        assertThat(trump.locator(".whist-trump-value")).hasCSS("color", "rgb(41, 54, 46)");
        var states = (Map<String, Object>) table.evaluate("""
                table => {
                   const tile = table.querySelector(".whist-trump");
                   const control = table.querySelector(".whist-auto input");
                   const appearance = () => {
                     const style = getComputedStyle(tile);
                     const value = getComputedStyle(tile.querySelector(".whist-trump-value"));
                     const { x, y, width, height } = tile.getBoundingClientRect();
                     return { x, y, width, height, background: style.backgroundColor,
                              border: style.boxShadow, ink: value.color, text: tile.textContent };
                   };
                   const off = appearance();
                   control.click();
                   const toggledOn = control.checked && table.hasAttribute("data-automatic");
                   const on = appearance();
                   control.click();
                   const toggledOff = !control.checked && !table.hasAttribute("data-automatic");
                   return { off, on, restored: appearance(), toggledOn, toggledOff };
                }
                """);
        require(Boolean.TRUE.equals(states.get("toggledOn")) && Boolean.TRUE.equals(states.get("toggledOff")),
                "AutoPlay did not toggle");
        require(states.get("off").equals(states.get("on")) && states.get("off").equals(states.get("restored")),
                "AutoPlay changed the trump tile");
        return states;
    }

    static Map<String, Object> enlargedHeader(Page page, String url, String checkpoint) {
        page.addInitScript("""
                document.addEventListener("DOMContentLoaded", () => {
                  document.documentElement.style.fontSize = "200%";
                }, { once: true });
                """);
        resumeHand(page, url, JSON.toJson(Map.of("checkpoint", checkpoint)));
        assertHeader(page, true);
        require((boolean) page.evaluate("""
                () => [...document.fonts].some(font =>
                  font.family === "CardTableSuits" && font.status === "loaded")
                """), "Card-suit font is missing");
        return Map.of("summary", page.locator(".whist-bids").innerText(),
                "fontPx", page.locator(".whist-bids").evaluate("node => getComputedStyle(node).fontSize"),
                "heightPx", page.locator(".whist-bids").boundingBox().height);
    }

    static Map<String, Object> stablePlaybackPaint(Page page, String url, String checkpoint,
                                                            Path output, List<Image> images) {
        page.addInitScript("""
                const send = Worker.prototype.postMessage;
                let actionSent = false;
                Worker.prototype.postMessage = function (message) {
                  if (message.path === "/api/action") actionSent = true;
                  if (message.path === "/api/layout" &&
                      actionSent && document.querySelector("#table .board:not([data-layout])")) {
                    actionSent = false;
                    window.releaseLayout = () => send.call(this, message);
                    document.documentElement.dataset.layoutHeld = "true";
                    return;
                  }
                  send.call(this, message);
                };
                """);
        resumeHand(page, url, checkpoint);
        var trump = page.locator(".whist-trump:visible");
        var manualTrump = trumpAppearance(trump);
        page.locator(".fan button[data-face='8♦']").tap();
        var collection = page.locator(".collection .whist-play-face");
        assertThat(collection).hasCount(4);
        var collected = List.of("9♦", "Q♥", "K♦", "8♦");
        for (int index = 0; index < collected.size(); index++) {
            assertThat(collection.nth(index)).hasAttribute("data-face", collected.get(index));
        }
        var collectingTrump = trumpAppearance(trump);
        page.locator("html[data-layout-held]").waitFor();
        try {
            var visibleBoard = page.locator(".board:visible");
            assertThat(visibleBoard).hasCount(1);
            require("ready".equals(visibleBoard.getAttribute("data-layout")),
                    "Playback showed a board with fallback positions while the Worker layout was pending");
            assertThat(visibleBoard.locator(".whist-play-face")).hasAttribute("data-face", "A♠");
            images.add(screenshot(page, output, "WEB-COMPONENT-PLAYBACK-WAIT", "Tabla în timp ce geometria nouă este în așteptare"));
        } finally {
            page.evaluate("() => window.releaseLayout()");
        }
        assertThat(page.locator("#table")).hasAttribute("aria-busy", "false");
        assertThat(page.locator(".board:visible")).hasAttribute("data-layout", "ready");
        assertThat(page.locator(".fan .card")).hasCount(3);
        assertThat(page.locator(".seat.south.active .seat-turn")).hasText("JOACĂ ACUM");
        assertThat(page.locator(".fan .legal-card")).hasAttribute("data-face", "10♦");
        require(manualTrump.equals(collectingTrump), "Collection changed the trump frame: " + collectingTrump);
        return Map.of("collection", List.of("9♦", "Q♥", "K♦", "8♦"), "remainingCards", 3,
                "opponentLead", "A♠", "legalCardOnReturn", "10♦",
                "layoutReadyWhilePending", true, "trumpFrame", manualTrump);
    }

    static Map<String, Object> stableSeats(Page page, String url, int players,
                                                   Path output, List<Image> images) {
        page.navigate(url);
        button(page, "Joc nou").click();
        page.locator("select[name=players]").selectOption(String.valueOf(players));
        page.locator("select[name=mode]").selectOption("ROMANIAN_WHIST");
        button(page, "Începe partida").click();
        var observations = new LinkedHashMap<String, Object>();
        var board = page.locator("#table[aria-busy=false] .board[data-layout=ready]");
        for (boolean automatic : List.of(false, true)) {
            board.waitFor();
            assertThat(board.locator(".seat")).hasCount(players);
            assertThat(page.locator(".fan .card")).hasCount(1);
            var bid = button(page, "Licitează");
            assertThat(bid).isVisible();
            var initial = seats(page);
            if (automatic) {
                page.getByRole(AriaRole.CHECKBOX,
                        new Page.GetByRoleOptions().setName("Automat").setExact(true)).tap();
            } else {
                bid.tap();
                var legal = page.locator("#table[aria-busy=false] .fan .legal-card");
                legal.waitFor();
                var playing = seats(page);
                require(initial.equals(playing), "Seat frames moved after bidding: " + initial + " -> " + playing);
                legal.tap();
            }
            assertThat(page.locator(".collection .whist-play-face")).hasCount(players);
            var collecting = seats(page);
            require(initial.equals(collecting), "Seat frames moved at collection: " + initial + " -> " + collecting);
            String mode = automatic ? "AutoGame" : "manual";
            images.add(screenshot(page, output, "WEB-COMPONENT-SEATS-" + players + "-" + mode,
                    "Colectare " + mode + " la " + players + " jucători"));
            assertThat(page.locator(".whist-score tbody tr")).hasCount(players);
            observations.put(mode, Map.of("seatFrames", collecting, "collectedCards", players));
            if (!automatic) {
                var notice = page.locator("dialog.achievement-notice[open]");
                if (notice.isVisible()) notice.getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Închide").setExact(true)).click();
                button(page, "Mâna următoare").click();
            }
        }
        return observations;
    }
}
