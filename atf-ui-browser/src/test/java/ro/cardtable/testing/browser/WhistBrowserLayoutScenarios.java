package ro.cardtable.testing.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import ro.cardtable.testing.browser.BrowserCheckResults.Image;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static ro.cardtable.testing.browser.BrowserCheckSupport.*;

/** Test-only Whist diagnostic scenario drivers; no browser lifecycle or report publication. */
final class WhistBrowserLayoutScenarios {

    static Map<String, Object> homeDedication(Page page, String url, Path output, List<Image> images) {
        page.navigate(url);
        var home = page.locator(".whist-home");
        var dedication = home.locator("figure:last-child");
        var caption = dedication.locator("figcaption");
        var photo = dedication.locator("img");
        var language = home.locator("select");
        assertThat(home).isVisible();
        language.selectOption("ro");
        assertThat(caption).hasText("Pentru Teo.. de Moș Nicolae :)");
        assertThat(dedication.locator("xpath=preceding-sibling::*[1]")).hasText("Reguli de joc");
        assertThat(photo).hasAttribute("alt", "Pisică");
        page.waitForFunction("document.querySelector('.whist-home-dedication img')?.naturalWidth > 0");
        images.add(screenshot(page, output, "WEB-HOME-DEDICATION-RO", "Meniul principal în română"));

        language.selectOption("en");
        assertThat(caption).hasText("For Teo.. from St. Nicholas :)");
        assertThat(dedication.locator("xpath=preceding-sibling::*[1]")).hasText("How to play");
        assertThat(photo).hasAttribute("alt", "Cat");
        page.waitForFunction("document.querySelector('.whist-home-dedication img')?.naturalWidth > 0");
        assertThat(home.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("How to play"))).isVisible();
        return Map.of("ro", "Pentru Teo.. de Moș Nicolae :)", "en", caption.innerText(),
                "photo", photo.getAttribute("src"), "viewport", "414×896");
    }

    static Map<String, Object> responsiveViewport(Page page, String url, Path output,
                                                           boolean mobile, List<Image> images) {
        page.navigate(url);
        page.locator(".whist-home button.primary").waitFor();
        List<int[]> sizes = mobile
                ? List.of(new int[]{414, 896}, new int[]{360, 800}, new int[]{896, 414})
                : List.of(new int[]{1920, 1080}, new int[]{1280, 720}, new int[]{535, 760},
                        new int[]{400, 900}, new int[]{220, 420}, new int[]{400, 500}, new int[]{400, 600}, new int[]{400, 610});
        List<Map<String, Object>> observations = new ArrayList<>();
        for (int index = 0; index < sizes.size(); index++) {
            int width = sizes.get(index)[0];
            int height = sizes.get(index)[1];
            page.evaluate("() => { window.__viewportResize = new Promise(resolve => " +
                    "window.addEventListener('resize', () => resolve(true), { once: true })); }");
            page.setViewportSize(width, height);
            page.evaluate("() => window.__viewportResize");
            @SuppressWarnings("unchecked")
            Map<String, Object> measured = (Map<String, Object>) page.evaluate("""
                    () => {
                      const frame = document.querySelector('#frame');
                      const stage = document.querySelector('#table');
                      const root = document.documentElement;
                      const visual = window.visualViewport;
                      const viewport = document.querySelector('.viewport');
                      const inset = getComputedStyle(viewport);
                      const number = value => parseFloat(value) || 0;
                      const rect = node => {
                        const box = node.getBoundingClientRect();
                        return { x: box.x, y: box.y, width: box.width, height: box.height };
                      };
                      return {
                        availableWidth: Math.min(root.clientWidth, visual?.width ?? innerWidth) -
                          number(inset.paddingLeft) - number(inset.paddingRight),
                        availableHeight: (visual?.height ?? innerHeight) -
                          number(inset.paddingTop) - number(inset.paddingBottom),
                        frame: rect(frame), viewport: rect(viewport),
                        logicalWidth: number(getComputedStyle(stage).getPropertyValue('--stage-width')),
                        logicalHeight: number(getComputedStyle(stage).getPropertyValue('--stage-height')),
                        scale: number(getComputedStyle(stage).getPropertyValue('--stage-scale')),
                        matrixScaleX: new DOMMatrixReadOnly(getComputedStyle(stage).transform).a,
                        matrixScaleY: new DOMMatrixReadOnly(getComputedStyle(stage).transform).d,
                        transform: getComputedStyle(stage).transform,
                        kind: frame.dataset.viewport
                      };
                    }
                    """);
            double availableWidth = ((Number) measured.get("availableWidth")).doubleValue();
            double availableHeight = ((Number) measured.get("availableHeight")).doubleValue();
            double actualScale = ((Number) measured.get("scale")).doubleValue();
            double expectedScale;
            if (mobile) {
                double logicalHeight = ((Number) measured.get("logicalHeight")).doubleValue();
                require(logicalHeight >= availableHeight, "Mobile content height cannot discard the useful viewport");
                expectedScale = Math.min(1, availableHeight / logicalHeight);
                near(availableWidth, ((Number) ((Map<?, ?>) measured.get("frame")).get("width")).doubleValue());

                require("phone".equals(measured.get("kind")), "The phone adapter must remain in 1:1 mode");
            } else {
                double logicalHeight = ((Number) measured.get("logicalHeight")).doubleValue();
                require(logicalHeight >= 626, "The adaptive scene retains its approved minimum height");
                expectedScale = Math.min(availableHeight / logicalHeight, availableWidth / 393.0);
                require("desktop".equals(measured.get("kind")), "The desktop adapter must use the scaled frame");
            }
            require(Math.abs(expectedScale - actualScale) <= .002,
                    "Expected scale " + expectedScale + ", observed " + actualScale + " at " + width + "×" + height);
            near(mobile ? availableWidth : 393, ((Number) measured.get("logicalWidth")).doubleValue());
            require(((Number) measured.get("logicalHeight")).doubleValue() >= (mobile ? availableHeight : 626), "Content retains its minimum logical height");
            require(Math.abs(expectedScale - ((Number) measured.get("matrixScaleX")).doubleValue()) <= .002,
                    "The horizontal CSS transform scale differs from the viewport contract");
            require(Math.abs(expectedScale - ((Number) measured.get("matrixScaleY")).doubleValue()) <= .002,
                    "The vertical CSS transform scale differs from the viewport contract");
            @SuppressWarnings("unchecked")
            Map<String, Object> frame = (Map<String, Object>) measured.get("frame");
            @SuppressWarnings("unchecked")
            Map<String, Object> viewport = (Map<String, Object>) measured.get("viewport");
            double frameX = ((Number) frame.get("x")).doubleValue();
            double frameY = ((Number) frame.get("y")).doubleValue();
            double frameWidth = ((Number) frame.get("width")).doubleValue();
            double frameHeight = ((Number) frame.get("height")).doubleValue();
            require(frameWidth <= availableWidth + 1,
                    "The frame exceeded the useful width at " + width + "×" + height + ": " + frame);
            near(((Number) viewport.get("x")).doubleValue() + Math.max(0, (availableWidth - frameWidth) / 2), frameX);
            require(frameHeight <= availableHeight + 1,
                    "The frame exceeded the useful height at " + width + "×" + height + ": " + frame);
            near(((Number) viewport.get("y")).doubleValue() + Math.max(0, (availableHeight - frameHeight) / 2), frameY);
            require(measured.get("transform") instanceof String transform && transform.startsWith("matrix("),
                    "The frame must use a CSS matrix transform");
            observations.add(Map.of("requested", List.of(width, height), "expectedScale", expectedScale,
                    "measured", measured));
            if (index == 0 || index == sizes.size() - 1 || (mobile && index == 1)) {
                images.add(screenshot(page, output, "WEB-VIEWPORT-" + (mobile ? "PHONE" : "DESKTOP") + "-" + width + "x" + height,
                        "Whist · viewport " + width + "×" + height));
            }
        }
        return Map.of("mobile", mobile, "cases", observations);
    }

    static Map<String, Object> responsiveHand(Page page, String url) {
        page.navigate(url);
        assertThat(page.locator(".whist-home h1")).hasCSS("font-size", "37.44px");
        assertThat(button(page, "Joc nou")).hasCSS("font-size", "24px");
        assertThat(page.locator(".whist-home select")).hasCSS("font-size", "18px");
        button(page, "Joc nou").click();
        assertThat(page.locator(".setup h1")).hasCSS("font-size", "37.44px");
        verifySetupDropdowns(page);
        assertThat(button(page, "Începe partida")).hasCSS("font-size", "24px");
        page.locator("select[name=players]").selectOption("3");
        page.locator("select[name=mode]").selectOption("DOWN_AND_UP");
        button(page, "Începe partida").click();
        page.locator("[data-bid][aria-pressed=true]").waitFor();
        page.locator(".board[data-layout=ready]").waitFor();
        require("phone".equals(page.locator("#frame").getAttribute("data-viewport")), "Expected mobile layout");
        var frame = page.locator("#frame").boundingBox();
        near(414, frame.width);
        near(896, frame.height);
        Locator hand = page.locator(".hand");
        Locator cards = hand.locator(".fan .card");
        require(cards.count() == 17, "Expected 17 cards");
        cards.last().scrollIntoViewIfNeeded();
        var end = cards.last().boundingBox();
        var holder = hand.boundingBox();
        require(end.x >= holder.x - 1 && end.x + end.width <= holder.x + holder.width + 1,
                "Last card cannot be shown in full");
        var biddingSeats = seats(page);
        assertHeader(page, false);
        assertThat(page.locator(".seat.south")).hasAttribute("aria-current", "true");
        assertThat(page.locator(".seat.south .seat-turn")).isHidden();
        assertThat(button(page, "Licitează")).isVisible();
        button(page, "Licitează").press("Space");
        assertThat(page.locator("[data-bid]")).hasCount(0);
        page.locator("#table[aria-busy=false] .board[data-layout=ready]").waitFor();
        var playingSeats = seats(page);
        assertHeader(page, false);
        require(biddingSeats.equals(playingSeats), "Player boxes moved: " + biddingSeats + " -> " + playingSeats);
        return Map.of("viewport", List.of(414, 896), "cards", cards.count(), "seats", playingSeats,
                "lastCardRight", end.x + end.width);
    }

    static Map<String, Object> desktopHeader(Page page, String url) {
        page.navigate(url);
        button(page, "Joc nou").click();
        page.locator("select[name=mode]").selectOption("OH_HELL");
        button(page, "Începe partida").click();
        page.locator(".board[data-layout=ready]").waitFor();
        assertThat(page.locator(".whist-title-row h1")).hasText("Whist · Oh Hell");
        assertThat(page.locator(".whist-trump .card")).isVisible();
        double scale = page.locator("#frame").boundingBox().width / 414.0;
        assertHeader(page, false, scale);
        return Map.of("viewport", List.of(535, 760), "scale", scale, "title", "Whist · Oh Hell");
    }

    static Map<String, Object> desktop(Page page, String url) {
        page.navigate(url);
        var profile = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Numele tău:")));
        String initial = profile.innerText();
        profile.click();
        Locator dialog = page.getByRole(AriaRole.DIALOG);
        dialog.getByRole(AriaRole.TEXTBOX).fill("Bianca");
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Anulează").setExact(true)).click();
        button(page, initial).waitFor();
        profile.click();
        dialog.getByRole(AriaRole.TEXTBOX).fill("Bianca");
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Salvează").setExact(true)).click();
        button(page, "Numele tău: Bianca").waitFor();
        page.reload();
        button(page, "Numele tău: Bianca").waitFor();
        var first = centered(page, 1280, 720);
        page.setViewportSize(1920, 1080);
        var second = centered(page, 1920, 1080);
        return Map.of("profile", "Bianca", "frame1280", first, "frame1920", second);
    }
}
