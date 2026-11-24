package ro.cardtable.testing.browser;

import com.google.gson.Gson;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Test-only shared observations and independent geometry assertions; no browser lifecycle or report publication. */
final class BrowserCheckSupport {
    static final Gson JSON = new Gson();
    private static final String MOBILE_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36";

    static Browser.NewContextOptions mobile() {
        return new Browser.NewContextOptions().setViewportSize(414, 896).setDeviceScaleFactor(1)
                .setIsMobile(true).setHasTouch(true).setUserAgent(MOBILE_AGENT);
    }

    static void verifySetupDropdowns(Page page) {
        for (var select : page.locator(".setup select").all()) {
            boolean scaled = (Boolean) select.evaluate("""
                    node => {
                      const size = element => parseFloat(getComputedStyle(element).fontSize);
                      return Math.abs(size(node) - .75 * size(node.parentElement)) < .01 &&
                        [...node.options].every(option => Math.abs(size(option) - size(node)) < .01);
                    }
                    """);
            require(scaled, "Expected a 25% smaller font for " + select.getAttribute("name"));
        }
        for (int width : List.of(360, 414)) {
            page.setViewportSize(width, width == 360 ? 800 : 896);
            assertThat(page.locator("#frame")).hasCSS("width", width + "px");
            boolean difficultyOnOneLine = (Boolean) page.locator(".difficulty-field").evaluate("""
                    label => {
                      const range = document.createRange();
                      range.selectNodeContents(label.firstChild);
                      const text = range.getBoundingClientRect();
                      const choice = label.querySelector("select").getBoundingClientRect();
                      return choice.left >= text.right && choice.top <= text.top && choice.bottom >= text.bottom;
                    }
                    """);
            require(difficultyOnOneLine, "The Romanian difficulty label and selector must share one row at " + width);
        }
    }

    static void assertHeader(Page page, boolean enlarged) {
        assertHeader(page, enlarged, 1);
    }

    static void assertHeader(Page page, boolean enlarged, double scale) {
        var header = page.locator(".whist-header");
        var row = header.locator(".whist-title-row");
        var title = row.locator("h1");
        var menu = row.locator("summary");
        var progress = header.locator(".whist-progress");
        var trump = header.locator(".whist-trump");
        assertThat(title).hasCSS("font-size", enlarged ? "28px" : "14px");
        assertThat(progress).hasCSS("font-size", enlarged ? "33px" : "16.5px");
        var bids = header.locator(".whist-bids");
        assertThat(progress).hasCSS("line-height", enlarged ? "30px" : "15px");
        assertThat(bids).hasCSS("line-height", enlarged ? "30px" : "15px");
        require((boolean) bids.evaluate("""
                node => node.scrollWidth <= node.clientWidth + 1
                  && node.textContent.split("\\n").length === 2
                """), "Bid summary is clipped or wrapped");
        var progressBounds = progress.boundingBox();
        var bidsBounds = bids.boundingBox();
        near(progressBounds.y + progressBounds.height, bidsBounds.y);
        near((enlarged ? 60 : 30) * scale, bidsBounds.height);
        assertThat(trump).hasCSS("width", "57px");
        assertThat(trump).hasCSS("height", enlarged ? "89px" : "49.5px");
        assertThat(menu).hasCSS("width", "48px");
        var bounds = header.boundingBox();
        var rowBounds = row.boundingBox();
        var trumpBounds = trump.boundingBox();
        near(bounds.x + 56 * scale, title.boundingBox().x);
        near(title.boundingBox().x, progress.boundingBox().x);
        near(bounds.x + bounds.width, trumpBounds.x + trumpBounds.width);
        var details = header.locator(".whist-header-details").boundingBox();
        near(rowBounds.y + rowBounds.height, details.y);
        near(details.y, trumpBounds.y);
    }

    static void resumeHand(Page page, String url, String checkpoint) {
        page.addInitScript("""
                if (location.hostname === "127.0.0.1") {
                  localStorage.setItem("whistlab:pages:profile:v1", %s);
                }
                """.formatted(JSON.toJson(checkpoint)));
        page.navigate(url);
        button(page, "Reia partida").click();
        page.locator(".board[data-layout=ready]").waitFor();
    }

    static Object trumpAppearance(Locator trump) {
        return trump.evaluate("""
                node => {
                  const { backgroundColor, boxShadow, color } = getComputedStyle(node);
                  const { x, y, width, height } = node.getBoundingClientRect();
                  return { backgroundColor, boxShadow, color, x, y, width, height };
                }
                """);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> faceGeometry(Locator card) {
        return (Map<String, Object>) card.evaluate("""
                node => {
                  const index = node.querySelector('.card-index:not(.inverted)');
                  const rank = index.querySelector('.card-rank');
                  const suit = node.querySelector('.suit');
                  const inverted = node.querySelector('.card-index.inverted');
                  const height = node.offsetHeight;
                  const width = node.offsetWidth;
                  const ratio = (mark, property, size = height) =>
                    parseFloat(getComputedStyle(mark)[property]) / size;
                  return {
                    marks: [rank.textContent, index.lastElementChild.textContent, suit.textContent,
                      inverted.querySelector('.card-rank').textContent, inverted.lastElementChild.textContent],
                    ink: getComputedStyle(suit).color,
                    rankSize: ratio(rank, 'fontSize'),
                    indexTop: ratio(index, 'top'),
                    suitX: ratio(suit, 'left', width),
                    suitBaseline: ratio(suit, 'top'),
                    suitSize: ratio(suit, 'fontSize'),
                    invertedRight: ratio(inverted, 'right', width)
                  };
                }
                """);
    }

    static List<Long> centered(Page page, int width, int height) {
        var frame = page.locator("#frame[data-viewport=desktop]");
        var box = frame.boundingBox();
        double logicalHeight = ((Number) page.locator("#table").evaluate("node => parseFloat(getComputedStyle(node).getPropertyValue('--stage-height'))")).doubleValue();
        require(logicalHeight >= 626, "The desktop scene retains the adaptive content minimum");
        double scale = Math.min(height / logicalHeight, width / 393.0);
        near(393 * scale, box.width);
        near(logicalHeight * scale, box.height);
        near((width - box.width) / 2, box.x);
        near((height - box.height) / 2, box.y);
        return dimensions(box);
    }

    static List<List<Double>> seats(Page page) {
        return page.locator(".board:visible .seat").all().stream().map(Locator::boundingBox)
                .map(box -> List.of(box.x, box.y, box.width, box.height)).toList();
    }

    static List<Long> dimensions(BoundingBox box) {
        return List.of(Math.round(box.x), Math.round(box.y), Math.round(box.width), Math.round(box.height));
    }

    static Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    static Image screenshot(Page page, Path output, String id, String title) {
        String file = id + ".png";
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(file)));
        return new Image(file, title);
    }

    static void near(double expected, double observed) {
        require(Math.abs(expected - observed) <= 1, "Expected " + expected + ", observed " + observed);
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
