package ro.cardtable.testing.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static ro.cardtable.testing.browser.BrowserCheckSupport.*;

/** Test-only Whist diagnostic scenario drivers; no browser lifecycle or report publication. */
final class WhistBrowserCardScenarios {

    static Map<String, Object> rankTypography(Page page, String url) {
        var ranks = List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
        String cards = ranks.stream().map(rank -> """
                <button class="card"><span class="card-index"><span class="card-rank">%s</span><span>♦</span></span></button>
                """.formatted(rank)).collect(java.util.stream.Collectors.joining());
        String fixture = url + "presentation-fixture.html";
        page.route(fixture, route -> route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8")
                .setBody("""
                        <!doctype html><html lang="ro"><meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <link rel="stylesheet" href="table.css">
                        <body data-surface="whist" style="--whist-hand-label: .8125rem; --whist-hand-rank-cap: 26px;
                          --whist-hand-face-width: 63px; --whist-hand-face-height: 88px; --whist-trump-value-size: 1.25rem">
                        <section class="whist-match"><div class="whist-trump"><span class="card"><span class="card-rank">A</span></span></div>
                        <div class="hand"><div class="fan">%s</div></div></section></body></html>
                        """.formatted(cards))));
        page.navigate(fixture);
        var labels = page.locator(".fan .card-rank");
        assertThat(labels).hasText(ranks.toArray(String[]::new));
        for (int scale : List.of(100, 200)) {
            page.evaluate("(scale) => document.documentElement.style.fontSize = scale + '%'", scale);
            for (var label : labels.all()) assertThat(label).hasCSS("font-size", scale == 100 ? "19.5px" : "39px");
            assertThat(page.locator(".fan .card-index span:last-child").first()).hasCSS("font-size", scale == 100 ? "13px" : "26px");
            assertThat(page.locator(".fan .card").first()).hasCSS("width", "63px");
        }
        page.evaluate("() => document.documentElement.style.fontSize = '100%'");
        assertThat(page.locator(".whist-trump .card-rank")).hasCSS("font-size", "20px");
        return Map.of("ranks", ranks, "fontPx", List.of(19.5, 39), "suitPx", List.of(13, 26), "trumpPx", 20);
    }

    static Map<String, Object> cardInk(Page page, String url) {
        String fixture = url + "card-ink.html";
        page.route(fixture, route -> route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8")
                .setBody("""
                        <!doctype html><html lang="ro"><meta charset="utf-8">
                        <link rel="stylesheet" href="table.css">
                        <body data-surface="whist" style="--whist-played-red:#ad393a;--whist-played-dark:#2a352f">
                          <main class="whist-match"><div class="hand"><div class="fan"></div></div>
                            <div class="cards-on-table"></div><div class="collection"></div></main>
                        </body></html>
                        """)));
        page.navigate(fixture);
        var suits = List.of("♠", "♥", "♣", "♦");
        page.evaluate("""
                suits => {
                  for (const container of document.querySelectorAll(".fan, .cards-on-table, .collection")) {
                    for (const suit of suits) {
                      const card = document.createElement("span");
                      const hand = container.matches(".fan");
                      card.className = hand ? "card" : "card whist-play-face";
                      card.classList.toggle("red", suit === "♥" || suit === "♦");
                      const symbol = document.createElement("span");
                      symbol.className = "suit";
                      symbol.textContent = suit;
                      card.append(symbol);
                      container.append(card);
                    }
                  }
                }
                """, suits);
        var colors = List.of("rgb(42, 53, 47)", "rgb(173, 57, 58)", "rgb(42, 53, 47)", "rgb(173, 57, 58)");
        for (String surface : List.of(".fan", ".cards-on-table", ".collection")) {
            var symbols = page.locator(surface + " .suit");
            assertThat(symbols).hasText(suits.toArray(String[]::new));
            for (int index = 0; index < suits.size(); index++) {
                assertThat(symbols.nth(index)).hasCSS("color", colors.get(index));
                assertThat(symbols.nth(index)).hasCSS("font-variant-emoji", "text");
            }
        }
        return Map.of("suits", suits, "ink", colors, "surfaces", List.of("hand", "table", "collection"));
    }

    static Map<String, Object> enlargedNumbers(Page page, String url, String checkpoint,
                                                       Path output, List<Image> images) {
        page.addInitScript("""
                document.addEventListener("DOMContentLoaded", () => {
                  document.documentElement.style.fontSize = "200%";
                }, { once: true });
                """);
        resumeHand(page, url, JSON.toJson(Map.of("checkpoint", checkpoint)));
        var hand = page.locator(".hand");
        var cards = hand.locator(".fan .card");
        assertThat(cards).hasCount(17);
        var ten = cards.and(page.locator("[data-face^='10']")).first();
        String face = ten.getAttribute("data-face");
        var rank = ten.locator(".card-index:not(.inverted) .card-rank");
        var suit = ten.locator(".card-index:not(.inverted) span:last-child");
        assertThat(rank).hasText("10");
        assertThat(rank).hasCSS("font-size", "39px");
        assertThat(suit).hasCSS("font-size", "26px");
        assertHeader(page, true);
        var tenCard = ten.boundingBox();
        var tenRank = rank.boundingBox();
        for (String faceRank : List.of("A", "K", "Q", "J")) {
            var figure = cards.and(page.locator("[data-face^='" + faceRank + "']")).first();
            var figureRank = figure.locator(".card-index:not(.inverted) .card-rank");
            assertThat(figureRank).hasCSS("font-size", "39px");
            var rect = figure.boundingBox();
            var glyph = figureRank.boundingBox();
            near(tenRank.x + tenRank.width / 2 - tenCard.x, glyph.x + glyph.width / 2 - rect.x);
            near(tenRank.y - tenCard.y, glyph.y - rect.y);
        }
        rank.scrollIntoViewIfNeeded();
        var next = ten.locator("xpath=following-sibling::button[1]").boundingBox();
        var glyphs = rank.boundingBox();
        var scrollport = hand.boundingBox();
        require(glyphs.x >= scrollport.x - 1 && glyphs.x + glyphs.width <= scrollport.x + scrollport.width + 1,
                "The scrollport clips " + face);
        require(glyphs.x + glyphs.width <= next.x, "The next card covers " + face);
        require(ten.boundingBox().y + ten.boundingBox().height <= scrollport.y + scrollport.height + 1,
                "The scrollport clips the card vertically");
        images.add(screenshot(page, output, "WEB-UI-NUMBERS-10", "Rangul 10 înaintea cărții următoare"));
        cards.last().scrollIntoViewIfNeeded();
        var last = cards.last().boundingBox();
        require(last.x >= scrollport.x - 1 && last.x + last.width <= scrollport.x + scrollport.width + 1,
                "The last card cannot be shown in full at 200%");
        return Map.of("viewport", List.of(360, 800), "textScale", 2, "cards", 17, "card", face,
                "rankPixels", 39, "suitPixels", 26,
                "rankWidth", glyphs.width, "clearanceToNextCard", next.x - glyphs.x - glyphs.width);
    }

    static Map<String, Object> autoGame(Page page, String url, String checkpoint, Path output, List<Image> images) {
        resumeHand(page, url, checkpoint);
        var ten = page.locator(".fan [data-face^='10'] .card-index:not(.inverted) .card-rank");
        assertThat(ten).hasText("10");
        assertThat(ten).hasCSS("font-size", "19.5px");
        assertThat(page.locator(".fan [data-face^='10'] .card-index.inverted .card-rank")).hasCSS("font-size", "13.5px");
        assertThat(page.locator(".fan [data-face^='10'] .suit")).hasCSS("font-size", "28px");
        var held = page.locator(".fan [data-face='8♦']");
        var heldFace = faceGeometry(held);
        assertHeader(page, false);
        var trump = page.locator(".whist-trump .card");
        assertThat(trump.locator(".card-index:not(.inverted) .card-rank")).hasText("Q");
        assertThat(trump.locator(".suit")).hasText("♦");
        assertThat(trump).hasCSS("flex-direction", "row");
        assertThat(trump.locator(".card-rank").first()).hasCSS("font-size", "20px");
        assertThat(trump).hasCSS("height", "27px");
        var trumpFrame = page.locator(".whist-trump").boundingBox();
        var trumpFace = trump.boundingBox();
        require(trumpFace.x >= trumpFrame.x + 4 && trumpFace.x + trumpFace.width <= trumpFrame.x + trumpFrame.width - 4,
                "The turned card must fit inside its frame padding");
        images.add(screenshot(page, output, "WEB-UI-HEADER", "Antetul Web după referința Android"));
        var menu = page.locator(".whist-navigation summary");
        menu.click();
        var menuItems = page.locator(".whist-menu-items");
        assertThat(menuItems).isVisible();
        assertThat(menuItems.locator("button").first()).hasCSS("font-size", "24px");
        var board = page.locator(".board");
        board.tap(new Locator.TapOptions().setPosition(board.boundingBox().width - 8, 8));
        assertThat(menuItems).isHidden();
        Locator rejected = page.locator(".fan button[data-face='8♣']");
        rejected.tap();
        assertThat(rejected).hasClass(Pattern.compile("rejected-card"));
        assertThat(page.locator(".fan .suggested-card")).hasCount(2);
        require(page.locator(".fan button").count() == 4, "A rejected card changed the hand");
        var manualTrump = trumpAppearance(trump.locator(".."));
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Automat").setExact(true)).tap();
        require(manualTrump.equals(trumpAppearance(trump.locator(".."))), "AutoGame changed the trump frame");
        var collection = page.locator(".collection .whist-play-face");
        var faces = List.of("9♦", "Q♥", "K♦", "8♦");
        assertThat(collection).hasCount(faces.size());
        var tableFaces = page.locator(".cards-on-table .whist-play-face");
        for (int index = 0; index < faces.size(); index++) {
            String id = faces.get(index);
            var copy = collection.nth(index);
            assertThat(copy).hasAttribute("data-face", id);
            var played = tableFaces.and(page.locator("[data-face='" + id + "']"));
            require(faceGeometry(played).equals(faceGeometry(copy)), "Collection changed the face of " + id);
        }
        var deposited = tableFaces.and(page.locator("[data-face='8♦']"));
        var depositedFace = faceGeometry(deposited);
        require(heldFace.get("marks").equals(depositedFace.get("marks")), "The deposited face lost a mark");
        require(heldFace.get("ink").equals(depositedFace.get("ink")), "The deposited face changed ink");
        for (String property : List.of("rankSize", "indexTop", "suitX", "suitBaseline", "suitSize", "invertedRight")) {
            double fromHand = ((Number) heldFace.get(property)).doubleValue();
            double fromTable = ((Number) depositedFace.get(property)).doubleValue();
            require(Math.abs(fromHand - fromTable) < .026, "The deposited face moved " + property);
        }
        require(manualTrump.equals(trumpAppearance(trump.locator(".."))), "AutoGame collection changed the trump frame");
        var eva = page.locator(".seat").filter(new Locator.FilterOptions()
                .setHas(page.getByText("Eva", new Page.GetByTextOptions().setExact(true))));
        assertThat(eva.locator(".seat-won")).hasText("Levate: 1");
        assertThat(page.locator(".fan button")).hasCount(3);
        images.add(screenshot(page, output, "WEB-UI-AUTO-collection", "AutoGame după prima levată"));
        page.locator(".whist-score").waitFor(new Locator.WaitForOptions().setTimeout(45_000));
        var notice = page.locator("dialog.achievement-notice[open]");
        if (notice.isVisible()) notice.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Închide").setExact(true)).click();
        var totals = page.locator(".whist-score tbody tr td:last-child").allTextContents();
        require(totals.equals(List.of("20", "6", "24", "10")), "Unexpected score: " + totals);
        require(page.locator(".whist-auto").count() == 0, "AutoGame remains available at score");
        return Map.of("firstCollection", faces, "winner", "Eva", "totals", totals, "rejectedCard", "8♣",
                "heldFace", heldFace, "depositedFace", depositedFace);
    }
}
